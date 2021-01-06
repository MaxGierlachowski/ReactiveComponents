package io.gierla.rccore.views.helper

import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.views.view.Structure
import io.gierla.rccore.views.view.Variation

class VariationBuilder<V : Structure, D : StateHandler> {
    private var initFunc: (view: V) -> Unit = {}
    private lateinit var stateHandlerFunc: () -> D

    fun init(initFunc: (view: V) -> Unit) {
        this.initFunc = initFunc
    }

    fun stateHandler(stateHandlerFunc: () -> D) {
        this.stateHandlerFunc = stateHandlerFunc
    }

    fun build(): Variation<V, D> = object : Variation<V, D> {
        override fun getStateHandler(): D = stateHandlerFunc()
        override fun init(view: V) = initFunc(view)
    }
}

fun <V : Structure, D : StateHandler> variation(initializer: VariationBuilder<V, D>.() -> Unit): Variation<V, D> {
    return VariationBuilder<V, D>().apply(initializer).build()
}