class Invariant<CT1, CT2> {
    fun yield(ct1: CT1, ct2: CT2) {}
    fun yieldX(ct1: CT1) {}
    fun yieldY(ct2: CT2) {}
}

fun <FT1, FT2> build(builder: Invariant<FT1, FT2>.() -> Unit): Invariant<FT1, FT2> = Invariant()

fun test1(arg1: String, arg2: Int) {
    val invariant = build { yield(arg1, arg2) }
    val result1: String = invariant.identityLeft(arg1)
    val result2: Int = invariant.identityRight(arg2)
}

fun test2(arg1: String, arg2: Int) {
    val invariant = build {
        yieldX(arg1)
        yieldY(arg2)
    }
    val result1: String = invariant.identityLeft(arg1)
    val result2: Int = invariant.identityRight(arg2)
}

fun <T> Invariant<T, *>.identityLeft(arg: T): T = arg
fun <T> Invariant<*, T>.identityRight(arg: T): T = arg

fun box(): String {
    test1("", 42)
    test2("", 42)
    return "OK"
}
