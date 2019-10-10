package io.gierla.rcannotationprocessors

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.gierla.rcannotations.Action
import io.gierla.rcannotations.ReactiveComponent
import io.gierla.rcannotations.State
import io.gierla.rcannotations.Structure
import io.gierla.rccore.state.StateDispatcher
import io.gierla.rccore.state.StateDrawer
import io.gierla.rccore.state.StateSubscriber
import io.gierla.rccore.store.DefaultStore
import io.gierla.rccore.view.Variation
import io.reactivex.disposables.CompositeDisposable
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


@AutoService(Processor::class)
@SupportedOptions(ReactiveComponentProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ReactiveComponentProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
        private const val STATE_DRAWER_NAME = "ViewStateDrawer"
        private const val STATE_DISPATCHER_NAME = "ViewStateDispatcher"
        private const val VIEW_NAME = "ViewImpl"
    }

    private var messager: Messager? = null
    private var filer: Filer? = null

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv?.messager
        filer = processingEnv?.filer
    }

    override fun process(
        p0: MutableSet<out TypeElement>?,
        roundEnvironment: RoundEnvironment?
    ): Boolean {

        val annotatedElements = roundEnvironment?.getElementsAnnotatedWith(ReactiveComponent::class.java) ?: mutableSetOf()
        val elementsWithMatchingType = listOf<TypeElement>(*ElementFilter.typesIn(annotatedElements).toTypedArray())

        elementsWithMatchingType
            .filter { isClass(it, "ReactiveComponent") && isPublic(it, "ReactiveComponent") && isNotAbstract(it, "ReactiveComponent") }
            .forEach {
                createComponentFile(it)
            }

        return true

    }

    private fun createComponentFile(element: TypeElement) {

        val packageName = processingEnv.elementUtils.getPackageOf(element).toString()

        var viewTypeMirror: TypeMirror? = null

        try {
            element.getAnnotation(ReactiveComponent::class.java).viewType
        } catch (e: MirroredTypeException) {
            viewTypeMirror = e.typeMirror
        }

        viewTypeMirror?.let { typeMirror ->

            val viewTypeClass = typeMirror

            val componentChildren = listOf<TypeElement>(*ElementFilter.typesIn(element.enclosedElements).toTypedArray())

            val stateChildren = componentChildren
                .filter { childElement -> childElement.getAnnotation(State::class.java) != null }
                .filter { childElement -> isClass(childElement, "State") && isPublic(childElement, "State") && isNotAbstract(childElement, "State") }

            val actionChildren = componentChildren
                .filter { childElement -> childElement.getAnnotation(Action::class.java) != null }
                .filter { childElement -> isClass(childElement, "Action") && isPublic(childElement, "Action") }

            val structureChildren = componentChildren
                .filter { childElement -> childElement.getAnnotation(Structure::class.java) != null }
                .filter { childElement -> isInterface(childElement, "Structure") && isPublic(childElement, "Structure") }

            var foundState = false
            var stateElement: TypeElement? = null

            var foundAction = false
            var actionElement: TypeElement? = null

            var foundStructure = false
            var structureElement: TypeElement? = null

            if (stateChildren.size == 1) {
                foundState = true
                stateElement = stateChildren.firstOrNull()
            }

            if (actionChildren.size == 1) {
                foundAction = true
                actionElement = actionChildren.firstOrNull()
            }

            if (structureChildren.size == 1) {
                foundStructure = true
                structureElement = structureChildren.firstOrNull()
            }

            if (foundAction && foundState && foundStructure) {

                structureElement?.let { mStructureElement ->
                    stateElement?.let { mStateElement ->
                        actionElement?.let { mActionElement ->
                            createStateClasses(packageName, mStateElement, mStructureElement)
                            createViewClasses(packageName, viewTypeClass.toString(), stateElement, structureElement, mActionElement)
                        }
                    }
                }

            } else {

                messager?.printMessage(
                    Diagnostic.Kind.ERROR,
                    "ReactiveComponent class requires exactly one class annotaed with State, exactly one class annotated with Action and exactly one Interface annotated with Structure! Howerver if you don't need them they can be empty."
                )

            }

        } ?: run {

            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "ReactiveComponent annotation requiers a viewType parameter!"
            )

        }

    }

    private fun createViewClasses(packageName: String, parentViewType: String, stateElement: TypeElement, structureElement: TypeElement, actionElement: TypeElement) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())
        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewActionType = ClassName(packageName, actionElement.qualifiedName.toString())

        val ViewStateDrawerType = ClassName(packageName, STATE_DRAWER_NAME)
        val ViewStateDisptacherType = ClassName(packageName, STATE_DISPATCHER_NAME)

        val ViewStoreType = DefaultStore::class.asTypeName()
        val DispatcherType = StateDispatcher::class.asTypeName()

        val ViewParentType = ClassName("", parentViewType)
        val ContextType = ClassName("android.content", "Context")
        val AttributeSetType = ClassName("android.util", "AttributeSet")

        val viewTypeSpecBuilder = TypeSpec.classBuilder(VIEW_NAME)
            .addModifiers(KModifier.ABSTRACT)

        viewTypeSpecBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addAnnotation(JvmOverloads::class)
                .addParameter("context", ContextType)
                .addParameter(
                    ParameterSpec.builder("attrs", AttributeSetType.copy(nullable = true))
                        .defaultValue("null")
                        .build()
                )
                .addParameter(
                    ParameterSpec.builder("defStyleAttr", Int::class.asTypeName())
                        .defaultValue("0")
                        .build()
                )
                .build()
        )
        viewTypeSpecBuilder.superclass(ViewParentType)
        viewTypeSpecBuilder.addSuperclassConstructorParameter("context")
        viewTypeSpecBuilder.addSuperclassConstructorParameter("attrs")
        viewTypeSpecBuilder.addSuperclassConstructorParameter("defStyleAttr")

        viewTypeSpecBuilder.addFunction(
            FunSpec.builder("getViewStructure")
                .returns(ViewStructureType)
                .addModifiers(KModifier.ABSTRACT)
                .build()
        )

        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder(
                    "store", ViewStoreType.parameterizedBy(
                        ViewStateType,
                        ViewActionType
                    )
                )
                .initializer("%T<%T, %T>(initialState = %T())", ViewStoreType, ViewStateType, ViewActionType, ViewStateType)
                .build()
        )

        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder("disposable", CompositeDisposable::class.asTypeName())
                .initializer("%T()", CompositeDisposable::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder(
                    "dispatcher", DispatcherType.parameterizedBy(
                        ViewStructureType,
                        ViewStateType
                    )
                )
                .mutable(true)
                .initializer("%T(null, null)", ViewStateDisptacherType)
                .setter(
                    FunSpec.setterBuilder()
                        .addParameter(
                            "value", DispatcherType.parameterizedBy(
                                ViewStructureType,
                                ViewStateType
                            )
                        )
                        .addStatement("field = value")
                        .addStatement("field.dispatchUpdates(getViewStructure(), null, store.getState())")
                        .build()
                )
                .build()
        )

        val stateSubscriber = TypeSpec.anonymousClassBuilder()
            .addSuperinterface(StateSubscriber::class.asTypeName().parameterizedBy(ViewStateType))
            .addFunction(
                FunSpec.builder("onError")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("error", Throwable::class)
                    .build()
            )
            .addFunction(
                FunSpec.builder("onComplete")
                    .addModifiers(KModifier.OVERRIDE)
                    .build()
            )
            .addFunction(
                FunSpec.builder("onNext")
                    .addModifiers(KModifier.OVERRIDE)
                    .addParameter("oldState", ViewStateType)
                    .addParameter("newState", ViewStateType)
                    .addStatement("dispatcher.dispatchUpdates(getViewStructure(), oldState, newState)")
                    .build()
            )
            .build()

        viewTypeSpecBuilder.addFunction(
            FunSpec
                .builder("onAttachedToWindow")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("super.onAttachedToWindow()")
                .addStatement("disposable.add(%N.subscribeState(%L))", "store", stateSubscriber)
                .build()
        )

        viewTypeSpecBuilder.addFunction(
            FunSpec
                .builder("onDetachedFromWindow")
                .addModifiers(KModifier.OVERRIDE)
                .addStatement("disposable.clear()")
                .addStatement("super.onDetachedFromWindow()")
                .build()
        )

        val VariationCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", ViewStructureType),
                ParameterSpec("oldState", ViewStateType.copy(nullable = true)),
                ParameterSpec("newState", ViewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )


        viewTypeSpecBuilder.addFunction(
            FunSpec
                .builder("setVariation")
                .addParameter("variation", Variation::class.asTypeName().parameterizedBy(ViewStructureType, ViewStateDrawerType))
                .addParameter("callback", VariationCallbackType)
                .addStatement("variation.init(getViewStructure())")
                .addStatement("dispatcher = %T(variation.getStateDrawer(), callback)", ViewStateDisptacherType)
                .build()
        )

        val viewFile = FileSpec.builder(packageName, VIEW_NAME).addType(viewTypeSpecBuilder.build()).build()

        filer?.let { viewFile.writeTo(it) }

    }

    private fun createStateClasses(packageName: String, stateElement: TypeElement, structureElement: TypeElement) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())

        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewStateDrawerType = ClassName(packageName, STATE_DRAWER_NAME)

        val StateDispatcherType = StateDispatcher::class.asTypeName()

        val DispatchCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", ViewStructureType),
                ParameterSpec("oldState", ViewStateType.copy(nullable = true)),
                ParameterSpec("newState", ViewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )

        val viewStateDrawerTypeSpecBuilder = TypeSpec.interfaceBuilder(STATE_DRAWER_NAME)
            .addSuperinterface(StateDrawer::class.asTypeName())

        val viewStateDisptacherTypeSpecBuilder = TypeSpec.classBuilder(STATE_DISPATCHER_NAME)
        viewStateDisptacherTypeSpecBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("stateDrawer", ViewStateDrawerType.copy(nullable = true))
                .addParameter("dispachtedCallback", DispatchCallbackType.copy(nullable = true))
                .build()
        )
        viewStateDisptacherTypeSpecBuilder.addSuperinterface(StateDispatcherType.parameterizedBy(ViewStructureType, ViewStateType))
        viewStateDisptacherTypeSpecBuilder.addProperty(
            PropertySpec.builder("stateDrawer", ViewStateDrawerType.copy(nullable = true))
                .initializer("stateDrawer")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        viewStateDisptacherTypeSpecBuilder.addProperty(
            PropertySpec.builder("dispachtedCallback", DispatchCallbackType.copy(nullable = true))
                .initializer("dispachtedCallback")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        val viewStateDispatcherFunSpecBuilder = FunSpec.builder("dispatchUpdates")
            .addModifiers(KModifier.OVERRIDE)
            .addParameter("view", ViewStructureType)
            .addParameter("oldState", ViewStateType.copy(nullable = true))
            .addParameter("newState", ViewStateType)

        stateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                viewStateDrawerTypeSpecBuilder.addFunction(
                    FunSpec.builder("draw${variableType.toString().capitalize()}")
                        .addModifiers(KModifier.ABSTRACT)
                        .addParameter("view", ViewStructureType)
                        .addParameter("state", ViewStateType)
                        .build()
                )

                viewStateDispatcherFunSpecBuilder
                    .addStatement("if (oldState == null || oldState.%N != newState.%N) stateDrawer?.%N(view, newState)", variableType.toString(), variableType.toString(), "draw${variableType.toString().capitalize()}")

            }

        viewStateDispatcherFunSpecBuilder
            .addStatement("dispachtedCallback?.invoke(view, oldState, newState)")

        val viewStateDispatcherFunSpec = viewStateDispatcherFunSpecBuilder.build()
        viewStateDisptacherTypeSpecBuilder.addFunction(viewStateDispatcherFunSpec)

        val viewStateDrawerFile = FileSpec.builder(packageName, STATE_DRAWER_NAME).addType(viewStateDrawerTypeSpecBuilder.build()).build()
        val viewStateDispatcherFile = FileSpec.builder(packageName, STATE_DISPATCHER_NAME).addType(viewStateDisptacherTypeSpecBuilder.build()).build()

        filer?.let { viewStateDrawerFile.writeTo(it) }
        filer?.let { viewStateDispatcherFile.writeTo(it) }

    }

    private fun isNotAbstract(type: TypeElement, annotationName: String): Boolean {
        if (type.modifiers.contains(Modifier.ABSTRACT)) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "${type.qualifiedName}: only non abstract classes can be annotated with $annotationName"
            )
            return false
        }
        return true
    }

    private fun isClass(type: TypeElement, annotationName: String): Boolean {
        if (type.kind != ElementKind.CLASS) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "${type.qualifiedName}: only classes can be annotated with $annotationName"
            )
            return false
        }
        return true
    }

    private fun isInterface(type: TypeElement, annotationName: String): Boolean {
        if (type.kind != ElementKind.INTERFACE) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "${type.qualifiedName}: only interfaces can be annotated with $annotationName"
            )
            return false
        }
        return true
    }

    private fun isPublic(type: TypeElement, annotationName: String): Boolean {
        if (type.modifiers.contains(Modifier.PRIVATE)) {
            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "${type.qualifiedName}: only public classes can be annotated with $annotationName"
            )
            return false
        }
        return true
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> = mutableSetOf(ReactiveComponent::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

}
