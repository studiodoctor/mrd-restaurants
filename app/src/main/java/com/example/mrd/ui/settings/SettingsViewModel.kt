package com.example.mrd.ui.settings

import androidx.lifecycle.ViewModel
import com.example.mrd.data.remote.NetworkSimulator
import com.example.mrd.data.remote.SimulatedNetwork
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.StateFlow

/** Backs the debug-only "Developer options" section (see NetworkSimulator). */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val networkSimulator: NetworkSimulator,
) : ViewModel() {
    val networkMode: StateFlow<SimulatedNetwork> = networkSimulator.mode

    fun setNetworkMode(mode: SimulatedNetwork) = networkSimulator.set(mode)
}
