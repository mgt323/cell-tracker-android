package com.mgt323.celltracker.ui.detail

import androidx.lifecycle.ViewModel
import com.mgt323.celltracker.data.repository.ExampleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


@HiltViewModel
class DetailViewModel @Inject constructor(private val exampleRepository: ExampleRepository) : ViewModel() {
    private val TAG = DetailViewModel::class.java.simpleName


}