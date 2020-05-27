package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rcannotations.ReactiveComponent
import io.gierla.rccore.action.Action
import io.gierla.rccore.state.State
import io.gierla.rccore.view.Structure
import io.gierla.rcsample.R
import io.gierla.rcviews.helper.DefaultViewLifecycle
import io.gierla.rcviews.helper.ViewLifecycle
import io.gierla.rcviews.store.ReactiveView

@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0, viewLifecycle: ViewLifecycle = DefaultViewLifecycle()) :
    ConstraintLayout(context, attributeSet, defStyleAttr),
    ViewLifecycle by viewLifecycle,
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateDrawer> by MyViewTestImpl(viewLifecycle.lifecycle) {

    init {
        inflate(context, R.layout.my_view, this)
        findViewById<TextView>(R.id.text_view).setOnClickListener {
            dispatchAction(ViewAction.TextClick(getState().text))
        }

        setViewStructureGetter { viewStructure }
    }

    private val viewStructure: ViewStructure = object : ViewStructure {
        override val testView by lazy { findViewById<TextView>(R.id.text_view) }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachLifecycle()
    }

    override fun onDetachedFromWindow() {
        detachLifecycle()
        super.onDetachedFromWindow()
    }

    @io.gierla.rcannotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rcannotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rcannotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}