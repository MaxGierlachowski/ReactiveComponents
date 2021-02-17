package io.gierla.rcsample

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import io.gierla.rcsample.component.MyViewTest
import io.gierla.rcsample.component.MyViewTestVariations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@ExperimentalCoroutinesApi
class Main : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val myView = findViewById<MyViewTest>(R.id.my_view)
        myView.setVariation(variation = MyViewTestVariations.MAIN)
        myView.setActionListener(this::handleAction)
        myView.updateState { currentState ->
            currentState.copy(
                text = "hello world!"
            )
        }

        GlobalScope.launch {
            delay(5000)
            for(i in 0..1000) {
                delay(1)
                myView.updateState { currentState ->
                    currentState.copy(
                        text = "Text $i"
                    )
                }
            }
        }
    }

    private fun handleAction(action: MyViewTest.ViewAction) {
        when (action) {
            is MyViewTest.ViewAction.TextClick -> {
                Toast.makeText(applicationContext, action.text, Toast.LENGTH_LONG).show()
            }
        }
    }
}
