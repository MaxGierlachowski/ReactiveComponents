package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateSubscriber

interface Store<S : State, A : Action> {
    fun updateState(stateCallback: (S) -> S)
    fun notifyState()
    suspend fun subscribeState(subscriber: StateSubscriber<S>)
    fun dispatchAction(action: A)
    fun setActionListener(listener: ActionListener<A>)
    fun getOldState(): S?
    fun getState(): S
}