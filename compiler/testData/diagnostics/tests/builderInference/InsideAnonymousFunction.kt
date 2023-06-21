// ISSUE: KT-59551

class Invariant<CT> {
    fun yield(ct: CT) {}
}

fun <FT> build(builder: Invariant<FT>.() -> Unit): Invariant<FT> = Invariant()

fun test(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!>(fun(it) { it.<!UNRESOLVED_REFERENCE!>yield<!>(arg) })
}
