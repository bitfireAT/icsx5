package at.bitfire.icsdroid.ui

import android.app.Activity
import androidx.fragment.app.FragmentManager

interface StartupFragment {

    fun initiate(activity: Activity, fragmentManager: FragmentManager)

}