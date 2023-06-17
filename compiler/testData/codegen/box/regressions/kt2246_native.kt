// TARGET_BACKEND: NATIVE
// WITH_STDLIB

fun box(): String {
    @OptIn(kotlin.experimental.ExperimentalNativeApi::class)
    kotlin.assert(true)
    return "OK"
}