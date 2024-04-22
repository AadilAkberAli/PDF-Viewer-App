package com.example.pdfviewer

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter


class ViewPagerAdapter(fm: FragmentManager, lifeCycle: Lifecycle) :
    FragmentStateAdapter(fm, lifeCycle) {

    override fun getItemCount(): Int {
        return  2
    }

    override fun createFragment(position: Int): Fragment {
        var fragment: Fragment? = null
        if (position == 0) {
            fragment = ManualFragment()
        } else if (position == 1) {
            fragment = QRScanFragment()
        }
        return fragment!!
    }
}