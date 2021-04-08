# ReactiveComponents

### Why?

When developing android ui's we use a lot of different predefined android views like `ConstraintLayout` and `RelativeLayout`. Just putting these predefined views into a single layout file makes them unreasonably long and hard to manage. Thats why it is a known best practice to split up our layout into smaller chunks (Components). This is achived by using so called compound views, which extend some kind of `ViewGroup` (for example RelativLayout, Constraintlayout, ...) to create one of these "Components". 

The advantage/problem with this approche is that every "Component" is an atomic unit which should be reusable and easy to extend. Because of this "Components" have to manage thier own state and expose actions/event (like click events) to thier "parent". 

If we take a closer look at the default android system components, they follow exactly these rules. For example a `TextView` has it's own state (the text we set) and exposes actions/events like the `setOnClickListener`. We can get and set this state and it will atomatically be rendered and we also can listen to the events of the view.

**ReactiveComponents is a minimalistic library which aims to provide the same functionality to manage state inside compound views in a uniform way with minimal developer work.**

### Usage

Bevor we start with the step by step guide: The respoitory contains a sample application which you can take as reference.

#### Dependecies

At first at your project level gradle file you have to use the jCenter repository:

```groovy
allprojects {
    repositories {
        ...
        jcenter()
        ...
    }
}
```

Then you have to import the libary and it's annotations processor (don' forget to apply the kapt plugin):

```groovy
implementation("io.gierla.reactivecomponents:core::{version}")
kapt("io.gierla.reactivecomponents:annotations:{version}")
```

You can find the newest version in maven central: https://search.maven.org/search?q=io.gierla.ReactiveComponents

#### Code

Here is a little step by step guid how a normal compound view transforms into a ReactiveComponent:

We start with a simple compound view in Kotlin:

```kotlin
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    init {
        inflate(context, R.layout.my_view, this)
    }

}
```

As we said earlier our compound views has a state, some actions and has an underlying structure where our state is going to be rendered. In this step we define them:

```kotlin
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    init {
        inflate(context, R.layout.my_view, this)
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

As you see we just added the three things I mentioned above and that every default android system component has. Because we want to do as little work as possible we are going to add an annotation to our view. This annotation will trigger the annotation processor and create some classes that will come in handy for us.

```kotlin
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr) {

    init {
        inflate(context, R.layout.my_view, this)
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

Now we want to add some functionality to our view. We want to be able to set the state, get the state, render the state and listen to events/actions. For this we extend the functionality of our view by delegating it to a class:

```kotlin
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure> by DefaultReactiveView(MyViewTest.ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

Somehow our class also needs to know how our "Component" looks and is structured, therefore we are going to set the structure inside our init block and from there on we can use the structure to define listeners and so on (we use setViewStructure and requireViewStructure to work with the structure of our "Component"):

```kotlin
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure> by DefaultReactiveView(MyViewTest.ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        requireViewStructure().run {
            testView.setOnClickListener {
                dispatchAction(ViewAction.TextClick(getState().text))
            }
            testView.isAllCaps = true
        }
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

Finally we have to manage the lifecycle of our "Component". We are going to add the `onDetachedFromWindow` and `onAttachedToWindow` functions. There are multiple reasons for this but for example we don't want to be notified about actions/events when the view is already gone (This step can be simplified quite a bit as you will see in the "Helpful" section further down):

```kotlin
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure> by DefaultReactiveView(MyViewTest.ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        requireViewStructure().run {
            testView.setOnClickListener {
                dispatchAction(ViewAction.TextClick(getState().text))
            }
            testView.isAllCaps = true
        }
    }

    override fun onDetachedFromWindow() {
        detachView()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachView()
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

Thats basically it, we just created a Reactive Component which manages state and actions for us. There is a single step missing, we have to tell our "Component" how to render the state, this is done by the `setVariation` function:

```kotlin
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(context: Context, attributeSet: AttributeSet? = null, defStyleAttr: Int = 0) : ConstraintLayout(context, attributeSet, defStyleAttr),
    ReactiveView<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure> by DefaultReactiveView(MyViewTest.ViewState()) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        requireViewStructure().run {
            testView.setOnClickListener {
                dispatchAction(ViewAction.TextClick(getState().text))
            }
            testView.isAllCaps = true
        }
      
      	setVariation(
            variation = myViewTestVariation { 
                init { 
                    it.testView.setTextColor(Color.BLACK)
                }
                stateHandler { 
                    drawText { view, state -> 
                        view.testView.text = state.text
                    }
                }
            }
        )
    }

    override fun onDetachedFromWindow() {
        detachView()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachView()
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```

The `setVariation` function is also avaiable from outside the "Component". This means that if we want to use the same "Component" twice (we want to achive reusable "Component") we just have to define two different variants and apply them accordingly to the use case. We could for example use a "ProfileImageComponent" in two different settings where at one place it is rendered as a squere und at another place it is rendered as a circle.

Inside the generated `MyViewTestStateHandler` is a function for every variable inside your state and every function will only be called if this specific variable has changed through a state change. This means that if you have a variable inside your state and want to render it when the state changes you just override the corresponding function inside `MyViewTestStateHandler` .

Now the last question: How do we listen to actions/events and set a new state? There are two simple functions we just have to call on our view to achive this:

```kotlin
val myView = findViewById<MyViewTest>(R.id.my_view)
myView.setActionListener {
            when (it) {
                is MyViewTest.ViewAction.TextClick -> {
                    Toast.makeText(applicationContext, it.text, Toast.LENGTH_LONG).show()
                }
            }
        }
```

That's it and now our "Component" is going to be updated atomatically and we will always now about actions/events that happen inside the "Component".



### Helpful

We often find ourself using the same `ViewGroup` for our compound views (for example ConstraintLayout). Because this happens very often we can create a `ReactiveConstraintLayout` superclass and eliminate some boilerplate code from our views. I plan on creating an library which is going to contain these superclasses in the future.

This is how we would create `ReactiveConstraintLayout`:

```kotlin
@ExperimentalCoroutinesApi
abstract class ReactiveConstraintLayout<S : State, A : Action, V : Structure> @JvmOverloads constructor(
    initialState: S,
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(
    context,
    attributeSet,
    defStyleAttr
), ReactiveView<S, A, V> by DefaultReactiveView<S, A, V>(initialState) {

    override fun onDetachedFromWindow() {
        detachView()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachView()
    }

}
```

And this is how we would use it, as you can see it eliminates some boilerplate code:

```kotlin
@ExperimentalCoroutinesApi
@ReactiveComponent
class MyViewTest @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ReactiveConstraintLayout<MyViewTest.ViewState, MyViewTest.ViewAction, MyViewTest.ViewStructure>(
    ViewState(),
    context,
    attributeSet,
    defStyleAttr
) {

    init {
        inflate(context, R.layout.my_view, this)

        setViewStructure {
            object : ViewStructure {
                override val testView by lazy { findViewById<TextView>(R.id.text_view) }
            }
        }

        setVariation(
            variation = myViewTestVariation {
                init {
                    it.testView.setTextColor(Color.BLACK)
                }
                stateHandler {
                    drawText { view, state ->
                        view.testView.text = state.text
                    }
                }
            }
        )

        requireViewStructure().run {
            testView.setOnClickListener {
                dispatchAction(ViewAction.TextClick(getState().text))
            }
            testView.isAllCaps = true
        }
    }

    @io.gierla.rccore.annotations.State
    data class ViewState(
        val text: String = ""
    ) : State

    @io.gierla.rccore.annotations.Action
    sealed class ViewAction : Action {
        class TextClick(val text: String) : ViewAction()
    }

    @io.gierla.rccore.annotations.Structure
    interface ViewStructure : Structure {
        val testView: TextView
    }

}
```



### Information

This library is an very early stage but I am going to try to realese a stable version as fast as I can. I know that Jetpack Compose just started to be a thing but I am pretty sure until we can take full advantage of it, it is going to be some time and until than this library can greatly increase productivity and stability. 

While developing this library I tried to reduce the work the developer has to do as much as I can, I still have a single idea how it could be made even more developer friendly but at first I want to look if this library is even wanted. (using bytecode transformation and an ide plugin)



### About me

I am a currently attending university of technology of vienna and I am quite new to library development and publishing. I would like to learn from you and if you have anything interesing to speak about, I would like to hear from you. 
