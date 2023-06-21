// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-59551

class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: Invariant<FT>.() -> Unit): Invariant<FT> = Invariant()

fun <T> Invariant<T>.identity(arg: T): T = arg

fun test(arg: String) {
    val invariant = build(fun(it) { it.yield(arg) })
    val result: String = invariant.identity(arg)
}

fun box(): String {
    test("")
    return "OK"
}
