/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality

/**
 * @property manifest Information from manifest that might be useful.
 * @property supportedSignatureVersions The versions of signatures supported by the given KLIB.
 * @property topLevelDeclarations Top-level declarations.
 */
@ExperimentalLibraryAbiReader
class LibraryAbi(
    val manifest: LibraryManifest,
    val supportedSignatureVersions: Set<AbiSignatureVersion>,
    val topLevelDeclarations: AbiTopLevelDeclarations
)

@ExperimentalLibraryAbiReader
enum class AbiSignatureVersion(val alias: String) {
    /**
     *  The signatures with hashes.
     */
    V1("1"),

    /**
     * The self-descriptive signatures (with mangled names).
     */
    V2("2"),
}

@ExperimentalLibraryAbiReader
interface AbiSignatures {
    /** Returns the signature of the specified [AbiSignatureVersion] **/
    operator fun get(signatureVersion: AbiSignatureVersion): String?
}

@ExperimentalLibraryAbiReader
interface AbiDeclaration {
    val signatures: AbiSignatures
}

@ExperimentalLibraryAbiReader
interface AbiPossiblyTopLevelDeclaration : AbiDeclaration {
    val modality: Modality
}

/**
 * Important: The order of [declarations] is preserved exactly as in serialized IR.
 * Would you need to use a different order while rendering, please refer to [AbiRenderingSettings.renderingOrder].
 */
@ExperimentalLibraryAbiReader
interface AbiDeclarationContainer {
    val declarations: List<AbiDeclaration>
}

@ExperimentalLibraryAbiReader
interface AbiTopLevelDeclarations : AbiDeclarationContainer

@ExperimentalLibraryAbiReader
interface AbiClass : AbiPossiblyTopLevelDeclaration, AbiDeclarationContainer {
    val kind: ClassKind
    val isInner: Boolean
    val isValue: Boolean
    val isFunction: Boolean

    /** The set of non-trivial supertypes (i.e. excluding [kotlin.Any] for regular classes, [kotlin.Enum] for enums, etc). */
    val superTypes: Set<AbiSuperType>
}

// TODO: decide how to render type arguments (effective(?) variance and effective(?) upper-bounds)
typealias AbiSuperType = String

@ExperimentalLibraryAbiReader
interface AbiEnumEntry : AbiDeclaration

@ExperimentalLibraryAbiReader
interface AbiFunction : AbiPossiblyTopLevelDeclaration {
    val isConstructor: Boolean
    val isInline: Boolean
    val isSuspend: Boolean

    /** Additional value parameter flags that might affect binary compatibility and that should be rendered along with the function itself. */
    val valueParameters: List<AbiValueParameter>
}

interface AbiValueParameter {
    val hasDefaultArg: Boolean
    val isNoinline: Boolean
    val isCrossinline: Boolean
}

@ExperimentalLibraryAbiReader
interface AbiProperty : AbiPossiblyTopLevelDeclaration {
    val kind: AbiPropertyKind
    val getter: AbiFunction?
    val setter: AbiFunction?
}

@ExperimentalLibraryAbiReader
enum class AbiPropertyKind { VAL, CONST_VAL, VAR }
