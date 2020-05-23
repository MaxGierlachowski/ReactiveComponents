package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.state.StateSubscriber
import io.reactivex.Observable
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.PublishSubject

class DefaultStore<S : State, A : Action>(
    private val initialState: S
) : Store<S, A> {

    private val stateSubscribers = PublishSubject.create<StateDiffPair<S>>()
    private val stateSubscriptions = CompositeDisposable()

    private var _actionListener: ActionListener<A>? = null

    private var state: S = initialState
        set(value) {
            stateSubscribers.onNext(
                StateDiffPair(
                    field,
                    value
                )
            )
            field = value
        }

    override fun updateState(stateCallback: (S) -> S) {
        state = state.let(stateCallback)
    }

    override fun getState(): S {
        return state
    }

    override fun dispatch(action: A) {
        // Send action to subscribers
        _actionListener?.onNext(action)
    }

    override fun subscribeState(subscriber: StateSubscriber<S>, options: ((Observable<StateDiffPair<S>>) -> Observable<StateDiffPair<S>>)?): Disposable {
        // Subscribe subscriber to state and return disposable to allow unsubscribeState(disposable: Disposable) later
        // Initialy call subscriber because we could already have a state. BehaviorSubject is going to call it the first time itself but oldState and newState would be the same and because of the diff it wouldn't be rendered

        val subject = stateSubscribers
        val configuredObservable = options?.invoke(subject) ?: subject
        val disposable = configuredObservable.subscribeBy(
            onNext = { subscriber.onNext(it.oldState, it.newState) },
            onComplete = { subscriber.onComplete() },
            onError = { subscriber.onError(it) }
        )
        stateSubscriptions.add(disposable)
        // Emit the the current state a single time after subsciption so that the whole ui will be updated once
        subscriber.onNext(null, state)
        return disposable
    }

    override fun unsubscribeState(disposable: Disposable) {
        // Dispose and remove disposable from CompositeDisposable
        stateSubscriptions.remove(disposable)
    }

    override fun setActionListener(listener: ActionListener<A>) {
        this._actionListener = listener
    }

    override fun destroy() {
        // Remove all remaining subscriptions
        stateSubscriptions.clear()
        _actionListener = null
        state = initialState
    }

}