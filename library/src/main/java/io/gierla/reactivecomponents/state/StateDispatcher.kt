package io.gierla.reactivecomponents.state

import io.gierla.reactivecomponents.view.Structure

interface StateDispatcher<V : Structure, S : State> {
    fun dispatchUpdates(view: V, oldState: S?, newState: S)
}