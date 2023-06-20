/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.library.abi.*

internal data class AbiSignaturesImpl(private val signatureV1: String?, private val signatureV2: String?) : AbiSignatures {
    override operator fun get(signatureVersion: AbiSignatureVersion): String? = when (signatureVersion) {
        AbiSignatureVersion.V1 -> signatureV1
        AbiSignatureVersion.V2 -> signatureV2
    }
}

internal class AbiTopLevelDeclarationsImpl(
    override val declarations: List<AbiDeclaration>
) : AbiTopLevelDeclarations

internal class AbiClassImpl(
    override val signatures: AbiSignatures,
    override val modality: Modality,
    override val kind: ClassKind,
    isInner: Boolean,
    isValue: Boolean,
    isFunction: Boolean,
    override val superTypes: Set<AbiSuperType>,
    override val declarations: List<AbiDeclaration>
) : AbiClass {
    private val flags = IS_INNER(isInner) or IS_VALUE(isValue) or IS_FUNCTION(isFunction)

    override val isInner get() = IS_INNER(flags)
    override val isValue get() = IS_VALUE(flags)
    override val isFunction get() = IS_FUNCTION(flags)

    companion object {
        private val IS_INNER = AbiBooleanFlag(0u)
        private val IS_VALUE = AbiBooleanFlag(1u)
        private val IS_FUNCTION = AbiBooleanFlag(2u)
    }
}

internal class AbiEnumEntryImpl(
    override val signatures: AbiSignatures
) : AbiEnumEntry

internal class AbiFunctionImpl(
    override val signatures: AbiSignatures,
    override val modality: Modality,
    isConstructor: Boolean,
    isInline: Boolean,
    isSuspend: Boolean,
    override val valueParameters: List<AbiValueParameter>
) : AbiFunction {
    private val flags = IS_CONSTRUCTOR(isConstructor) or IS_INLINE(isInline) or IS_SUSPEND(isSuspend)

    override val isConstructor get() = IS_CONSTRUCTOR(flags)
    override val isInline get() = IS_INLINE(flags)
    override val isSuspend get() = IS_SUSPEND(flags)

    companion object {
        private val IS_CONSTRUCTOR = AbiBooleanFlag(0u)
        private val IS_INLINE = AbiBooleanFlag(1u)
        private val IS_SUSPEND = AbiBooleanFlag(2u)
    }
}

@JvmInline
internal value class AbiValueParameterImpl private constructor(private val flags: Int) : AbiValueParameter {
    constructor(hasDefaultArg: Boolean, isNoinline: Boolean, isCrossinline: Boolean) : this(
        HAS_DEFAULT_ARG(hasDefaultArg) or IS_NOINLINE(isNoinline) or IS_CROSSINLINE(isCrossinline)
    )

    override val hasDefaultArg get() = HAS_DEFAULT_ARG(flags)
    override val isNoinline get() = IS_NOINLINE(flags)
    override val isCrossinline get() = IS_CROSSINLINE(flags)

    companion object {
        private val HAS_DEFAULT_ARG = AbiBooleanFlag(0u)
        private val IS_NOINLINE = AbiBooleanFlag(1u)
        private val IS_CROSSINLINE = AbiBooleanFlag(2u)
    }
}

internal class AbiPropertyImpl(
    override val signatures: AbiSignatures,
    override val modality: Modality,
    override val kind: AbiPropertyKind,
    override val getter: AbiFunction?,
    override val setter: AbiFunction?
) : AbiProperty

private class AbiBooleanFlag(private val index: UInt) {
    operator fun invoke(state: Boolean): Int = if (state) 1 shl index.toInt() else 0
    operator fun invoke(flags: Int): Boolean = (flags ushr index.toInt()) and 1 != 0
}
