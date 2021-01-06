package io.gierla.rcannotationprocessors

import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.gierla.rccore.annotations.Action
import io.gierla.rccore.annotations.ReactiveComponent
import io.gierla.rccore.annotations.State
import io.gierla.rccore.annotations.Structure
import io.gierla.rccore.main.state.StateHandler
import io.gierla.rccore.views.helper.VariationBuilder
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
                            packageName,
                            mStateElement,
                            mStructureElement,
                            viewStateDispatcherName,
                            viewStateHandlerName
                        )
                        createViewClasses(
                            packageName,
                            viewName,
                            stateElement,
                            structureElement,
                            mActionElement,
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

    private val ExperimentalCoroutinesApiAnnotation =
        ClassName("kotlinx.coroutines", "ExperimentalCoroutinesApi")

    private fun createViewClasses(
        packageName: String,
        viewName: String,
        stateElement: TypeElement,
        structureElement: TypeElement,
        actionElement: TypeElement,
        viewStateDispatcherName: String,
        viewStateHandlerName: String
    ) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())
        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewActionType = ClassName(packageName, actionElement.qualifiedName.toString())

        val ViewStateHandlerType = ClassName(packageName, viewStateHandlerName).parameterizedBy(
            ViewStructureType,
            ViewStateType
        )
        val ViewStateDisptacherType = ClassName(packageName, viewStateDispatcherName)

        val DefaultReactiveViewType = DefaultReactiveView::class.asTypeName()

        val viewTypeSpecBuilder = TypeSpec.classBuilder(viewName + "Impl")
        viewTypeSpecBuilder.addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)

        viewTypeSpecBuilder.superclass(
            DefaultReactiveViewType.parameterizedBy(
                ViewStateType,
                ViewActionType,
                ViewStructureType,
                ViewStateHandlerType
            )
        )
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

        val VariationBuilderCallbackType = LambdaTypeName.get(
            parameters = listOf(),
            returnType = Unit::class.asTypeName(),
            receiver = VariationBuilder::class.asTypeName()
                .parameterizedBy(ViewStructureType, ViewStateHandlerType)
        )

        viewTypeSpecBuilder.addFunction(
            FunSpec
                .builder("setVariation")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    ParameterSpec.builder(
                        "callback",
                        VariationCallbackType.copy(nullable = true)
                    ).build()
                )
                .addParameter(
                    "variation",
                    Variation::class.asTypeName()
                        .parameterizedBy(ViewStructureType, ViewStateHandlerType)
                )
                .addStatement("getViewStructure()?.let { variation.init(it) }")
                .addStatement(
                    "setStateDispatcher(%T(variation.getStateHandler(), callback))",
                    ViewStateDisptacherType
                )
                .build()
        )

        viewTypeSpecBuilder.addFunction(
            FunSpec
                .builder("setVariation")
                .addModifiers(KModifier.OVERRIDE)
                .addParameter(
                    ParameterSpec.builder(
                        "callback",
                        VariationCallbackType.copy(nullable = true)
                    ).build()
                )
                .addParameter("variationBuilder", VariationBuilderCallbackType)
                .addStatement(
                    "val variation = %T().apply(variationBuilder).build()",
                    VariationBuilder::class.asTypeName()
                        .parameterizedBy(ViewStructureType, ViewStateHandlerType)
                )
                .addStatement("getViewStructure()?.let { variation.init(it) }")
                .addStatement(
                    "setStateDispatcher(%T(variation.getStateHandler(), callback))",
                    ViewStateDisptacherType
                )
                .build()
        )

        val viewFile =
            FileSpec.builder(packageName, viewName + "Impl").addType(viewTypeSpecBuilder.build())
                .build()

        filer?.let { viewFile.writeTo(it) }

    }

    private fun createStateClasses(
        packageName: String,
        stateElement: TypeElement,
        structureElement: TypeElement,
        viewStateDispatcherName: String,
        viewStateHandlerName: String
    ) {

        val ViewStructureType = ClassName(packageName, structureElement.qualifiedName.toString())

        val ViewStateType = ClassName(packageName, stateElement.qualifiedName.toString())
        val ViewStateHandlerType = ClassName(packageName, viewStateHandlerName).parameterizedBy(
            ViewStructureType,
            ViewStateType
        )

        val StateDispatcherType = StateDispatcher::class.asTypeName()

        val DispatchCallbackType = LambdaTypeName.get(
            parameters = listOf(
                ParameterSpec("view", ViewStructureType),
                ParameterSpec("oldState", ViewStateType.copy(nullable = true)),
                ParameterSpec("newState", ViewStateType)
            ),
            returnType = Unit::class.asTypeName()
        )

        val GenericViewStructureType =
            TypeVariableName.invoke("V", io.gierla.rccore.views.view.Structure::class.asTypeName())
        val GenerivViewStateType =
            TypeVariableName.invoke("S", io.gierla.rccore.main.state.State::class.asTypeName())

        val viewStateHandlerTypeSpecBuilder = TypeSpec.interfaceBuilder(viewStateHandlerName)
            .addSuperinterface(StateHandler::class.asTypeName())
            .addTypeVariable(GenericViewStructureType)
            .addTypeVariable(GenerivViewStateType)

        /* StateHandler Builder */

        val StateHandlerBuilderType = ClassName(packageName, viewStateHandlerName + "Builder")

        val viewStateHandlerBuilderTypeSpecBuilder = TypeSpec.classBuilder(viewStateHandlerName + "Builder")
        viewStateHandlerBuilderTypeSpecBuilder.addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)
        viewStateHandlerBuilderTypeSpecBuilder.addTypeVariable(GenericViewStructureType)
        viewStateHandlerBuilderTypeSpecBuilder.addTypeVariable(GenerivViewStateType)

        /* StateHandler Builder End */

        val viewStateDispatcherTypeSpecBuilder = TypeSpec.classBuilder(viewStateDispatcherName)
        viewStateDispatcherTypeSpecBuilder.primaryConstructor(
            FunSpec.constructorBuilder()
                .addParameter("stateHandler", ViewStateHandlerType.copy(nullable = true))
                .addParameter("dispachtedCallback", DispatchCallbackType.copy(nullable = true))
                .build()
        )
        viewStateDispatcherTypeSpecBuilder.addAnnotation(annotation = ExperimentalCoroutinesApiAnnotation)
        viewStateDispatcherTypeSpecBuilder.addSuperinterface(
            StateDispatcherType.parameterizedBy(
                ViewStateType,
                ViewStructureType
            )
        )
        viewStateDispatcherTypeSpecBuilder.addProperty(
            PropertySpec.builder("stateHandler", ViewStateHandlerType.copy(nullable = true))
                .initializer("stateHandler")
                .addModifiers(KModifier.PRIVATE)
                .build()
        )
        viewStateDispatcherTypeSpecBuilder.addProperty(
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

        val viewStateHandlerBuilderObj = TypeSpec.anonymousClassBuilder()
            .addSuperinterface(ClassName(packageName, viewStateHandlerName).parameterizedBy(GenericViewStructureType, GenerivViewStateType))

        stateElement.enclosedElements
            .filter { it.kind == ElementKind.FIELD }
            .forEach { variableType ->

                val actionName = "draw${variableType.toString().capitalize()}"

                viewStateHandlerTypeSpecBuilder.addFunction(
                    FunSpec.builder(actionName)
                        .addParameter("view", GenericViewStructureType)
                        .addParameter("state", GenerivViewStateType)
                        .build()
                )

                val PropFuncType = LambdaTypeName.get(
                    parameters = listOf(
                        ParameterSpec("view", GenericViewStructureType),
                        ParameterSpec("state", GenerivViewStateType)
                    ),
                    returnType = Unit::class.asTypeName()
                )

                val propFuncName = actionName + "Func"

                viewStateHandlerBuilderTypeSpecBuilder.addProperty(
                    PropertySpec.builder(propFuncName, PropFuncType)
                        .addModifiers(KModifier.PRIVATE)
                        .mutable()
                        .initializer("{_, _ ->}")
                        .build()
                )

                viewStateHandlerBuilderTypeSpecBuilder.addFunction(
                    FunSpec.builder(actionName)
                        .addParameter(propFuncName, PropFuncType)
                        .addStatement("this.$propFuncName = $propFuncName")
                        .build()
                )

                viewStateHandlerBuilderObj.addFunction(
                    FunSpec.builder(actionName)
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("view", GenericViewStructureType)
                        .addParameter("state", GenerivViewStateType)
                        .addStatement("$propFuncName(view, state)")
                        .build()
                )

                viewStateDispatcherFunSpecBuilder
                    .beginControlFlow(
                        "if (oldState == null || oldState.%N != newState.%N)",
                        variableType.toString(),
                        variableType.toString()
                    )
                    .addStatement(
                        "changes.add({ stateHandler?.%N(view, newState) })",
                        actionName
                    )
                    .endControlFlow()

            }

        viewStateHandlerBuilderTypeSpecBuilder.addFunction(
            FunSpec.builder("build")
                .returns(ClassName(packageName, viewStateHandlerName).parameterizedBy(GenericViewStructureType, GenerivViewStateType))
                .addStatement("return %L", viewStateHandlerBuilderObj.build())
                .build()
        )

        viewStateDispatcherFunSpecBuilder
            .addStatement("dispachtedCallback?.invoke(view, oldState, newState)")

        viewStateDispatcherFunSpecBuilder
            .addStatement("return changes")

        val viewStateDispatcherFunSpec = viewStateDispatcherFunSpecBuilder.build()
        viewStateDispatcherTypeSpecBuilder.addFunction(viewStateDispatcherFunSpec)

        val StateHandlerBuilderCallbackType = LambdaTypeName.get(
            parameters = listOf(),
            returnType = Unit::class.asTypeName(),
            receiver = StateHandlerBuilderType.parameterizedBy(GenericViewStructureType, GenerivViewStateType)
        )

        val viewStateHandlerBuilderFile = FileSpec.builder(packageName, viewStateHandlerName + "Builder")
            .addType(viewStateHandlerBuilderTypeSpecBuilder.build())
            .addFunction(
                FunSpec.builder(viewStateHandlerName.decapitalize())
                    .addAnnotation(ExperimentalCoroutinesApiAnnotation)
                    .addTypeVariable(GenericViewStructureType)
                    .addTypeVariable(GenerivViewStateType)
                    .addParameter(
                        ParameterSpec.builder("initializer", StateHandlerBuilderCallbackType)
                            .build()
                    )
                    .returns(ClassName(packageName, viewStateHandlerName).parameterizedBy(GenericViewStructureType, GenerivViewStateType))
                    .addStatement("return %T().apply(initializer).build()", StateHandlerBuilderType.parameterizedBy(GenericViewStructureType, GenerivViewStateType))
                    .build()
            )
            .build()
        val viewStateHandlerFile = FileSpec.builder(packageName, viewStateHandlerName)
            .addType(viewStateHandlerTypeSpecBuilder.build()).build()
        val viewStateDispatcherFile = FileSpec.builder(packageName, viewStateDispatcherName)
            .addType(viewStateDispatcherTypeSpecBuilder.build()).build()

        filer?.let { viewStateHandlerBuilderFile.writeTo(it) }
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

    override fun getSupportedAnnotationTypes(): MutableSet<String> =
        mutableSetOf(ReactiveComponent::class.java.name)

    override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latestSupported()

}
