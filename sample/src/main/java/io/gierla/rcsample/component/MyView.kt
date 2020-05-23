package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import io.gierla.rccore.action.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.view.Structure
import io.gierla.rcsample.R
import io.gierla.rcsample.databinding.MyViewBinding
import io.gierla.rcsample.helper.viewbinding.ViewBindingHolder
import io.gierla.rcsample.helper.viewbinding.ViewBindingHolderImpl
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

@ReactiveComponent(viewType = ConstraintLayout::class)
class MyView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : MyViewImpl(context, attributeSet, defStyleAttr), ViewBindingHolder<MyViewBinding> by ViewBindingHolderImpl() {

    init {
        LayoutInflater.from(context).inflate(R.layout.my_view, this, true)
        initBinding(MyViewBinding.bind(this)) {
            textView.setOnClickListener {
                store.dispatch(ViewAction.TextClick(store.getState().text))
            }
        }
    }

    private val viewStructure: ViewStructure = object : ViewStructure {
        override val testView by lazy { requireBinding().textView }
    }

    override fun getViewStructure(): ViewStructure = viewStructure;

    override fun configureSubscriber(config: Observable<StateDiffPair<ViewState>>): Observable<StateDiffPair<ViewState>> {
        return config.observeOn(Schedulers.computation()).subscribeOn(AndroidSchedulers.mainThread())
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : io.gierla.rccore.state.State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}