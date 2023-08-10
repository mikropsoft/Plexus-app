/*
 * Copyright (c) 2022-present Techlore
 *
 *  This file is part of Plexus.
 *
 *  Plexus is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Plexus is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Plexus.  If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techlore.plexus.adapters.main

import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import me.zhanghai.android.fastscroll.PopupTextProvider
import tech.techlore.plexus.R
import tech.techlore.plexus.appmanager.ApplicationManager
import tech.techlore.plexus.models.minimal.MainDataMinimal
import tech.techlore.plexus.utils.MainDataMinimalDiffUtil
import tech.techlore.plexus.utils.UiUtils.Companion.hScrollText
import tech.techlore.plexus.utils.UiUtils.Companion.mapStatusStringToColor
import kotlin.collections.ArrayList

class FavoriteItemAdapter(private val aListViewItems: ArrayList<MainDataMinimal>,
                          private val clickListener: OnItemClickListener,
                          private val coroutineScope: CoroutineScope) :
    RecyclerView.Adapter<FavoriteItemAdapter.ListViewHolder>(), PopupTextProvider {
    
    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
    
    inner class ListViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val name: TextView = itemView.findViewById(R.id.name)
        val packageName: TextView = itemView.findViewById(R.id.packageName)
        val dgStatus: MaterialTextView = itemView.findViewById(R.id.dgStatus)
        val mgStatus: MaterialTextView = itemView.findViewById(R.id.mgStatus)
        val fav: MaterialCheckBox = itemView.findViewById(R.id.fav)
        
        init {
            itemView.setOnClickListener(this)
        }
        
        override fun onClick(v: View?) {
            val position = bindingAdapterPosition
            if (position != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(position)
            }
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        return ListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_main_recycler_view, parent, false)
        )
    }
    
    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        
        holder.fav.setOnCheckedChangeListener(null)
        val favorite = aListViewItems[position]
        val context = holder.itemView.context
    
        holder.icon.apply {
            if (favorite.isInstalled) {
                try {
                    setImageDrawable(context.packageManager.getApplicationIcon(favorite.packageName))
                    // Don't use GLIDE to load icons directly to ImageView
                    // as there's a delay in displaying icons when fast scrolling
                }
                catch (e: PackageManager.NameNotFoundException) {
                    e.printStackTrace()
                }
            }
            else {
                val requestOptions =
                    RequestOptions()
                        .placeholder(R.drawable.ic_apk) // Placeholder icon
                        .fallback(R.drawable.ic_apk) // Fallback image in case requested image isn't available
                        .centerCrop() // Center-crop the image to fill the ImageView
                        .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache strategy
        
                Glide.with(context)
                    .load(favorite.iconUrl)
                    .onlyRetrieveFromCache(true) // Icon should always be in cache
                    .apply(requestOptions)
                    .into(this)
            }
        }
        
        holder.name.apply {
            text = favorite.name
            hScrollText(this)
        }
        
        holder.packageName.apply {
            text = favorite.packageName
            hScrollText(this)
        }
        
        holder.dgStatus.apply {
            text = favorite.dgStatus
            backgroundTintList =
                mapStatusStringToColor(context, favorite.dgStatus)?.let { ColorStateList.valueOf(it) }
        }
    
        holder.mgStatus.apply {
            text = favorite.mgStatus
            backgroundTintList =
                mapStatusStringToColor(context, favorite.mgStatus)?.let { ColorStateList.valueOf(it) }
        }
        
        holder.fav.apply {
            isChecked = favorite.isFav
            setOnCheckedChangeListener{ _, isChecked ->
                favorite.isFav = isChecked
                coroutineScope.launch {
                    (context.applicationContext as ApplicationManager).miniRepository.updateFav(favorite)
                }
        
                val currentPosition = holder.bindingAdapterPosition
                if (currentPosition != RecyclerView.NO_POSITION) {
                    coroutineScope.launch(Dispatchers.Main) {
                        aListViewItems.removeAt(currentPosition)
                        notifyItemRemoved(currentPosition)
                        notifyItemRangeChanged(currentPosition, aListViewItems.size - currentPosition)
                    }
                }
            }
        }
        
    }
    
    override fun getItemCount(): Int {
        return aListViewItems.size
    }
    
    override fun getItemViewType(position: Int): Int {
        return position
    }
    
    // Fast scroll popup
    override fun getPopupText(position: Int): String {
        return aListViewItems[position].name.substring(0, 1)
    }
    
    fun updateList(newList: ArrayList<MainDataMinimal>){
        val diffCallback = MainDataMinimalDiffUtil(aListViewItems, newList)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        diffResult.dispatchUpdatesTo(this)
        aListViewItems.apply{
            clear()
            addAll(newList)
        }
    }
}