package io.gierla.rccore.state

interface StateSubscriber<S: State> {
    suspend fun onNext(oldState: S?, newState: S)
}