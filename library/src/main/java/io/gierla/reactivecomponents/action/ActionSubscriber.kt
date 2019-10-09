package io.gierla.reactivecomponents.action

interface ActionSubscriber<A: Action> {
    fun onError(error: Throwable)
    fun onComplete()
    fun onNext(action: A)
}