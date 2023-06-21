// ISSUE: KT-47986

class Invariant<CT1, CT2> {
    fun yield(ct1: CT1, ct2: CT2) {}
    fun yieldX(ct1: CT1) {}
    fun yieldY(ct2: CT2) {}
}

fun <FT> build(builder: Invariant<FT, FT>.() -> Unit): Invariant<FT, FT> = Invariant()

fun <T> produce(): T = null!!

fun test1() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> { yield(produce(), produce()) }
}

fun test2() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> { yieldX(produce()) }
}

fun test3() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> { yieldY(produce()) }
}

fun test4() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> {
        yieldX(produce())
        yieldY(produce())
    }
}
