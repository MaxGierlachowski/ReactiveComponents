package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.action.ActionListener
import io.gierla.rccore.main.helper.StateHandlerBuilder
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.main.state.StateSubscriber
import io.gierla.rccore.views.helper.VariationBuilder
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import io.gierla.rccore.views.view.Variation

interface ReactiveView<S: State, A: Action, V: Structure, D: StateHandler> {

    // Managing the view
    fun requireViewStructure(): V
    fun getViewStructure(): V?
    fun setViewStructure(viewStructureGetter: (() -> V)?)

    // Managing the state
    fun updateState(stateCallback: (S) -> S)
    fun getState(): S
    suspend fun subscribeState(subscriber: StateSubscriber<S>)

    // Managing actions
    fun dispatchAction(action: A)
    fun setActionListener(listener: ActionListener<A>)

    // Managing rendering
    fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>)
    fun setVariation(callback: ((view: V, oldState: S?, newState: S) -> Unit)? = null, variation: Variation<V, D>)
    fun setVariation(callback: ((view: V, oldState: S?, newState: S) -> Unit)? = null, variationBuilder: VariationBuilder<V, D>.() -> Unit)

    // Managing lifecycle
    fun detachView()
    fun attachView()
}