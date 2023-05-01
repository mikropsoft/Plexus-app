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

package tech.techlore.plexus.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import retrofit2.awaitResponse
import tech.techlore.plexus.R
import tech.techlore.plexus.appmanager.ApplicationManager
import tech.techlore.plexus.databinding.ActivityAppDetailsBinding
import tech.techlore.plexus.fragments.bottomsheets.FirstSubmissionBottomSheet
import tech.techlore.plexus.fragments.bottomsheets.MoreOptionsBottomSheet
import tech.techlore.plexus.fragments.bottomsheets.SortUserRatingsBottomSheet
import tech.techlore.plexus.models.get.main.MainData
import tech.techlore.plexus.models.get.ratings.Rating
import tech.techlore.plexus.preferences.PreferenceManager
import tech.techlore.plexus.preferences.PreferenceManager.Companion.FIRST_SUBMISSION
import tech.techlore.plexus.utils.UiUtils.Companion.mapScoreRangeToStatusString

class AppDetailsActivity : AppCompatActivity(), MenuProvider {
    
    private lateinit var activityBinding: ActivityAppDetailsBinding
    private lateinit var navHostFragment: NavHostFragment
    lateinit var navController: NavController
    private lateinit var preferenceManager: PreferenceManager
    lateinit var app: MainData
    var ratingsList = ArrayList<Rating>()
    private var ratingsRetrieved = false
    var differentVersionsList = listOf<String>()
    var selectedVersionString: String? = null
    var statusRadio = R.id.user_ratings_radio_any_status
    var dgStatusSort = 0
    var mgStatusSort = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        addMenuProvider(this)
        activityBinding = ActivityAppDetailsBinding.inflate(layoutInflater)
        setContentView(activityBinding.root)
        
        preferenceManager = PreferenceManager(this)
        navHostFragment = supportFragmentManager.findFragmentById(R.id.details_nav_host) as NavHostFragment
        navController = navHostFragment.navController
        selectedVersionString = getString(R.string.any)
        val repository = (applicationContext as ApplicationManager).mainRepository
        val requestManager = Glide.with(this)
        val requestOptions =
            RequestOptions()
                .placeholder(R.drawable.ic_apk) // Placeholder image
                .fallback(R.drawable.ic_apk) // Fallback image in case requested image isn't available
                .centerCrop() // Center-crop the image to fill the ImageView
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache strategy
    
        /*########################################################################################*/
        
        setSupportActionBar(activityBinding.bottomAppBar)
        activityBinding.bottomAppBar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }
    
        // Disable radio group until ratings list is retrieved
        activityBinding.detailsRadiogroup.isEnabled = false
        
        runBlocking {
            launch {
                app = intent.getStringExtra("packageName")?.let { repository.getAppByPackage(it) } !!
            }
        }
    
        lifecycleScope.launch {
    
            // Icon
            val requestBuilder =
                if (!app.isInstalled) {
                    requestManager
                        .load(app.iconUrl)
                        .onlyRetrieveFromCache(true) // Icon should always be in cache
                        .apply(requestOptions)
                }
                else {
                    try {
                        requestManager
                            .load(packageManager.getApplicationIcon(app.packageName))
                            .apply(requestOptions)
                    }
                    catch (e: PackageManager.NameNotFoundException) {
                        throw RuntimeException(e)
                    }
                }
    
            requestBuilder.into(activityBinding.detailsAppIcon)
            activityBinding.detailsName.text = app.name
            activityBinding.detailsPackageName.text = app.packageName
            @SuppressLint("SetTextI18n")
            activityBinding.detailsInstalledVersion.text = "${getString(R.string.installed)}: " +
                                                           app.installedVersion.ifEmpty { getString(R.string.not_tested_title) }
    
            // Radio group/buttons
            activityBinding.detailsRadiogroup.setOnCheckedChangeListener{_, checkedId: Int ->
                displayFragment(checkedId)
            }
    
            // FAB
            if (!app.isInstalled){
                activityBinding.fab.visibility = View.GONE
            }
            else {
                activityBinding.fab.setOnClickListener {
                    if (preferenceManager.getBoolean(FIRST_SUBMISSION)) {
                        FirstSubmissionBottomSheet(positiveButtonClickListener = { startSubmitActivity() })
                            .show(supportFragmentManager, "FirstSubmissionBottomSheet")
                    }
                    else {
                        startSubmitActivity()
                    }
                }
            }
    
            // Only retrieve ratings list if not done already
            if (!ratingsRetrieved) {
                val apiRepository = (applicationContext as ApplicationManager).apiRepository
                val ratingsCall = apiRepository.getRatings(app.packageName)
                val ratingsResponse = ratingsCall.awaitResponse()
        
                if (ratingsResponse.isSuccessful) {
                    ratingsResponse.body()?.let { ratingsRoot ->
                        app.ratingsList = ratingsRoot.ratingsData
                        ratingsList = ratingsRoot.ratingsData
                    }
                }
        
                repository.insertOrUpdatePlexusData(app)
                ratingsRetrieved = true
                displayFragment(10)
                activityBinding.detailsRadiogroup.isEnabled = true
            }
        }
        
    }
    
    // Setup fragments
    private fun displayFragment(checkedItem: Int) {
        val currentFragment = navController.currentDestination!!
        
        val action: Int =
            when (checkedItem) {
                
                10 -> R.id.action_fragmentProgressBar_to_totalScoreFragment
                // 10 is just a custom number
                // to let the nav controller navigate from
                // progress bar fragment to total score fragment
                
                R.id.radio_total_score -> R.id.action_userRatingsFragment_to_totalScoreFragment
                
                R.id.radio_user_ratings -> R.id.action_totalScoreFragment_to_userRatingsFragment
                
                else -> 0
            }
        
        // java.lang.IllegalArgumentException:
        // Destination id == 0 can only be used in conjunction with a valid navOptions.popUpTo
        // Hence the second check
        if (checkedItem != currentFragment.id && action != 0) {
            navController.navigate(action)
        }
    }
    
    override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.menu_activity_details, menu)
        
        menu.findItem(R.id.menu_sort_user_ratings).isVisible =
            navController.currentDestination!!.id == R.id.userRatingsFragment
    }
    
    override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
        when(menuItem.itemId) {
            
            R.id.details_menu_help -> startActivity(Intent(this@AppDetailsActivity, SettingsActivity::class.java)
                                                        .putExtra("frag", R.id.helpFragment))
            
            R.id.menu_sort_user_ratings -> SortUserRatingsBottomSheet().show(supportFragmentManager, "SortUserRatingsBottomSheet")
            
            R.id.menu_more ->
                MoreOptionsBottomSheet(app.name, app.packageName,
                                       mapScoreRangeToStatusString(this@AppDetailsActivity, app.dgScore),
                                       mapScoreRangeToStatusString(this@AppDetailsActivity, app.mgScore),
                                       activityBinding.appDetailsCoordinatorLayout,
                                       activityBinding.bottomAppBar)
                    .show(supportFragmentManager, "MoreOptionsBottomSheet")
            
        }
        
        return true
    }
    
    private fun startSubmitActivity() {
        val intent =
            Intent(this@AppDetailsActivity, SubmitActivity::class.java)
                .putExtra("name", app.name)
                .putExtra("packageName", app.packageName)
                .putExtra("installedVersion", app.installedVersion)
                .putExtra("installedBuild", app.installedBuild)
                .putExtra("isInPlexusData", app.isInPlexusData)
        
        startActivity(intent)
        overridePendingTransition(R.anim.fade_in_slide_from_bottom, R.anim.no_movement)
    }
    
    // On back pressed
    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() { finish() }
    }
}