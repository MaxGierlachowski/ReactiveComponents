package io.gierla.rcviews.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDrawer
import io.gierla.rccore.state.StateSubscriber
import io.gierla.rcviews.view.StateDispatcher
import io.gierla.rcviews.view.Structure
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
abstract class DefaultReactiveView<S : State, A : Action, V : Structure, D : StateDrawer>(initialState: S) : ReactiveView<S, A, V, D> {

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
                            store.applyChanges(viewStructure)
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