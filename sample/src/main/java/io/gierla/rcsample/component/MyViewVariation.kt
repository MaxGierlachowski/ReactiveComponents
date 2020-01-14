package io.gierla.rcsample.component

import io.gierla.rccore.view.Variation

enum class MyViewVariation : Variation<MyView.ViewStructure, MyViewStateDrawer> {
    MAIN {
        override fun init(view: MyView.ViewStructure) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun getStateDrawer(): MyViewStateDrawer = object : MyViewStateDrawer {

        }
    }
}