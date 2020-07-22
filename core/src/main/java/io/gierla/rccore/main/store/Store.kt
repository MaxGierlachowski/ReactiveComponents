package io.gierla.rccore.main.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.action.ActionListener
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateSubscriber

interface Store<S : State, A : Action> {
    fun updateState(stateCallback: (S) -> S)
    fun notifyState()
    suspend fun subscribeState(subscriber: StateSubscriber<S>)
    fun dispatchAction(action: A)
    fun setActionListener(listener: ActionListener<A>)
    fun getOldState(): S?
    fun getState(): S
}