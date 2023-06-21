class Invariant<CT1, CT2> {
    fun yield(ct1: CT1, ct2: CT2) {}
    fun yieldX(ct1: CT1) {}
    fun yieldY(ct2: CT2) {}
}

fun <FT> build(builder: Invariant<FT, FT>.() -> Unit): Invariant<FT, FT> = Invariant()

fun <T> produce(): T = null!!

fun test1(arg: String) {
    val invariant = build { yield(arg, produce()) }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun test2(arg: String) {
    val invariant = build { yield(produce(), arg) }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun test3(arg: String) {
    val invariant = build { yieldX(arg) }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun test4(arg: String) {
    val invariant = build { yieldY(arg) }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun test5(arg: String) {
    val invariant = build {
        yieldX(arg)
        yieldY(produce())
    }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun test6(arg: String) {
    val invariant = build {
        yieldX(produce())
        yieldY(arg)
    }
    val result1: String = invariant.identityLeft(arg)
    val result2: String = invariant.identityRight(arg)
}

fun <T> Invariant<T, *>.identityLeft(arg: T): T = arg
fun <T> Invariant<*, T>.identityRight(arg: T): T = arg

fun box(): String {
    test1("")
    test2("")
    test3("")
    test4("")
    test5("")
    test6("")
    return "OK"
}
