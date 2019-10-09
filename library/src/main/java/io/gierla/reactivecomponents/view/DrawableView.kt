package io.gierla.reactivecomponents.view

import io.gierla.reactivecomponents.state.State
import io.gierla.reactivecomponents.state.StateDispatcher

interface DrawableView<V: Structure, S: State> {
    var dispatcher: StateDispatcher<V, S>
}