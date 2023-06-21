// ISSUE: KT-47986

class Invariant<CT1, CT2> {
    fun yield(ct1: CT1, ct2: CT2) {}
    fun yieldX(ct1: CT1) {}
    fun yieldY(ct2: CT2) {}
}

fun <FT1, FT2> build(builder: Invariant<FT1, FT2>.() -> Unit): Invariant<FT1, FT2> = Invariant()

fun <T> produce(): T = null!!

fun test1(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldX(arg) }
}

fun test2(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldY(arg) }
}

fun test3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yield(produce(), produce()) }
}

fun test4(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yield(arg, produce()) }
}

fun test5(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yield(produce(), arg) }
}

fun test6() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldX(produce()) }
}

fun test7() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldY(produce()) }
}

fun test8() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER, NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        yieldX(produce())
        yieldY(produce())
    }
}

fun test9(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        yieldX(arg)
        yieldY(produce())
    }
}

fun test10(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        yieldX(produce())
        yieldY(arg)
    }
}
