package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.action.ActionListener
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.main.state.StateSubscriber
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
abstract class DefaultReactiveView<S : State, A : Action, V : Structure, D : StateHandler>(initialState: S) : ReactiveView<S, A, V, D> {

    private val store: ViewStore<S, A, V> = DefaultViewStore(initialState = initialState)

    private var storeJob: Job? = null

    private var viewStructureGetter: (() -> V)? = null

    private var viewStructure: V? = null

    override fun attachView() {
        if (this.viewStructure == null) {
            this.viewStructure = this.viewStructureGetter?.invoke()
        }
        storeJob = CoroutineScope(Dispatchers.Main).launch {
            store.subscribeState(object : StateSubscriber<S> {
                override suspend fun onNext(oldState: S?, newState: S) {
                    launch {
                        viewStructure?.let { viewStructure ->
                            store.applyChanges(viewStructure, oldState, newState)
                        }
                    }
                }
            })
        }
    }

    override fun detachView() {
        storeJob?.cancel()
        storeJob = null
        viewStructure = null
    }

    override fun requireViewStructure(): V = viewStructure!!

    override fun getViewStructure(): V? = viewStructure

    override fun setViewStructure(viewStructureGetter: (() -> V)?) {
        this.viewStructureGetter = viewStructureGetter
        this.viewStructure = viewStructureGetter?.invoke()
        this.store.notifyState()
    }

    override fun updateState(stateCallback: (S) -> S) = store.updateState(stateCallback)

    override fun getState(): S = store.getState()

    override suspend fun subscribeState(subscriber: StateSubscriber<S>) = store.subscribeState(subscriber)

    override fun dispatchAction(action: A) = store.dispatchAction(action)

    override fun setActionListener(listener: ActionListener<A>) = store.setActionListener(listener)

    override fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>) {
        store.setStateDispatcher(stateDispatcher)
        store.notifyState()
    }

}