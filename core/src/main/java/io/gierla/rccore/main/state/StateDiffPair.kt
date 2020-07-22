package io.gierla.rccore.main.state

data class StateDiffPair<S: State>(
        val oldState: S?,
        val newState: S,
        val updateBool: Boolean
)