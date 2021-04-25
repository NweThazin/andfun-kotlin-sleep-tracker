/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.launch

/**
 * ViewModel for SleepTrackerFragment.
 */
// ViewModel needs access to the data in the database
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {

    private val _tonight = MutableLiveData<SleepNight?>()
    val tonight: LiveData<SleepNight?> = _tonight

    private val nights = database.getAllNights()

    val nightsString = Transformations.map(nights) { newNights ->
        formatNights(newNights, application.resources)
    }

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight> = _navigateToSleepQuality

    val startButtonVisible: LiveData<Boolean> = Transformations.map(tonight) {
        null == it
    }

    val stopButtonVisible: LiveData<Boolean> = Transformations.map(tonight) {
        null != it
    }

    val clearButtonVisible: LiveData<Boolean> = Transformations.map(nights) {
        it?.isNotEmpty()
    }

    private val _showSnackBarEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent: LiveData<Boolean> = _showSnackBarEvent

    init {
        initializeTonight()
    }

    private fun initializeTonight() {
        viewModelScope.launch {
            _tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun getTonightFromDatabase(): SleepNight? {
        var night = database.getTonight()
        if (night?.endTimeMilli != night?.startTimeMilli) {
            night = null
        }
        return night
    }

    // call when click "Start" button from UI
    fun onStartTracking() {
        viewModelScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            _tonight.value = getTonightFromDatabase()
        }
    }

    private suspend fun insert(night: SleepNight) {
        database.insert(night)
    }

    // call when click "End" button from UI
    fun onStopTracking() {
        viewModelScope.launch {
            // get tonight value
            val oldNight = tonight.value ?: return@launch
            // set end time
            oldNight.endTimeMilli = System.currentTimeMillis()
            // update in db
            update(oldNight)

            _navigateToSleepQuality.value = oldNight
        }
    }

    fun doneNavigating() {
        _navigateToSleepQuality.value = null
    }

    private suspend fun update(oldNight: SleepNight) {
        database.update(oldNight)
    }

    // call when click "Clear" button on UI
    fun onClear() {
        viewModelScope.launch {
            clear()
            // clear tonight object
            _tonight.value = null

            _showSnackBarEvent.value = true
        }
    }

    private suspend fun clear() {
        database.clear()
    }

    fun doneShowingSnackbar() {
        _showSnackBarEvent.value = false
    }
}

