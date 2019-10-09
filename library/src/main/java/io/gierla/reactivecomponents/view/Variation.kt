package io.gierla.reactivecomponents.view

import io.gierla.reactivecomponents.state.StateDrawer

interface Variation<V: Structure, D: StateDrawer> {
    fun init(view: V)
    fun getStateDrawer(): D
}