// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FIR_IDENTICAL
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*


interface InterfaceSerializer: KSerializer<WithInterfaceSerializser>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(InterfaceSerializer::class)<!>
class WithInterfaceSerializser(val i: Int)


abstract class AbstractSerializer: KSerializer<WithAbstract>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(AbstractSerializer::class)<!>
class WithAbstract(val i: Int)


sealed class SealedSerializer: KSerializer<WithSealed>

<!ABSTRACT_SERIALIZER_TYPE!>@Serializable(SealedSerializer::class)<!>
class WithSealed(val i: Int)
