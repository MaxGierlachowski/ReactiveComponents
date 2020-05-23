package io.gierla.rcsample

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import io.gierla.rccore.action.ActionListener
import io.gierla.rcsample.component.MyView
import io.gierla.rcsample.component.MyViewVariation

class Main : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myView = findViewById<MyView>(R.id.my_view)
        myView.setVariation(MyViewVariation.MAIN, null)
        myView.store.setActionListener(object : ActionListener<MyView.ViewAction> {
            override fun onNext(action: MyView.ViewAction) {
                when (action) {
                    is MyView.ViewAction.TextClick -> {
                        Toast.makeText(applicationContext, action.text, Toast.LENGTH_LONG).show()
                    }
                }
            }
        })
        myView.store.updateState { currentState ->
            currentState.copy(
                text = "Hello World!"
            )
        }
    }
}
