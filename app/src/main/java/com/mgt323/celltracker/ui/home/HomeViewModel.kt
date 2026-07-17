package com.mgt323.celltracker.ui.home

import androidx.lifecycle.ViewModel
import com.mgt323.celltracker.data.repository.ExampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val exampleRepository: ExampleRepository) : ViewModel() {
    private val TAG = HomeViewModel::class.java.simpleName


}