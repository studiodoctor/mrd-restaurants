package com.example.mrd.data.remote

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class SimulatedNetwork { ONLINE, OFFLINE, SERVER_ERROR }

/**
 * Lets reviewers flip the mock backend into failure modes at runtime (debug builds expose this in
 * the toolbar menu) so the offline baseline and error states can be demonstrated on demand
 * without toggling airplane mode.
 */
@Singleton
class NetworkSimulator @Inject constructor() {
    private val _mode = MutableStateFlow(SimulatedNetwork.ONLINE)
    val mode: StateFlow<SimulatedNetwork> = _mode

    fun set(mode: SimulatedNetwork) {
        _mode.value = mode
    }
}
