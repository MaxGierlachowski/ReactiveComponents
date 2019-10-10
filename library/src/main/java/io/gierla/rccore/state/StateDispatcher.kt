package io.gierla.rccore.state

import io.gierla.rccore.view.Structure

interface StateDispatcher<V : Structure, S : State> {
    fun dispatchUpdates(view: V, oldState: S?, newState: S)
}