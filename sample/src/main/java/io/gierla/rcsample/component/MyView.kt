package io.gierla.rcsample.component

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.RecyclerView
import io.gierla.rccore.action.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.state.StateDiffPair
import io.gierla.rccore.view.Structure
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers

@ReactiveComponent(viewType = RecyclerView::class)
class MyView @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : MyViewImpl(context, attributeSet, defStyleAttr) {

    override fun getViewStructure(): ViewStructure = object : ViewStructure {

    }

    override fun configureSubscriber(config: Observable<StateDiffPair<ViewState>>): Observable<StateDiffPair<ViewState>> {
        return config.observeOn(Schedulers.computation()).subscribeOn(AndroidSchedulers.mainThread())
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ): io.gierla.rccore.state.State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction: Action {

    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {

    }

}