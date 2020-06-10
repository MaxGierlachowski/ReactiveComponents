package io.gierla.rcviews.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.state.State
import io.gierla.rccore.store.Store
import io.gierla.rcviews.view.StateDispatcher
import io.gierla.rcviews.view.Structure

interface ViewStore<S: State, A: Action, V: Structure> : Store<S, A> {
    fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>?)
    suspend fun applyChanges(view: V)
}