package io.gierla.rcsample.component.utils

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.views.store.ReactiveView
import io.gierla.rccore.views.view.Structure

open class ReactiveConstraintLayout<S: State, A: Action, V: Structure, D: StateHandler> @JvmOverloads constructor(delegate: ReactiveView<S, A, V, D>, context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr), ReactiveView<S, A, V, D> by delegate {

    override fun onDetachedFromWindow() {
        detachView()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachView()
    }

}