package io.gierla.rcannotationprocessors

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.gierla.rccore.annotations.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.annotations.State
import io.gierla.rccore.annotations.Structure
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.views.store.DefaultReactiveView
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Variation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.annotation.processing.*
import javax.lang.model.SourceVersion
import javax.lang.model.element.ElementKind
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.util.ElementFilter
import javax.tools.Diagnostic


@ExperimentalCoroutinesApi
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

        val viewStateHandlerName = viewName + "StateHandler"
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
                        createStateClasses(packageName, mStateElement, mStructureElement, viewStateDispatcherName, viewStateHandlerName)
                        createViewClasses(packageName, viewName, stateElement, structureElement, mActionElement, viewStateDispatcherName, viewStateHandlerName)
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

    private fun createViewClasses(packageName: String, viewName: String, stateElement: TypeElement, structureElement: TypeElement, actionElement: TypeElement, viewStateDispatcherName: String, viewStateHandlerName: String) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())
        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewActionType = ClassName(packageName, actionElement.qualifiedName.toString())

        val ViewStateHandlerType = ClassName(packageName, viewStateHandlerName)
        val ViewStateDisptacherType = ClassName(packageName, viewStateDispatcherName)

        val DefaultReactiveViewType = DefaultReactiveView::class.asTypeName()

        val viewTypeSpecBuilder = TypeSpec.classBuilder(viewName + "Impl")

        viewTypeSpecBuilder.superclass(DefaultReactiveViewType.parameterizedBy(ViewStateType, ViewActionType, ViewStructureType, ViewStateHandlerType))
        viewTypeSpecBuilder.addSuperclassConstructorParameter(CodeBlock.of("initialState"))

        val constructorBuilder = FunSpec.constructorBuilder()
        constructorBuilder.addParameter(
            ParameterSpec.builder("initialState", ViewStateType).build()
        )
        viewTypeSpecBuilder.primaryConstructor(constructorBuilder.build())

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
                .addParameter("variation", Variation::class.asTypeName().parameterizedBy(ViewStructureType, ViewStateHandlerType))
                .addParameter(ParameterSpec.builder("callback", VariationCallbackType.copy(nullable = true)).build())
                .addStatement("getViewStructure()?.let { variation.init(it) }")
                .addStatement("setStateDispatcher(%T(variation.getStateHandler(), callback))", ViewStateDisptacherType)
                .build()
        )

        //viewTypeSpecBuilder.addAnnotation(AnnotationSpec.builder(ExperimentalCoroutinesApi::class.asClassName()).build())

        val viewFile = FileSpec.builder(packageName, viewName + "Impl").addType(viewTypeSpecBuilder.build()).build()

        filer?.let { viewFile.writeTo(it) }

    }

    private fun createStateClasses(packageName: String, stateElement: TypeElement, structureElement: TypeElement, viewStateDispatcherName: String, viewStateHandlerName: String) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())

        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewStateHandlerType = ClassName(packageName, viewStateHandlerName)

        val StateDispatcherType = StateDispatcher::class.asTypeName()

        val DispatchCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", ViewStructureType),
                ParameterSpec("oldState", ViewStateType.copy(nullable = true)),
                ParameterSpec("newState", ViewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )

        val viewStateHandlerTypeSpecBuilder = TypeSpec.interfaceBuilder(viewStateHandlerName)
            .addSuperinterface(StateHandler::class.asTypeName())

        val viewStateDisptacherTypeSpecBuilder = TypeSpec.classBuilder(viewStateDispatcherName)
        viewStateDisptacherTypeSpecBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("stateHandler", ViewStateHandlerType.copy(nullable = true))
                .addParameter("dispachtedCallback", DispatchCallbackType.copy(nullable = true))
                .build()
        )
        viewStateDisptacherTypeSpecBuilder.addSuperinterface(StateDispatcherType.parameterizedBy(ViewStateType, ViewStructureType))
        viewStateDisptacherTypeSpecBuilder.addProperty(
            PropertySpec.builder("stateHandler", ViewStateHandlerType.copy(nullable = true))
                .initializer("stateHandler")
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

                viewStateHandlerTypeSpecBuilder.addFunction(
                    FunSpec.builder("draw${variableType.toString().capitalize()}")
                        .addParameter("view", ViewStructureType)
                        .addParameter("state", ViewStateType)
                        .build()
                )

                viewStateDispatcherFunSpecBuilder
                    .addStatement("if (oldState == null || oldState.%N != newState.%N) changes.add { stateHandler?.%N(view, newState) }", variableType.toString(), variableType.toString(), "draw${variableType.toString().capitalize()}")

            }

        viewStateDispatcherFunSpecBuilder
            .addStatement("dispachtedCallback?.invoke(view, oldState, newState)")

        viewStateDispatcherFunSpecBuilder
            .addStatement("return changes")

        val viewStateDispatcherFunSpec = viewStateDispatcherFunSpecBuilder.build()
        viewStateDisptacherTypeSpecBuilder.addFunction(viewStateDispatcherFunSpec)

        val viewStateHandlerFile = FileSpec.builder(packageName, viewStateHandlerName).addType(viewStateHandlerTypeSpecBuilder.build()).build()
        val viewStateDispatcherFile = FileSpec.builder(packageName, viewStateDispatcherName).addType(viewStateDisptacherTypeSpecBuilder.build()).build()

        filer?.let { viewStateHandlerFile.writeTo(it) }
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