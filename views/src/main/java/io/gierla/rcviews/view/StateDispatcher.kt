package io.gierla.rcviews.view

import io.gierla.rccore.state.State

interface StateDispatcher<S : State, V : Structure> {
    suspend fun calculateChanges(view: V, oldState: S?, newState: S): List<() -> Unit>
}