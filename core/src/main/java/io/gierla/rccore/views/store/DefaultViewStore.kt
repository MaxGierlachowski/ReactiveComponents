package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.store.DefaultStore
import io.gierla.rccore.main.store.Store
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
class DefaultViewStore<S : State, A : Action, V : Structure>(initialState: S, private val defaultStoreDelegate: Store<S, A> = DefaultStore(initialState)) : ViewStore<S, A, V>,
    Store<S, A> by defaultStoreDelegate {

    private var stateDispatcher: StateDispatcher<S, V>? = null

    override fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>?) {
        this.stateDispatcher = stateDispatcher
    }

    override suspend fun applyChanges(view: V, state: S) = withContext(Dispatchers.Default) {
        stateDispatcher?.dispatchChanges(view, state)
        return@withContext
    }

}