package io.gierla.rccore.main.state

data class StateDiffPair<S: State>(
        val state: S,
        val emit: Boolean,
        val updateBool: Boolean
)