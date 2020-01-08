package io.gierla.rccore.annotations

import kotlin.reflect.KClass

@MustBeDocumented
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class RCReceivers(val receivers: Array<KClass<*>>)