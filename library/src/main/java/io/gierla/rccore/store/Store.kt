package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.state.StateSubscriber
import io.reactivex.Observable
import io.reactivex.disposables.Disposable

interface Store<S : State, A : Action> {
    fun dispatch(action: A)
    fun subscribeState(subscriber: StateSubscriber<S>, options: ((Observable<StateDiffPair<S>>) -> Observable<StateDiffPair<S>>)? = null): Disposable
    fun unsubscribeState(disposable: Disposable)
    fun updateState(stateCallback: (S) -> S)
    fun setActionListener(listener: ActionListener<A>)
    fun getState(): S
    fun destroy()
}