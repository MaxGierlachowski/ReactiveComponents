package io.gierla.rcsample.component

import android.graphics.Color
import io.gierla.rcviews.view.Variation

enum class MyViewTestVariation : Variation<MyViewTest.ViewStructure, MyViewTestStateDrawer> {
    MAIN {
        override fun init(view: MyViewTest.ViewStructure) {
            view.testView.setTextColor(Color.YELLOW)
        }

        override fun getStateDrawer(): MyViewTestStateDrawer = object : MyViewTestStateDrawer {
            override fun drawText(view: MyViewTest.ViewStructure, state: MyViewTest.ViewState) {
                view.testView.text = state.text.capitalize()
            }
        }
    }
}