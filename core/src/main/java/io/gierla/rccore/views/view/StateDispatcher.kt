package io.gierla.rccore.views.view

import io.gierla.rccore.main.state.State

interface StateDispatcher<S : State, V : Structure> {
    suspend fun calculateChanges(view: V, oldState: S?, newState: S): List<() -> Unit>
}