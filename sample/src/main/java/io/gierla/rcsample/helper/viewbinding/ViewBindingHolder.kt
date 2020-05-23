package io.gierla.rcsample.helper.viewbinding

import android.view.View
import androidx.viewbinding.ViewBinding

/**
 * Holds and manages ViewBinding inside a Fragment.
 */
interface ViewBindingHolder<T : ViewBinding> {

    val binding: T?

    /**
     * Saves the binding for cleanup, calls the specified function [onBound] with `this` value
     * as its receiver and returns the bound view root.
     */
    fun initBinding(binding: T, onBound: (T.() -> Unit)?): View

    /**
     * Cleans up ViewBinding
     * */
    fun cleanupBinding()

    /**
     * Calls the specified [block] with the binding as `this` value and returns the binding. As a consequence, this method
     * can be used with a code block lambda in [block] or to initialize a variable with the return type.
     *
     * @throws IllegalStateException if not currently holding a ViewBinding (when called outside of an active fragment's lifecycle)
     */
    fun requireBinding(block: (T.() -> Unit)? = null): T
}