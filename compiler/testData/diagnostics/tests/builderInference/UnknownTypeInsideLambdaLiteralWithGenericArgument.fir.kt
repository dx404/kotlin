// ISSUE: KT-47986

class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: (Invariant<FT>) -> Unit): Invariant<FT> = Invariant()

fun <T> produce(): T = null!!

fun test() {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> { it.yield(produce()) }
}
