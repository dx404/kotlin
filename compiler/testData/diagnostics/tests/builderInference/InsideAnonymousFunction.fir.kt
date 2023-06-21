// ISSUE: KT-59551

class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: Invariant<FT>.() -> Unit): Invariant<FT> = Invariant()

fun test(arg: String) {
    build(fun(it) { it.yield(arg) })
}
