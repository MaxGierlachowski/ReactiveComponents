package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rcannotations.ReactiveComponent
import io.gierla.rcannotations.Structure
import io.gierla.rccore.action.Action
import io.gierla.rccore.state.State
import io.gierla.rcsample.R

@ReactiveComponent(
    viewType = ConstraintLayout::class
)
class MyView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : ViewImpl(context, attrs, defStyleAttr) {

    override fun getViewStructure(): ViewStructure = object : ViewStructure {
        override val imageView: ImageView by lazy { findViewById<ImageView>(R.id.test) }
    }

    init {

        LayoutInflater.from(context).inflate(R.layout.my_view, this, true)
        store.dispatch(ViewAction.MyAction(1))

    }

    @io.gierla.rcannotations.State
    data class ViewState(
        val myInt: Int = 1,
        val someThinElse: String = "",
        val naUnd: Boolean = false
    ) : State

    @io.gierla.rcannotations.Action
    sealed class ViewAction : Action {
        class MyAction(val actionNumber: Int) : ViewAction()
    }

    @Structure
    interface ViewStructure : io.gierla.rccore.view.Structure {
        val imageView: ImageView
    }

}