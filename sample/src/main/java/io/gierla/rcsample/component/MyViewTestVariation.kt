package io.gierla.rcsample.component

import android.graphics.Color
import io.gierla.rccore.views.view.Variation

enum class MyViewTestVariation : Variation<MyViewTest.ViewStructure, MyViewTestStateHandler> {
    MAIN {
        override fun init(view: MyViewTest.ViewStructure) {
            view.testView.setTextColor(Color.MAGENTA)
        }

        override fun getStateHandler(): MyViewTestStateHandler = object : MyViewTestStateHandler {
            override fun drawText(view: MyViewTest.ViewStructure, state: MyViewTest.ViewState) {
                view.testView.text = state.text.capitalize()
            }
        }
    }
}