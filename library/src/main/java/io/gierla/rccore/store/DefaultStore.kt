package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionSubscriber
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.state.StateSubscriber
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import io.reactivex.subjects.PublishSubject

class DefaultStore<S : State, A : Action>(
    private val initialState: S
) : Store<S, A> {

    private val stateSubscribers = BehaviorSubject.create<StateDiffPair<S>>()
    private val stateSubscriptions = CompositeDisposable()

    private val actionSubscribers = PublishSubject.create<A>()
    private val actionSubscriptions = CompositeDisposable()

    private var onDestroyStateListener: () -> Unit = {}
    private var onDestroyActionListener: () -> Unit = {}

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
        actionSubscribers.onNext(action)
    }

    override fun subscribeState(subscriber: StateSubscriber<S>, options: ((Observable<StateDiffPair<S>>) -> Observable<StateDiffPair<S>>)?): Disposable {
        // Subscribe subscriber to state and return disposable to allow unsubscribeState(disposable: Disposable) later
        // Initialy call subscriber because we could already have a state. BehaviorSubject is going to call it the first time itself but oldState and newState would be the same and because of the diff it wouldn't be rendered
        subscriber.onNext(initialState, state)
        val subject = stateSubscribers
        val configuredObservable = options?.invoke(subject) ?: subject
        val disposable = configuredObservable.subscribeBy(
            onNext = { subscriber.onNext(it.oldState, it.newState) },
            onComplete = { subscriber.onComplete() },
            onError = { subscriber.onError(it) }
        )
        stateSubscriptions.add(disposable)
        return disposable
    }

    override fun unsubscribeState(disposable: Disposable) {
        // Dispose and remove disposable from CompositeDisposable
        stateSubscriptions.remove(disposable)
    }

    override fun subscribeAction(subscriber: ActionSubscriber<A>): Disposable {
        // Subscribe subscriber to actions and return disposable to allow unsubscribeAction(disposable: Disposable) later
        val disposable = actionSubscribers.subscribeBy(
            onNext = { subscriber.onNext(it) },
            onComplete = { subscriber.onComplete() },
            onError = { subscriber.onError(it) }
        )
        actionSubscriptions.add(disposable)
        return disposable
    }

    override fun unsubscribeAction(disposable: Disposable) {
        // Dispose and remove disposable from CompositeDisposable
        actionSubscriptions.remove(disposable)
    }

    override fun destroyActionListener() {
        actionSubscriptions.clear()
        onDestroyActionListener.invoke()
    }

    override fun destroyStateListener() {
        stateSubscriptions.clear()
        onDestroyStateListener.invoke()
        state = initialState
    }

    override fun destroy() {
        // Remove all remaining subscriptions
        stateSubscriptions.clear()
        onDestroyStateListener.invoke()
        actionSubscriptions.clear()
        onDestroyActionListener.invoke()
        state = initialState
    }

    override fun setOnDestroyActionListener(onDestroy: () -> Unit) {
        onDestroyActionListener = onDestroy
    }

    override fun setOnDestroyStateListener(onDestroy: () -> Unit) {
        onDestroyStateListener = onDestroy
    }
}