package io.gierla.rccore.main.store

import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.action.ActionListener
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateDiffPair
import io.gierla.rccore.main.state.StateSubscriber
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect

@ExperimentalCoroutinesApi
class DefaultStore<S : State, A : Action>(initialState: S) : Store<S, A> {

    private var changesJob: Job? = null

    // This is used because StateFlow checks for equality of objects and in some cases we want to force it to emit the same value twice (e.g. when the stateDispatcher of an reactive component is changed),
    // "forced" is not enough because we could set a new state dispatcher and a new view-structure right after each other and the view would only be notified once.
    private var updateBool = false

    private val stateSubscribers = MutableStateFlow(StateDiffPair(initialState, updateBool, false))

    private var actionListener: ActionListener<A>? = null

    private var oldState: S? = null
    private var state: S = initialState

    override fun getOldState(): S? {
        return oldState
    }

    override fun getState(): S {
        return state
    }

    override fun updateState(stateCallback: (S) -> S) {
        state = stateCallback.invoke(state)
        stateSubscribers.value = StateDiffPair(newState = state, updateBool = updateBool, forced = false)
    }

    override fun notifyState() {
        updateBool = !updateBool
        stateSubscribers.value = StateDiffPair(newState = state, updateBool = updateBool, forced = true)
    }

    override fun dispatchAction(action: A) {
        actionListener?.onNext(action)
    }

    override suspend fun subscribeState(subscriber: StateSubscriber<S>) = withContext(Dispatchers.Default) {
        stateSubscribers.collect {
            changesJob?.cancelAndJoin()
            changesJob = launch {
                subscriber.onNext(if (it.forced) null else oldState, it.newState)
                oldState = it.newState
            }
        }
    }

    override fun setActionListener(listener: ActionListener<A>) {
        this.actionListener = listener
    }

}