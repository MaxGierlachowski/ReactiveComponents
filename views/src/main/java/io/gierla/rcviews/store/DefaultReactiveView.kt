package io.gierla.rcviews.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDrawer
import io.gierla.rccore.state.StateSubscriber
import io.gierla.rcviews.view.StateDispatcher
import io.gierla.rcviews.view.Structure
import io.gierla.rcviews.view.Variation
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class DefaultReactiveView<S : State, A : Action, V : Structure, D : StateDrawer>(initialState: S) : ReactiveView<S, A, V, D> {

    private val store: ViewStore<S, A, V> = DefaultViewStore(initialState = initialState)

    override var viewStructure: V? = null

    private var storeJob: Job? = null

    fun onViewAttached() {
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

    fun onViewDetached() {
        storeJob?.cancel()
    }

    override fun updateState(stateCallback: (S) -> S) = store.updateState(stateCallback)

    override fun getState(): S = store.getState()

    override suspend fun subscribeState(subscriber: StateSubscriber<S>) = store.subscribeState(subscriber)

    override fun dispatchAction(action: A) = store.dispatchAction(action)

    override fun setActionListener(listener: ActionListener<A>) = store.setActionListener(listener)

    fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>) = store.setStateDispatcher(stateDispatcher)

    override fun setVariation(variation: Variation<V, D>, callback: ((view: V, oldState: S?, newState: S) -> Unit)?) {
        viewStructure?.let {
            variation.init(it)
        }
    }
}