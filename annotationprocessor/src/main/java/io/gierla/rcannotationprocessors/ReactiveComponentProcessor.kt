package io.gierla.rcannotationprocessors

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.gierla.rccore.annotations.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.annotations.State
import io.gierla.rccore.annotations.Structure
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.views.view.StateDispatcher
import io.gierla.rccore.views.view.Variation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.withContext
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

        private val ExperimentalCoroutinesApiAnnotation = ClassName("kotlinx.coroutines", "ExperimentalCoroutinesApi")
        private val StateDispatcherType = StateDispatcher::class.asTypeName()
        private val VariationType = Variation::class.asTypeName()
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

        val annotatedElements =
            roundEnvironment?.getElementsAnnotatedWith(ReactiveComponent::class.java)
                ?: mutableSetOf()
        val elementsWithMatchingType =
            listOf<TypeElement>(*ElementFilter.typesIn(annotatedElements).toTypedArray())

        elementsWithMatchingType
            .filter {
                isClass(it, "ReactiveComponent") && isPublic(
                    it,
                    "ReactiveComponent"
                ) && isNotAbstract(it, "ReactiveComponent")
            }
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

        val componentChildren =
            listOf<TypeElement>(*ElementFilter.typesIn(element.enclosedElements).toTypedArray())

        val stateChildren = componentChildren
            .filter { childElement -> childElement.getAnnotation(State::class.java) != null }
            .filter { childElement ->
                isClass(childElement, "State") && isPublic(
                    childElement,
                    "State"
                ) && isNotAbstract(childElement, "State")
            }

        val actionChildren = componentChildren
            .filter { childElement -> childElement.getAnnotation(Action::class.java) != null }
            .filter { childElement ->
                isClass(childElement, "Action") && isPublic(
                    childElement,
                    "Action"
                )
            }

        val structureChildren = componentChildren
            .filter { childElement -> childElement.getAnnotation(Structure::class.java) != null }
            .filter { childElement ->
                isInterface(childElement, "Structure") && isPublic(
                    childElement,
                    "Structure"
                )
            }

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
                        createStateClasses(
                            TargetInfo(
                                packageName,
                                viewName,
                                mStateElement,
                                mActionElement,
                                mStructureElement,
                            ),
                            viewStateDispatcherName,
                            viewStateHandlerName
                        )
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

    private fun createStateClasses(
        targetInfo: TargetInfo,
        viewStateDispatcherName: String,
        viewStateHandlerName: String
    ) {

        createStateHandler(targetInfo, viewStateHandlerName)

        val viewStateHandlerType = ClassName(targetInfo.packageName, viewStateHandlerName)

        createStateDispatcher(targetInfo, viewStateDispatcherName, viewStateHandlerType)

        val viewStateDispatcherType = ClassName(targetInfo.packageName, viewStateDispatcherName)

        createStateHandlerBuilder(targetInfo, viewStateHandlerName + "Builder", viewStateHandlerType)

        val stateHandlerBuilderType = ClassName(targetInfo.packageName, viewStateHandlerName + "Builder")

        createVariationBuilder(targetInfo, targetInfo.viewName + "VariationBuilder", viewStateHandlerType, viewStateDispatcherType, stateHandlerBuilderType)
    }

    private fun createVariationBuilder(target: TargetInfo, name: String, stateHandlerType: ClassName, stateDispatcherType: ClassName, stateHandlerBuilderType: ClassName) {
        val variationBuilderType = ClassName(target.packageName, name)

        val variationBuilderTypeSpecBuilder = TypeSpec.classBuilder(name)
            .addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)

        val stateHandlerBuilderCallbackType = LambdaTypeName.get(
            parameters = listOf(),
            returnType = Unit::class.asTypeName(),
            receiver = stateHandlerBuilderType
        )

        // Builder functions

        val initFuncType = LambdaTypeName.get(
            parameters = listOf(ParameterSpec("view", target.viewStructureType)),
            returnType = Unit::class.asTypeName()
        )
        val initFuncName = "initFunc"

        variationBuilderTypeSpecBuilder.addProperty(
            PropertySpec.builder(initFuncName, initFuncType)
                .addModifiers(KModifier.PRIVATE)
                .mutable()
                .initializer("{}")
                .build()
        )

        variationBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder("init")
                .addParameter(initFuncName, initFuncType)
                .addStatement("this.$initFuncName = $initFuncName")
                .build()
        )

        val dispatchFuncType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", target.viewStructureType),
                ParameterSpec("oldState", target.viewStateType.copy(nullable = true)),
                ParameterSpec("newState", target.viewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )
        val dispatchFuncName = "dispatchCallback"

        variationBuilderTypeSpecBuilder.addProperty(
            PropertySpec.builder(dispatchFuncName, dispatchFuncType)
                .addModifiers(KModifier.PRIVATE)
                .mutable()
                .initializer("{_, _, _ ->}")
                .build()
        )

        variationBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder(dispatchFuncName)
                .addParameter(dispatchFuncName, dispatchFuncType)
                .addStatement("this.$dispatchFuncName = $dispatchFuncName")
                .build()
        )

        val stateHandlerName = "stateHandler"

        variationBuilderTypeSpecBuilder.addProperty(
            PropertySpec.builder(stateHandlerName, stateHandlerType)
                .addModifiers(KModifier.PRIVATE, KModifier.LATEINIT)
                .mutable()
                .build()
        )

        variationBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder(stateHandlerName)
                .addParameter("initializer", stateHandlerBuilderCallbackType)
                .addStatement("this.$stateHandlerName = %T().apply(initializer).build()", stateHandlerBuilderType)
                .build()
        )

        // Build function

        val variationBuilderObj = TypeSpec.anonymousClassBuilder()
            .addSuperinterface(VariationType.parameterizedBy(target.viewStructureType, target.viewStateType))

        variationBuilderObj.addFunction(
            FunSpec.builder("init")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter("view", target.viewStructureType)
                .addStatement("initFunc(view)")
                .build()
        )

        variationBuilderObj.addFunction(
            FunSpec.builder("getStateDispatcher")
                .addModifiers(KModifier.OVERRIDE)
                .returns(StateDispatcherType.parameterizedBy(target.viewStateType, target.viewStructureType))
                .addStatement("return %T(stateHandler, dispatchCallback)", stateDispatcherType)
                .build()
        )

        variationBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder("build")
                .returns(VariationType.parameterizedBy(target.viewStructureType, target.viewStateType))
                .addStatement("return %L", variationBuilderObj.build())
                .build()
        )

        val variationBuilderCallbackType = LambdaTypeName.get(
            parameters = listOf(),
            returnType = Unit::class.asTypeName(),
            receiver = variationBuilderType
        )

        val builderFunction = FunSpec.builder(target.viewName.decapitalize() + "Variation")
            .addAnnotation(ExperimentalCoroutinesApiAnnotation)
            .addParameter("initializer", variationBuilderCallbackType)
            .returns(VariationType.parameterizedBy(target.viewStructureType, target.viewStateType))
            .addStatement("return %T().apply(initializer).build()", variationBuilderType)
            .build()

        val variationBuilderFile = FileSpec.builder(target.packageName, name).addType(variationBuilderTypeSpecBuilder.build()).addFunction(builderFunction).build()
        filer?.let { variationBuilderFile.writeTo(it) }
    }

    private fun createStateHandlerBuilder(target: TargetInfo, name: String, stateHandlerType: ClassName) {

        val stateHandlerBuilderType = ClassName(target.packageName, name)

        val viewStateHandlerBuilderTypeSpecBuilder = TypeSpec.classBuilder(name)
            .addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)

        val viewStateHandlerBuilderObj = TypeSpec.anonymousClassBuilder().addSuperinterface(stateHandlerType)

        target.viewStateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                val actionName = "draw${variableType.toString().capitalize()}"

                val localFuncType = LambdaTypeName.get(
                    parameters = listOf(
                        ParameterSpec("view", target.viewStructureType),
                        ParameterSpec("state", target.viewStateType)
                    ),
                    returnType = Unit::class.asTypeName()
                )

                val localFuncName = actionName + "Func"

                viewStateHandlerBuilderTypeSpecBuilder.addProperty(
                    PropertySpec.builder(localFuncName, localFuncType)
                        .addModifiers(KModifier.PRIVATE)
                        .mutable()
                        .initializer("{_, _ ->}")
                        .build()
                )

                viewStateHandlerBuilderTypeSpecBuilder.addFunction(
                    FunSpec.builder(actionName)
                        .addParameter(localFuncName, localFuncType)
                        .addStatement("this.$localFuncName = $localFuncName")
                        .build()
                )

                viewStateHandlerBuilderObj.addFunction(
                    FunSpec.builder(actionName)
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("view", target.viewStructureType)
                        .addParameter("state", target.viewStateType)
                        .addStatement("$localFuncName(view, state)")
                        .build()
                )

            }

        viewStateHandlerBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder("build")
                .returns(stateHandlerType)
                .addStatement("return %L", viewStateHandlerBuilderObj.build())
                .build()
        )

        val stateHandlerBuilderCallbackType = LambdaTypeName.get(
            parameters = listOf(),
            returnType = Unit::class.asTypeName(),
            receiver = stateHandlerBuilderType
        )

        val builderFunction = FunSpec.builder(stateHandlerType.simpleName.decapitalize())
            .addAnnotation(ExperimentalCoroutinesApiAnnotation)
            .addParameter(ParameterSpec.builder("initializer", stateHandlerBuilderCallbackType).build())
            .returns(stateHandlerType)
            .addStatement("return %T().apply(initializer).build()", stateHandlerBuilderType)
            .build()

        val viewStateHandlerBuilderFile = FileSpec.builder(target.packageName, name).addType(viewStateHandlerBuilderTypeSpecBuilder.build()).addFunction(builderFunction).build()
        filer?.let { viewStateHandlerBuilderFile.writeTo(it) }
    }

    private fun createStateDispatcher(target: TargetInfo, name: String, stateHandlerType: ClassName) {
        val dispatchCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", target.viewStructureType),
                ParameterSpec("oldState", target.viewStateType.copy(nullable = true)),
                ParameterSpec("newState", target.viewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )

        val dispatchersType = Dispatchers::class.asTypeName()

        val viewStateDispatcherFunSpecBuilder = FunSpec.builder("dispatchChanges")
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.SUSPEND)
            .addParameter("view", target.viewStructureType)
            .addParameter("state", target.viewStateType)

        target.viewStateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                val actionName = "draw${variableType.toString().capitalize()}"
                viewStateDispatcherFunSpecBuilder
                    .beginControlFlow(
                        "if (oldState == null || oldState?.%N != state.%N)",
                        variableType.toString(),
                        variableType.toString()
                    )
                    .beginControlFlow(
                        "withContext(%T.Main) {",
                        dispatchersType
                    )
                    .addStatement(
                        "stateHandler?.%N(view, state)",
                        actionName
                    )
                    .endControlFlow()
                    .endControlFlow()
            }

        viewStateDispatcherFunSpecBuilder
            .addStatement("dispatchedCallback?.invoke(view, oldState, state)")
            .addStatement("oldState = state")

        val viewStateDispatcherTypeSpecBuilder = TypeSpec.classBuilder(name)
            .primaryConstructor(
                FunSpec.constructorBuilder()
                    .addParameter("stateHandler", stateHandlerType.copy(nullable = true))
                    .addParameter(ParameterSpec.builder("dispatchedCallback", dispatchCallbackType.copy(nullable = true)).defaultValue("null").build())
                    .build()
            )
            .superclass(
                StateDispatcherType.parameterizedBy(
                    target.viewStateType,
                    target.viewStructureType
                )
            )
            .addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)
            .addProperty(
                PropertySpec.builder("stateHandler", stateHandlerType.copy(nullable = true))
                    .initializer("stateHandler")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addProperty(
                PropertySpec.builder("dispatchedCallback", dispatchCallbackType.copy(nullable = true))
                    .initializer("dispatchedCallback")
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )
            .addFunction(viewStateDispatcherFunSpecBuilder.build())

        val viewStateDispatcherFile = FileSpec.builder(target.packageName, name).addType(viewStateDispatcherTypeSpecBuilder.build()).addImport("kotlinx.coroutines", "withContext").build()
        filer?.let { viewStateDispatcherFile.writeTo(it) }
    }

    private fun createStateHandler(target: TargetInfo, name: String) {
        val viewStateHandlerTypeSpecBuilder = TypeSpec.interfaceBuilder(name)
            .addSuperinterface(StateHandler::class.asTypeName())
            .addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)

        target.viewStateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                val actionName = "draw${variableType.toString().capitalize()}"

                viewStateHandlerTypeSpecBuilder.addFunction(
                    FunSpec.builder(actionName)
                        .addParameter("view", target.viewStructureType)
                        .addParameter("state", target.viewStateType)
                        .build()
                )

            }

        val viewStateHandlerFile = FileSpec.builder(target.packageName, name).addType(viewStateHandlerTypeSpecBuilder.build()).build()
        filer?.let { viewStateHandlerFile.writeTo(it) }
    }

    private data class TargetInfo(
        val packageName: String,
        val viewName: String,

        val viewStateElement: TypeElement,
        val viewActionElement: TypeElement,
        val viewStructureElement: TypeElement
    ) {
        val viewStateType = ClassName(packageName, viewStateElement.qualifiedName.toString())
        val viewActionType = ClassName(packageName, viewActionElement.qualifiedName.toString())
        val viewStructureType = ClassName(packageName, viewStructureElement.qualifiedName.toString())
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

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(ReactiveComponent::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

}
