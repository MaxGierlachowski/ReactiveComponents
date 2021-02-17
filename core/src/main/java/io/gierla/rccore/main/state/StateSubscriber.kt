package io.gierla.rccore.main.state

interface StateSubscriber<S: State> {
    suspend fun onNext(state: S)
}