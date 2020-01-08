package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rccore.action.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.state.State
import io.gierla.rccore.view.Structure

@ReactiveComponent(viewType = ConstraintLayout::class)
class MyView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : MyViewImpl(context, attributeSet, defStyleAttr) {

    override fun getViewStructure(): ViewStructure = object : ViewStructure {

    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ): State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction: Action {

    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {

    }

}