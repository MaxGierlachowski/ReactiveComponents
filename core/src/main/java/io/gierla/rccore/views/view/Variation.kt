package io.gierla.rccore.views.view

import io.gierla.rccore.main.state.StateHandler

interface Variation<V: Structure, D: StateHandler> {
    fun init(view: V) {}
    fun getStateHandler(): D
}