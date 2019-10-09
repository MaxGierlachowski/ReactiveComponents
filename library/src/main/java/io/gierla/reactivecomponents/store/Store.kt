package io.gierla.reactivecomponents.store

import io.gierla.reactivecomponents.action.Action
import io.gierla.reactivecomponents.action.ActionSubscriber
import io.gierla.reactivecomponents.state.State
import io.gierla.reactivecomponents.state.StateSubscriber
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