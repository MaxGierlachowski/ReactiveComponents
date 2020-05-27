package io.gierla.rccore.state

data class StateDiffPair<S: State>(
        val oldState: S?,
        val newState: S
)