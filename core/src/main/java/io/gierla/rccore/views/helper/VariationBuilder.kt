package io.gierla.rccore.views.helper

import io.gierla.rccore.main.state.State
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import io.gierla.rccore.views.view.Variation

class VariationBuilder<V : Structure, S : State> {
    private var initFunc: (view: V) -> Unit = {}
    private lateinit var stateDispatcherFunc: () -> StateDispatcher<S, V>

    fun init(initFunc: (view: V) -> Unit) {
        this.initFunc = initFunc
    }

    fun stateDispatcher(stateDispatcherFunc: () -> StateDispatcher<S, V>) {
        this.stateDispatcherFunc = stateDispatcherFunc
    }

    fun build(): Variation<V, S> = object : Variation<V, S> {
        override fun init(view: V) = initFunc(view)
        override fun getStateDispatcher(): StateDispatcher<S, V> = stateDispatcherFunc()
    }
}

fun <V : Structure, S : State> variation(initializer: VariationBuilder<V, S>.() -> Unit): Variation<V, S> {
    return VariationBuilder<V, S>().apply(initializer).build()
}