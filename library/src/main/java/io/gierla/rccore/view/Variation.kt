package io.gierla.rccore.view

import io.gierla.rccore.state.StateDrawer

interface Variation<V: Structure, D: StateDrawer> {
    fun init(view: V)
    fun getStateDrawer(): D
}