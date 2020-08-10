package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.store.Store
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure

interface ViewStore<S: State, A: Action, V: Structure> : Store<S, A> {
    fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>?)
    suspend fun applyChanges(view: V, oldState: S?, newState: S)
}