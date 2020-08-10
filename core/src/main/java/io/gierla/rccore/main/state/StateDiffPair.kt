package io.gierla.rccore.main.state

data class StateDiffPair<S: State>(
        val newState: S,
        val updateBool: Boolean,
        val forced: Boolean
)