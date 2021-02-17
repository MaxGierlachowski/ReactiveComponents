package io.gierla.rcsample.component

import android.graphics.Color
import kotlinx.coroutines.ExperimentalCoroutinesApi

@ExperimentalCoroutinesApi
object MyViewTestVariations {
    val MAIN = myViewTestVariation {
        init { view ->
            view.testView.setTextColor(Color.MAGENTA)
        }
        stateHandler {
            drawText { view, state ->
                view.testView.text = state.text.capitalize()
            }
        }
    }
}