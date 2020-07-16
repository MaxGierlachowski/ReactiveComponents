package io.gierla.rcsample

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import io.gierla.rccore.action.ActionListener
import io.gierla.rcsample.component.MyViewTest
import io.gierla.rcsample.component.MyViewTestVariation
import kotlinx.coroutines.ExperimentalCoroutinesApi

class Main : AppCompatActivity() {

    @ExperimentalCoroutinesApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myView = findViewById<MyViewTest>(R.id.my_view)
        myView.setVariation(MyViewTestVariation.MAIN)
        myView.setActionListener(object : ActionListener<MyViewTest.ViewAction> {
            override fun onNext(action: MyViewTest.ViewAction) {
                when (action) {
                    is MyViewTest.ViewAction.TextClick -> {
                        myView.updateState { currentState ->
                            currentState.copy(text = currentState.text + "1")
                        }
                    }
                }
            }
        })
        myView.updateState { currentState ->
            currentState.copy(
                text = "hello world!"
            )
        }
        Handler().postDelayed({
            myView.setVariation(MyViewTestVariation.TEST)
        }, 5000)
    }
}
