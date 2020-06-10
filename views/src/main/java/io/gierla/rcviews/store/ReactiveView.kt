package io.gierla.rcviews.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDrawer
import io.gierla.rccore.state.StateSubscriber
import io.gierla.rcviews.view.Structure
import io.gierla.rcviews.view.Variation

interface ReactiveView<S: State, A: Action, V: Structure, D: StateDrawer> {
    var viewStructure: V?

    fun updateState(stateCallback: (S) -> S)
    fun getState(): S
    suspend fun subscribeState(subscriber: StateSubscriber<S>)
    fun dispatchAction(action: A)
    fun setActionListener(listener: ActionListener<A>)
    fun setVariation(variation: Variation<V, D>, callback: ((view: V, oldState: S?, newState: S) -> Unit)? = null)
}