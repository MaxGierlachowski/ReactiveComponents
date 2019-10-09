package io.gierla.reactivecomponents

import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class ReactiveComponent(val viewType: KClass<*>)