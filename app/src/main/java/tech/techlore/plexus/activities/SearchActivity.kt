/*
 *     Copyright (C) 2022-present Techlore
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package tech.techlore.plexus.activities

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.stellarsand.android.fastscroll.FastScrollerBuilder
import tech.techlore.plexus.R
import tech.techlore.plexus.adapters.main.MainDataItemAdapter
import tech.techlore.plexus.appmanager.ApplicationManager
import tech.techlore.plexus.databinding.ActivitySearchBinding
import tech.techlore.plexus.models.minimal.MainDataMinimal
import tech.techlore.plexus.utils.IntentUtils.Companion.startDetailsActivity

class SearchActivity : AppCompatActivity(), MainDataItemAdapter.OnItemClickListener {
    
    private lateinit var searchDataList: ArrayList<MainDataMinimal>
    
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val activityBinding = ActivitySearchBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)
        
        val miniRepository = (applicationContext as ApplicationManager).miniRepository
        searchDataList = ArrayList()
        var job: Job? = null
        
        // Bottom toolbar as actionbar
        activityBinding.searchToolbarBottom.apply {
            setSupportActionBar(this)
            setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
        }
    
        supportActionBar?.setDisplayShowTitleEnabled(false)
        
        // Perform search
        activityBinding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            
            override fun onQueryTextSubmit(searchString: String): Boolean {
                return true
            }
            
            override fun onQueryTextChange(searchString: String): Boolean {
                job?.cancel()
                
                // Search with a subtle delay
                job = lifecycleScope.launch {
                    delay(350)
                    if (searchString.isNotEmpty()) {
                            searchDataList = miniRepository.searchFromDb(searchString)
                            if (searchDataList.isEmpty()) {
                                activityBinding.searchRv.adapter = null
                                activityBinding.emptySearchView.visibility = View.VISIBLE
                            }
                            else {
                                activityBinding.emptySearchView.visibility = View.GONE
                                val mainDataItemAdapter = MainDataItemAdapter(searchDataList,
                                                                              this@SearchActivity,
                                                                              lifecycleScope)
                                activityBinding.searchRv.adapter = mainDataItemAdapter
                                FastScrollerBuilder(activityBinding.searchRv).build() // Fast scroll
                            }
                    }
                    else {
                        activityBinding.searchRv.adapter = null
                        activityBinding.emptySearchView.visibility = View.GONE
                    }
                }
                
                return true
            }
        })
    }
    
    // On click
    override fun onItemClick(position: Int) {
        val searchData = searchDataList[position]
        startDetailsActivity(this@SearchActivity, searchData.packageName)
    }
    
    override fun finish() {
        super.finish()
        overridePendingTransition(0, R.anim.fade_out_slide_to_bottom)
    }
}