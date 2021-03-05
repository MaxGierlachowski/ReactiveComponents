package io.gierla.rccore.views.view

import io.gierla.rccore.main.state.State

abstract class StateDispatcher<S : State, V : Structure> {
    protected var oldState: S? = null

    abstract fun dispatchChanges(view: V, state: S)
}