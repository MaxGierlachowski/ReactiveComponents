package io.gierla.rccore.state

interface StateSubscriber<S: State> {
    fun onError(error: Throwable)
    fun onComplete()
    fun onNext(oldState: S?, newState: S)
}