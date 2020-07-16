package io.gierla.rccore.store

import io.gierla.rccore.action.Action
import io.gierla.rccore.action.ActionListener
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.state.StateSubscriber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext

@ExperimentalCoroutinesApi
class DefaultStore<S : State, A : Action>(initialState: S) : Store<S, A> {

    // This is used because StateFlow checks for equality of objects and in some cases we want to force it to emit the same value twice (e.g. when the stateDispatcher of an reactive component is changed)
    private var updateBool = false

    private val stateSubscribers = MutableStateFlow(StateDiffPair(null, initialState, updateBool))

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
        oldState = state
        state = stateCallback.invoke(state)
        stateSubscribers.value = StateDiffPair(oldState = oldState, newState = state, updateBool = updateBool)
    }

    override fun notifyState() {
        oldState = null
        updateBool = !updateBool
        stateSubscribers.value = StateDiffPair(oldState = oldState, newState = state, updateBool = updateBool)
    }

    override fun dispatchAction(action: A) {
        actionListener?.onNext(action)
    }

    override suspend fun subscribeState(subscriber: StateSubscriber<S>) = withContext(Dispatchers.Default) {
        stateSubscribers.collect {
            subscriber.onNext(it.oldState, it.newState)
        }
    }

    override fun setActionListener(listener: ActionListener<A>) {
        this.actionListener = listener
    }

}