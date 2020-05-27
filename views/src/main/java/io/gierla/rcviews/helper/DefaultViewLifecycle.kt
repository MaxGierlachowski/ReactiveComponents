package io.gierla.rcviews.helper

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry

class DefaultViewLifecycle : ViewLifecycle {

    private val lifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    override fun attachLifecycle() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun detachLifecycle() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
    }

    override fun getLifecycle(): Lifecycle {
        return lifecycleRegistry
    }

}