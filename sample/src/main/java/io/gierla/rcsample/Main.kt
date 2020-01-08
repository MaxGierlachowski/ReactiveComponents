package io.gierla.rcsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rccore.annotations.RCReceivers

@RCReceivers(receivers = [ConstraintLayout::class, ConstraintLayout::class, ImageView::class])
class Main : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }
}
