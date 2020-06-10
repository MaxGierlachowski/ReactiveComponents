package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.widget.TextView
import io.gierla.rcannotations.ReactiveComponent
import io.gierla.rccore.action.Action
import io.gierla.rccore.state.State
import io.gierla.rcsample.R
import io.gierla.rcviews.view.Structure

@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : MyViewTestImpl2(context, attributeSet, defStyleAttr) {

    init {
        inflate(context, R.layout.my_view, this)
        findViewById<TextView>(R.id.text_view).setOnClickListener {
            dispatchAction(ViewAction.TextClick(getState().text))
        }

        viewStructure = object : ViewStructure {
            override val testView by lazy { findViewById<TextView>(R.id.text_view) }
        }
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