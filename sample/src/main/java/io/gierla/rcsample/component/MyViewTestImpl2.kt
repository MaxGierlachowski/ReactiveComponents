package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rcviews.store.DefaultReactiveView
import io.gierla.rcviews.store.ReactiveView
import io.gierla.rcviews.view.Variation

abstract class MyViewTestImpl2 @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, private val reactiveView: DefaultReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateDrawer> = DefaultReactiveView(MyViewTest.ViewState())) :
    ConstraintLayout(context, attrs, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure, MyViewTestStateDrawer> by reactiveView {

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        reactiveView.onViewAttached()
    }

    override fun onDetachedFromWindow() {
        reactiveView.onViewDetached()
        super.onDetachedFromWindow()
    }

    override
    fun setVariation(variation: Variation<MyViewTest.ViewStructure, MyViewTestStateDrawer>, callback: ((view: MyViewTest.ViewStructure, oldState: MyViewTest.ViewState?, newState: MyViewTest.ViewState) -> Unit)?) {
        reactiveView.setVariation(variation)
        reactiveView.setStateDispatcher(MyViewTestStateDispatcher(variation.getStateDrawer(), callback))
    }
}