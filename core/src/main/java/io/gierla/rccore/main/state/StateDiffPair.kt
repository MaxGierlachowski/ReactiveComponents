package io.gierla.rccore.main.state

data class StateDiffPair<S: State>(
        val state: S,
        val updateBool: Boolean
)