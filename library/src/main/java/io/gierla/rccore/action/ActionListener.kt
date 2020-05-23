package io.gierla.rccore.action

interface ActionListener<A: Action> {
    fun onNext(action: A)
}