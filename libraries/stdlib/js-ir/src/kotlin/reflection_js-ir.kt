/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.reflect.*
import kotlin.reflect.js.internal.*

@PublishedApi
internal fun <T : Annotation> KClass<*>.findAssociatedObject(annotationClass: KClass<T>): Any? {
    return if (this is KClassImpl<*> && annotationClass is KClassImpl<T>) {
        val key = annotationClass.jClass.asDynamic().`$metadata$`?.associatedObjectKey?.unsafeCast<Int>() ?: return null
        val map = jClass.asDynamic().`$metadata$`?.associatedObjects ?: return null
        val factory = map[key] ?: return null
        return factory()
    } else {
        null
    }
}

@OptIn(JsIntrinsic::class)
@ExperimentalJsReflectionCreateInstance
public fun <T : Any> KClass<T>.createInstance(): T {
    val jsClass = js.asDynamic()

    if (jsClass === js("Object")) return js("{}")

    val noArgsConstructor = jsClass.`$metadata$`.unsafeCast<Metadata?>()?.defaultConstructor
        ?: throw IllegalArgumentException("Class \"$simpleName\" should have a single no-arg constructor")

    return if (jsIsEs6() && noArgsConstructor !== jsClass) {
        js("noArgsConstructor.call(jsClass)")
    } else {
        js("new noArgsConstructor()")
    }
}
