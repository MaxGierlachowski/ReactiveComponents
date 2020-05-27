package io.gierla.rcviews.store

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDispatcher
import io.gierla.rccore.state.StateDrawer
import io.gierla.rccore.state.StateSubscriber
import io.gierla.rccore.view.Structure
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
abstract class DefaultReactiveView<S : State, A : Action, V : Structure, D: StateDrawer>(lifecycle: Lifecycle, initialState: S) : ReactiveView<S, A, V, D>, LifecycleObserver {

    private val store: ViewStore<S, A, V> = DefaultViewStore(initialState = initialState)

    override var viewStructure: V? = null

    private var storeJob: Job? = null

    init {
        lifecycle.addObserver(this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
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

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onViewDetached() {
        storeJob?.cancel()
    }

    override fun updateState(stateCallback: (S) -> S) = store.updateState(stateCallback)

    override fun getState(): S = store.getState()

    override suspend fun subscribeState(subscriber: StateSubscriber<S>) = store.subscribeState(subscriber)

    override fun dispatchAction(action: A) = store.dispatchAction(action)

    override fun setActionListener(listener: ActionListener<A>) = store.setActionListener(listener)

    override fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>) = store.setStateDispatcher(stateDispatcher)

}