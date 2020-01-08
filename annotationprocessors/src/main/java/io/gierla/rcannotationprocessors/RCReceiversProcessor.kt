package io.gierla.rcannotationprocessors

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import io.gierla.rccore.action.Action
import io.gierla.rccore.annotations.RCReceivers
import io.gierla.rccore.state.State
import io.gierla.rccore.state.StateDispatcher
import io.gierla.rccore.store.DefaultStore
import io.gierla.rccore.store.Store
import io.gierla.rccore.view.Structure
import io.reactivex.disposables.CompositeDisposable
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.ElementFilter


class RCReceiversProcessor(private val messager: Messager?, private val filer: Filer?, private val processingEnv: ProcessingEnvironment) {

    fun getReceivers(roundEnvironment: RoundEnvironment?): Set<TypeElementClass> {
        val annotatedElements = roundEnvironment?.getElementsAnnotatedWith(RCReceivers::class.java) ?: mutableSetOf()
        val elementsWithMatchingType = listOf<TypeElement>(*ElementFilter.typesIn(annotatedElements).toTypedArray())

        val receiverSet = mutableSetOf<TypeElementClass>()

        elementsWithMatchingType.forEach { typeElement ->

            val annotationMirrors: List<AnnotationMirror> = typeElement.annotationMirrors
            for (annotationMirror in annotationMirrors) {

                val typeUtils = processingEnv.typeUtils

                annotationMirror.elementValues
                    .filter { it.key.simpleName.toString() == "receivers" }
                    .map { it.value.value }
                    .filterIsInstance<List<*>>()
                    .flatten()
                    .filterIsInstance<AnnotationValue>()
                    .map { it.value }
                    .filterIsInstance<TypeMirror>()
                    .map {
                        typeUtils.asElement(it)
                    }
                    .filterIsInstance<TypeElement>()
                    .forEach {
                        receiverSet.add(TypeElementClass(typeElement, it))
                    }
            }

        }

        return receiverSet
    }

    fun processReceivers(receivers: Set<TypeElementClass>) {
        receivers.forEach {
            createRCReceiver(it)
        }
    }

    private fun createRCReceiver(receiver: TypeElementClass) {

        val packageName = processingEnv.elementUtils.getPackageOf(receiver.rootTypeElement).toString()

        val className = receiver.typeElement.simpleName.toString()
        val viewParentType = ClassName("", receiver.typeElement.qualifiedName.toString())

        /* -- Class Constructor -- */

        val viewTypeSpecBuilder = TypeSpec.classBuilder("Reactive$className").addModifiers(KModifier.ABSTRACT)
        viewTypeSpecBuilder.superclass(viewParentType)

        val stateTypeVariableName = TypeVariableName("S", State::class)
        val actionTypeVariableName = TypeVariableName("A", Action::class)
        val structureTypeVariableName = TypeVariableName("T", Structure::class)

        viewTypeSpecBuilder.addTypeVariable(stateTypeVariableName)
        viewTypeSpecBuilder.addTypeVariable(actionTypeVariableName)
        viewTypeSpecBuilder.addTypeVariable(structureTypeVariableName)

        viewTypeSpecBuilder.addProperty(
            PropertySpec.builder(name = "initialState", type = stateTypeVariableName).addModifiers(KModifier.PRIVATE).build()
        )

        receiver.typeElement.enclosedElements.filter { it.kind == ElementKind.CONSTRUCTOR }.filterIsInstance<ExecutableElement>().forEach { constructor ->
            val constructorBuilder = FunSpec.constructorBuilder()
            val parameterNames = mutableSetOf<String>()
            constructor.parameters.forEach { variable ->
                parameterNames.add(variable.simpleName.toString())
                constructorBuilder.addParameter(
                    ParameterSpec.builder(variable.simpleName.toString(), variable.asType().asTypeName()).build()
                )
            }

            constructorBuilder.addParameter(
                ParameterSpec.builder("initialState", stateTypeVariableName).build()
            )

            constructorBuilder.addStatement("this.initialState = initialState")
            constructorBuilder.addStatement("this.store = %T<%T, %T>(initialState = initialState)", DefaultStore::class.asTypeName(), stateTypeVariableName, actionTypeVariableName)

            constructorBuilder.callSuperConstructor(*parameterNames.toTypedArray())
            viewTypeSpecBuilder.addFunction(constructorBuilder.build())
        }

        /* -- Class Constructor End -- */
        /* -- Variables -- */

        // Store
        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder(
                    "store", Store::class.asClassName().parameterizedBy(
                        stateTypeVariableName,
                        actionTypeVariableName
                    )
                )
                .build()
        )

        // Disposable
        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder("disposable", CompositeDisposable::class.asTypeName())
                .initializer("%T()", CompositeDisposable::class.asTypeName())
                .addModifiers(KModifier.PRIVATE)
                .build()
        )

        // Dispatcher
        viewTypeSpecBuilder.addProperty(
            PropertySpec
                .builder(
                    "dispatcher", StateDispatcher::class.asTypeName().parameterizedBy(
                        structureTypeVariableName,
                        stateTypeVariableName
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

        /*messager?.printMessage(
            Diagnostic.Kind.WARNING,
            "${variable.simpleName}"
        )*/

        val viewFile = FileSpec.builder(packageName, "Reactive$className").addType(viewTypeSpecBuilder.build()).build()

        filer?.let { viewFile.writeTo(it) }

    }

    data class TypeElementClass(val rootTypeElement: TypeElement, val typeElement: TypeElement) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TypeElementClass

            if (typeElement.simpleName != other.typeElement.simpleName) return false

            return true
        }

        override fun hashCode(): Int {
            return typeElement.hashCode()
        }
    }

}