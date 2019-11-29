package io.gierla.rcsample.component

import io.gierla.rccore.view.Variation

enum class MyViewVariation : Variation<MyView.ViewStructure, MyViewStateDrawer> {
    TEST {
        override fun init(view: MyView.ViewStructure) {}

        override fun getStateDrawer(): MyViewStateDrawer = object : MyViewStateDrawer {
            override fun drawMyInt(view: MyView.ViewStructure, state: MyView.ViewState) {}

            override fun drawSomeThinElse(view: MyView.ViewStructure, state: MyView.ViewState) {}

            override fun drawNaUnd(view: MyView.ViewStructure, state: MyView.ViewState) {}
        }
    }
}