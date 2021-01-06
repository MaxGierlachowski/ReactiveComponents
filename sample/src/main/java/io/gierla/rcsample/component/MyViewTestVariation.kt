package io.gierla.rcsample.component

import android.graphics.Color
import io.gierla.rccore.views.helper.variation
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
object MyViewTestVariations {
    val MAIN = variation<MyViewTest.ViewStructure, MyViewTestStateHandler<MyViewTest.ViewStructure, MyViewTest.ViewState>> {
        init { view ->
            view.testView.setTextColor(Color.MAGENTA)
        }
        stateHandler {
            myViewTestStateHandler {
                drawText { view, state ->
                    view.testView.text = state.text.capitalize()
                }
            }
        }
    }
}