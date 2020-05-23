package io.gierla.rcsample.component

import android.graphics.Color
import io.gierla.rccore.view.Variation

enum class MyViewVariation : Variation<MyView.ViewStructure, MyViewStateDrawer> {
    MAIN {
        override fun init(view: MyView.ViewStructure) {
            view.testView.setTextColor(Color.BLACK)
        }

        override fun getStateDrawer(): MyViewStateDrawer = object : MyViewStateDrawer {
            override fun drawText(view: MyView.ViewStructure, state: MyView.ViewState) {
                view.testView.text = state.text
            }
        }
    }
}