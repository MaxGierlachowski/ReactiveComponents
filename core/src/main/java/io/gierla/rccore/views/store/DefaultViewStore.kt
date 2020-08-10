package io.gierla.rccore.views.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.store.DefaultStore
import io.gierla.rccore.main.store.Store
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Structure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class DefaultViewStore<S : State, A : Action, V : Structure>(initialState: S, private val defaultStoreDelegate: Store<S, A> = DefaultStore(initialState)) : ViewStore<S, A, V>, Store<S, A> by defaultStoreDelegate {

    //private var controlledRunner = ControlledRunner<List<() -> Unit>>()

    private var stateDispatcher: StateDispatcher<S, V>? = null

    override fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>?) {
        this.stateDispatcher = stateDispatcher
    }

    override suspend fun applyChanges(view: V, oldState: S?, newState: S) {
        // TODO: do change calculation in background
        withContext(Dispatchers.Default) {
            //val changes = controlledRunner.cancelPreviousThenRun {
            val changes = stateDispatcher?.calculateChanges(view, oldState, newState) ?: emptyList()
            //}
            // Applying state and therefore executing ui operations must be done on the main thread
            withContext(Dispatchers.Main) {
                changes.forEach { it.invoke() }
            }
        }
    }

}