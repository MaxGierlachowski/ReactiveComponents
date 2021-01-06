package io.gierla.rccore.main.action

fun interface ActionListener<A: Action> {
    fun onNext(action: A)
}