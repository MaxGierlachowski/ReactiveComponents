package io.gierla.rcviews.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.helper.ControlledRunner
import io.gierla.rccore.state.State
import io.gierla.rccore.store.DefaultStore
import io.gierla.rccore.store.Store
import io.gierla.rcviews.view.StateDispatcher
import io.gierla.rcviews.view.Structure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class DefaultViewStore<S : State, A : Action, V : Structure>(initialState: S, private val defaultStoreDelegate: Store<S, A> = DefaultStore<S, A>(initialState)) : ViewStore<S, A, V>, Store<S, A> by defaultStoreDelegate {

    private var controlledRunner = ControlledRunner<List<() -> Unit>>()

    private var stateDispatcher: StateDispatcher<S, V>? = null

    override fun setStateDispatcher(stateDispatcher: StateDispatcher<S, V>?) {
        this.stateDispatcher = stateDispatcher
    }

    override suspend fun applyChanges(view: V) = withContext(Dispatchers.Default) {
        val changes = controlledRunner.cancelPreviousThenRun {
            stateDispatcher?.calculateChanges(view, getOldState(), getState()) ?: emptyList()
        }
        // Applying state and therefore executing ui operations must be done on the main thread
        withContext(Dispatchers.Main) {
            changes.forEach { it.invoke() }
        }
    }

}