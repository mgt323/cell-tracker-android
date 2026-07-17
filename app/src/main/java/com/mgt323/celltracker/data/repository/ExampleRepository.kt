package com.mgt323.celltracker.data.repository

import com.mgt323.celltracker.data.remote.AppApi
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ExampleRepository @Inject constructor(private val appApi: AppApi)