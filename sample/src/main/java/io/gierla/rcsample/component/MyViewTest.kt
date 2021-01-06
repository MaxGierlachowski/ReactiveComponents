package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.main.action.Action
import io.gierla.rccore.main.state.State
import io.gierla.rccore.views.view.Structure
import io.gierla.rcsample.R
import io.gierla.rcsample.component.utils.ReactiveConstraintLayout
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ReactiveConstraintLayout<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateHandler>(MyViewTestImpl(ViewState()), context, attributeSet, defStyleAttr) {

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