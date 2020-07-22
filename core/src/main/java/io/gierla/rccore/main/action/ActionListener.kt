package io.gierla.rccore.main.action

interface ActionListener<A: Action> {
    fun onNext(action: A)
}