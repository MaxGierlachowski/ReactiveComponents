package io.gierla.rcsample.helper.viewbinding

import android.view.View
import androidx.viewbinding.ViewBinding

class ViewBindingHolderImpl<T : ViewBinding> : ViewBindingHolder<T> {

    override var binding: T? = null

    override fun requireBinding(block: (T.() -> Unit)?) =
        binding?.apply { block?.invoke(this) } ?: throw IllegalStateException("Accessing binding outside of lifecycle!")

    override fun initBinding(binding: T, onBound: (T.() -> Unit)?): View {
        this.binding = binding
        onBound?.invoke(binding)
        return binding.root
    }

    override fun cleanupBinding() {
        binding = null
    }
}