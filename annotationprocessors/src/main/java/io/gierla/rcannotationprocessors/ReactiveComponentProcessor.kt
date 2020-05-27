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
import io.gierla.rccore.view.Variation
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


@AutoService(Processor::class)
@SupportedOptions(ReactiveComponentProcessor.KAPT_KOTLIN_GENERATED_OPTION_NAME)
class ReactiveComponentProcessor : AbstractProcessor() {

    companion object {
        const val KAPT_KOTLIN_GENERATED_OPTION_NAME = "kapt.kotlin.generated"
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

        val viewName = element.simpleName.toString()

        val viewStateDrawerName = viewName + "StateDrawer"
        val viewStateDispatcherName = viewName + "StateDispatcher"

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
                        createStateClasses(packageName, mStateElement, mStructureElement, viewStateDispatcherName, viewStateDrawerName)
                        createViewClasses(packageName, viewName, stateElement, structureElement, mActionElement, viewStateDispatcherName, viewStateDrawerName)
                    }
                }
            }

        } else {

            messager?.printMessage(
                Diagnostic.Kind.ERROR,
                "ReactiveComponent classes requires exactly one class annotaed with State, exactly one class annotated with Action and exactly one Interface annotated with Structure! Howerver if you don't need them they can be empty."
            )

        }

    }

    private fun createViewClasses(packageName: String, viewName: String, stateElement: TypeElement, structureElement: TypeElement, actionElement: TypeElement, viewStateDispatcherName: String, viewStateDrawerName: String) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())
        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewActionType = ClassName(packageName, actionElement.qualifiedName.toString())

        val ViewStateDrawerType = ClassName(packageName, viewStateDrawerName)
        val ViewStateDisptacherType = ClassName(packageName, viewStateDispatcherName)

        val DefaultReactiveViewType = ClassName("io.gierla.rcviews.store", "DefaultReactiveView")

        val viewTypeSpecBuilder = TypeSpec.classBuilder(viewName + "Impl")

        val LifecycleObserverType = ClassName("androidx.lifecycle", "LifecycleObserver")

        viewTypeSpecBuilder.superclass(
            DefaultReactiveViewType.parameterizedBy(
                ViewStateType,
                ViewActionType,
                ViewStructureType,
                ViewStateDrawerType
            )
        )
        viewTypeSpecBuilder.addSuperinterface(LifecycleObserverType)

        val LifecycleType = ClassName("androidx.lifecycle", "Lifecycle")

        val constructorBuilder = FunSpec.constructorBuilder()
        constructorBuilder.addParameter(
            ParameterSpec.builder("lifecycle", LifecycleType).build()
        )

        constructorBuilder.callSuperConstructor("lifecycle", ViewStateType.simpleName + "()")

        viewTypeSpecBuilder.addFunction(constructorBuilder.build())

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
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("variation", Variation::class.asTypeName().parameterizedBy(ViewStructureType, ViewStateDrawerType))
                .addParameter(ParameterSpec.builder("callback", VariationCallbackType.copy(nullable = true)).build())
                .addStatement("viewStructure?.let { variation.init(it) }")
                .addStatement("setStateDispatcher(%T(variation.getStateDrawer(), callback))", ViewStateDisptacherType)
                .build()
        )

        val viewFile = FileSpec.builder(packageName, viewName + "Impl").addType(viewTypeSpecBuilder.build()).build()

        filer?.let { viewFile.writeTo(it) }

    }

    private fun createStateClasses(packageName: String, stateElement: TypeElement, structureElement: TypeElement, viewStateDispatcherName: String, viewStateDrawerName: String) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())

        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewStateDrawerType = ClassName(packageName, viewStateDrawerName)

        val StateDispatcherType = StateDispatcher::class.asTypeName()

        val DispatchCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", ViewStructureType),
                ParameterSpec("oldState", ViewStateType.copy(nullable = true)),
                ParameterSpec("newState", ViewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )

        val viewStateDrawerTypeSpecBuilder = TypeSpec.interfaceBuilder(viewStateDrawerName)
            .addSuperinterface(StateDrawer::class.asTypeName())

        val viewStateDisptacherTypeSpecBuilder = TypeSpec.classBuilder(viewStateDispatcherName)
        viewStateDisptacherTypeSpecBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("stateDrawer", ViewStateDrawerType.copy(nullable = true))
                .addParameter("dispachtedCallback", DispatchCallbackType.copy(nullable = true))
                .build()
        )
        viewStateDisptacherTypeSpecBuilder.addSuperinterface(StateDispatcherType.parameterizedBy(ViewStateType, ViewStructureType))
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

        val CallbackListType = List::class.asTypeName()
            .parameterizedBy(
                LambdaTypeName.get(
                    parameters = listOf(),
                    returnType = Unit::class.asTypeName()
                )
            )

        val viewStateDispatcherFunSpecBuilder = FunSpec.builder("calculateChanges")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.SUSPEND)
            .addParameter("view", ViewStructureType)
            .addParameter("oldState", ViewStateType.copy(nullable = true))
            .addParameter("newState", ViewStateType)
            .returns(CallbackListType)

        viewStateDispatcherFunSpecBuilder
            .addStatement("val changes = mutableListOf<() -> Unit>()")

        stateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                viewStateDrawerTypeSpecBuilder.addFunction(
                    FunSpec.builder("draw${variableType.toString().capitalize()}")
                        .addParameter("view", ViewStructureType)
                        .addParameter("state", ViewStateType)
                        .build()
                )

                viewStateDispatcherFunSpecBuilder
                    .addStatement("if (oldState == null || oldState.%N != newState.%N) changes.add { stateDrawer?.%N(view, newState) }", variableType.toString(), variableType.toString(), "draw${variableType.toString().capitalize()}")

            }

        viewStateDispatcherFunSpecBuilder
            .addStatement("dispachtedCallback?.invoke(view, oldState, newState)")

        viewStateDispatcherFunSpecBuilder
            .addStatement("return changes")

        val viewStateDispatcherFunSpec = viewStateDispatcherFunSpecBuilder.build()
        viewStateDisptacherTypeSpecBuilder.addFunction(viewStateDispatcherFunSpec)

        val viewStateDrawerFile = FileSpec.builder(packageName, viewStateDrawerName).addType(viewStateDrawerTypeSpecBuilder.build()).build()
        val viewStateDispatcherFile = FileSpec.builder(packageName, viewStateDispatcherName).addType(viewStateDisptacherTypeSpecBuilder.build()).build()

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
