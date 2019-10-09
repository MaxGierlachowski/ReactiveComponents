package io.gierla.reactivecomponents.state

data class StateDiffPair<S: State>(
        val oldState: S,
        val newState: S
)