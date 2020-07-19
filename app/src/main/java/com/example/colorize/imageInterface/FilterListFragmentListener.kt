package com.example.colorize.imageInterface

import com.zomato.photofilters.imageprocessors.Filter


interface FilterListFragmentListener {
    fun onFilterSelected(filter: Filter)
}