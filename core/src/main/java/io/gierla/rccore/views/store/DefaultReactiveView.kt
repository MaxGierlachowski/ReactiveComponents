package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.action.ActionListener
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateSubscriber
import io.gierla.rccore.views.helper.VariationBuilder
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import io.gierla.rccore.views.view.Variation
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class DefaultReactiveView<S : State, A : Action, V : Structure>(initialState: S) : ReactiveView<S, A, V> {

    private val store: ViewStore<S, A, V> = DefaultViewStore(initialState)

    private var storeJob: Job? = null

    private var viewStructureGetter: (() -> V)? = null

    private var viewStructure: V? = null

    private var changesJob: Job? = null

    override fun attachView() {
        if (this.viewStructure == null) {
            this.viewStructure = this.viewStructureGetter?.invoke()
        }
        storeJob = CoroutineScope(Dispatchers.Default).launch {
            store.subscribeState(object : StateSubscriber<S> {
                override suspend fun onNext(oldState: S?, newState: S) {
                    changesJob?.cancelAndJoin()
                    changesJob = launch {
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

    override fun setVariation(callback: ((view: V, oldState: S?, newState: S) -> Unit)?, variation: Variation<V, S>) {
        getViewStructure()?.let { variation.init(it) }
        setStateDispatcher(variation.getStateDispatcher())
    }

    override fun setVariation(callback: ((view: V, oldState: S?, newState: S) -> Unit)?, variationBuilder: VariationBuilder<V, S>.() -> Unit) {
        val variation = VariationBuilder<V, S>().apply(variationBuilder).build()
        getViewStructure()?.let { variation.init(it) }
        setStateDispatcher(variation.getStateDispatcher())
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