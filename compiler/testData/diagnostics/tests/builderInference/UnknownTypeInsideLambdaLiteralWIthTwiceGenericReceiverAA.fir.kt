// ISSUE: KT-47986

class Invariant<CT1, CT2> {
    fun yield(ct1: CT1, ct2: CT2) {}
    fun yieldX(ct1: CT1) {}
    fun yieldY(ct2: CT2) {}
}

fun <FT> build(builder: Invariant<FT, FT>.() -> Unit): Invariant<FT, FT> = Invariant()

fun <T> produce(): T = null!!

fun test1() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yield(produce(), produce()) }
}

fun test2() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldX(produce()) }
}

fun test3() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { yieldY(produce()) }
}

fun test4() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {
        yieldX(produce())
        yieldY(produce())
    }
}
