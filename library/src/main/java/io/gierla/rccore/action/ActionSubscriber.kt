package io.gierla.rccore.action

interface ActionSubscriber<A: Action> {
    fun onError(error: Throwable)
    fun onComplete()
    fun onNext(action: A)
}