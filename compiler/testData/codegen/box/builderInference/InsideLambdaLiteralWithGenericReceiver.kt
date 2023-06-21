class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: Invariant<FT>.() -> Unit): Invariant<FT> = Invariant()

fun test(arg: String) {
    val invariant = build { yield(arg) }
    val result: String = invariant.identity(arg)
}

fun <T> Invariant<T>.identity(arg: T): T = arg

fun box(): String {
    test("")
    return "OK"
}
