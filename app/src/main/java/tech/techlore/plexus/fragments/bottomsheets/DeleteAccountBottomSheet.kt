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

package tech.techlore.plexus.fragments.bottomsheets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import tech.techlore.plexus.R
import tech.techlore.plexus.activities.MainActivity
import tech.techlore.plexus.databinding.BottomSheetDeleteAccountBinding
import tech.techlore.plexus.databinding.BottomSheetFooterBinding
import tech.techlore.plexus.databinding.BottomSheetHeaderBinding
import tech.techlore.plexus.preferences.EncryptedPreferenceManager
import tech.techlore.plexus.preferences.EncryptedPreferenceManager.Companion.DEVICE_TOKEN
import tech.techlore.plexus.preferences.EncryptedPreferenceManager.Companion.IS_REGISTERED
import tech.techlore.plexus.utils.UiUtils.Companion.showSnackbar

class DeleteAccountBottomSheet : BottomSheetDialogFragment() {
    
    private var _binding: BottomSheetDeleteAccountBinding? = null
    private val bottomSheetBinding get() = _binding!!
    
    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        
        _binding = BottomSheetDeleteAccountBinding.inflate(inflater, container, false)
        return bottomSheetBinding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        
        val headerBinding = BottomSheetHeaderBinding.bind(bottomSheetBinding.root)
        val footerBinding = BottomSheetFooterBinding.bind(bottomSheetBinding.root)
        
        // Title
        headerBinding.bottomSheetTitle.text = getString(R.string.delete_account)
        
        // Proceed
        footerBinding.positiveButton.apply {
            text = getString(R.string.proceed)
            setOnClickListener {
                EncryptedPreferenceManager(requireContext()).apply {
                    deleteString(DEVICE_TOKEN)
                    setBoolean(IS_REGISTERED, false)
                }
                dismiss()
                (requireActivity() as MainActivity).apply {
                    if (selectedNavItem == R.id.nav_my_ratings) {
                        selectedNavItem = R.id.nav_plexus_data
                        clickedNavItem = R.id.nav_plexus_data
                        displayFragment(clickedNavItem)
                    }
                    showSnackbar(activityBinding.mainCoordLayout,
                                 getString(R.string.deleted_account_successfully),
                                 activityBinding.bottomNavContainer)
                }
            }
        }
        
        // Cancel
        footerBinding.negativeButton.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}