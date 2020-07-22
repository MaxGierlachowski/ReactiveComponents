package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rcsample.R
import io.gierla.rccore.views.store.ReactiveView
import io.gierla.rccore.views.view.Structure

@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateHandler> by MyViewTestImpl(ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        requireViewStructure().run {
            testView.setOnClickListener {
                dispatchAction(ViewAction.TextClick(getState().text))
            }
            testView.isAllCaps = true
        }
    }

    override fun onDetachedFromWindow() {
        detachView()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachView()
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}