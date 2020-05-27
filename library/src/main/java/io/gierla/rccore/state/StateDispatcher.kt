package io.gierla.rccore.state

import io.gierla.rccore.view.Structure

interface StateDispatcher<S : State, V : Structure> {
    suspend fun calculateChanges(view: V, oldState: S?, newState: S): List<() -> Unit>
}