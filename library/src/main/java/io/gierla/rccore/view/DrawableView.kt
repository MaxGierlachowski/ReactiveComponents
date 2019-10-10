package io.gierla.rccore.view

import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDispatcher

interface DrawableView<V: Structure, S: State> {
    var dispatcher: StateDispatcher<V, S>
}