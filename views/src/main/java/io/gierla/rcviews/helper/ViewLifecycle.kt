package io.gierla.rcviews.helper

import androidx.lifecycle.LifecycleOwner

interface ViewLifecycle : LifecycleOwner {
    fun attachLifecycle()
    fun detachLifecycle()
}