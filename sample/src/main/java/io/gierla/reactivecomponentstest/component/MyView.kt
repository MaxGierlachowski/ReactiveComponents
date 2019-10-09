package io.gierla.reactivecomponentstest.component

import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.reactivecomponents.ReactiveComponent
import io.gierla.reactivecomponents.Structure
import io.gierla.reactivecomponents.action.Action
import io.gierla.reactivecomponents.state.State

@ReactiveComponent(
    viewType = ConstraintLayout::class
)
class MyView {

    @io.gierla.reactivecomponents.State
    data class ViewState(
        val myInt: Int = 1,
        val someThinElse: String = "",
        val naUnd: Boolean = false
    ): State

    @io.gierla.reactivecomponents.Action
    sealed class ViewAction: Action {
        class MyAction(val actionNumber: Int): ViewAction()
    }

    @Structure
    interface ViewStructure {
        val imageView: ImageView
    }

}