// FIR_IDENTICAL

class Invariant<CT>

fun <FT> build(builder: Invariant<FT>.() -> Unit): Invariant<FT> = Invariant()

fun test(arg: String) {
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> {}
    <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!>(fun(it) {})
}
