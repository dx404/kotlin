// FIR_IDENTICAL
// WITH_STDLIB
// MODULE: m1-common
// FILE: common.kt
@Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.CLASS)
annotation class Ann

@Ann
expect class KtTypealiasNotMatch

@Ann
expect class AnnotationsNotConsideredOnTypealias

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
class KtTypealiasNotMatchImpl

actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>KtTypealiasNotMatch<!> = KtTypealiasNotMatchImpl

class AnnotationsNotConsideredOnTypealiasImpl

@Ann
actual typealias <!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>AnnotationsNotConsideredOnTypealias<!> = AnnotationsNotConsideredOnTypealiasImpl
