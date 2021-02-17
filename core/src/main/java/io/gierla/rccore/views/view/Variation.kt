package io.gierla.rccore.views.view

import io.gierla.rccore.main.state.State

interface Variation<V: Structure, S: State> {
    fun init(view: V) {}
    fun getStateDispatcher(): StateDispatcher<S, V>
}