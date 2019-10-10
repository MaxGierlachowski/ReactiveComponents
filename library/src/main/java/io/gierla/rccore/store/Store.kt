package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionSubscriber
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateSubscriber
import io.reactivex.disposables.Disposable

interface Store<S : State, A : Action> {
    fun dispatch(action: A)
    fun subscribeState(subscriber: StateSubscriber<S>): Disposable
    fun unsubscribeState(disposable: Disposable)
    fun subscribeAction(subscriber: ActionSubscriber<A>): Disposable
    fun unsubscribeAction(disposable: Disposable)
    fun updateState(stateCallback: (S) -> S)
    fun getState(): S
    fun destroy()
    fun destroyActionListener()
    fun destroyStateListener()
    fun setOnDestroyActionListener(onDestroy: () -> Unit)
    fun setOnDestroyStateListener(onDestroy: () -> Unit)
}