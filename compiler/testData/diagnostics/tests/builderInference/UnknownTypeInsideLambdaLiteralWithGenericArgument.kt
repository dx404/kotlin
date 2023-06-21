// ISSUE: KT-47986

class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: (Invariant<FT>) -> Unit): Invariant<FT> = Invariant()

fun <T> produce(): T = null!!

fun test() {
    <!INFERRED_INTO_DECLARED_UPPER_BOUNDS!>build<!> { it.yield(produce()) }
}
