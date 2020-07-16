package io.gierla.rcsample.component

import android.content.Context
import android.os.Handler
import android.util.AttributeSet
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rcannotations.ReactiveComponent
import io.gierla.rccore.action.Action
import io.gierla.rccore.state.State
import io.gierla.rcsample.R
import io.gierla.rcviews.store.ReactiveView
import io.gierla.rcviews.view.Structure

@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) :
    ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateDrawer> by MyViewTestImpl(ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        Handler().postDelayed({
            setViewStructure {
                object : ViewStructure {
                    override val testView by lazy { findViewById<TextView>(R.id.text_view2) }
                }
            }
        }, 10000)

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