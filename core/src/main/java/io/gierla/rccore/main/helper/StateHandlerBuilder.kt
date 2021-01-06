package io.gierla.rccore.main.helper

import io.gierla.rccore.main.state.StateHandler

interface StateHandlerBuilder<D : StateHandler> {
    fun build(): D
}