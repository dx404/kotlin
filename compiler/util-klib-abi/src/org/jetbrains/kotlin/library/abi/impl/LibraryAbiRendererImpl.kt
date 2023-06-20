/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.library.abi.*
import java.lang.Appendable

@ExperimentalLibraryAbiReader
internal class AbiRendererImpl(
    private val topLevelDeclarations: AbiTopLevelDeclarations,
    private val settings: AbiRenderingSettings
) {
    fun renderTo(output: Appendable) {
        topLevelDeclarations.renderDeclarationContainer(output)
    }

    private fun AbiDeclarationContainer.renderDeclarationContainer(output: Appendable) {
        settings.renderingOrder.computeOrderOfRendering(this).forEach { declaration ->
            when (declaration) {
                is AbiClass -> declaration.renderClass(output)
                is AbiEnumEntry -> declaration.renderEnumEntry(output)
                is AbiFunction -> declaration.renderFunction(output)
                is AbiProperty -> declaration.renderProperty(output)
            }
        }
    }

    private fun AbiClass.renderClass(output: Appendable): Unit = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            if (isInner) output.append("inner ")
            if (isValue) output.append("value ")
            if (isFunction) output.append("fun ")
            output.appendClassKind(kind)
        },
        doAfterSignatures = {
            if (superTypes.isNotEmpty()) {
                output.append(" : ")
                superTypes.sorted().joinTo(output, separator = ", ")
            }

            if (declarations.isNotEmpty()) {
                output.appendLine(" {")
                indented {
                    renderDeclarationContainer(output)
                }
                output.appendIndent().append('}')
            }

            output.appendLine()
        }
    )

    private fun AbiEnumEntry.renderEnumEntry(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            output.append(ENUM_ENTRY_REPRESENTATION)
        }
    )

    private fun AbiFunction.renderFunction(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            if (isSuspend) output.append("suspend ")
            if (isInline) output.append("inline ")
            output.append(if (isConstructor) "constructor" else "fun")
            output.appendValueParameterFlags(valueParameters)
        }
    )

    private fun AbiProperty.renderProperty(output: Appendable) = renderDeclarationBase(
        output,
        doBeforeSignatures = {
            output.appendPropertyKind(kind)
        },
        doAfterSignatures = {
            output.appendLine()
            indented {
                getter?.renderFunction(output)
                setter?.renderFunction(output)
            }
        }
    )

    private inline fun <T : AbiDeclaration> T.renderDeclarationBase(
        output: Appendable,
        doBeforeSignatures: T.() -> Unit,
        doAfterSignatures: T.() -> Unit = { output.appendLine() },
    ) {
        output.appendIndent()
        if (this is AbiPossiblyTopLevelDeclaration && needToRenderModality) output.appendModality(modality).append(' ')
        doBeforeSignatures()
        output.append(' ').appendSignatures(this)
        doAfterSignatures()
    }

    private var indent = 0u

    private inline fun indented(block: () -> Unit) {
        indent++
        try {
            block()
        } finally {
            indent--
        }
    }

    private fun Appendable.appendIndent(): Appendable {
        for (i in 0u until indent) append("    ")
        return this
    }

    private val AbiPossiblyTopLevelDeclaration.needToRenderModality: Boolean
        get() = this !is AbiFunction || !isConstructor || modality != Modality.FINAL

    private fun Appendable.appendModality(modality: Modality): Appendable = append(modality.name.lowercase())

    private fun Appendable.appendClassKind(classKind: ClassKind): Appendable =
        append(
            classKind.codeRepresentation
                ?: if (classKind == ClassKind.ENUM_ENTRY) ENUM_ENTRY_REPRESENTATION else error("Unexpected class kind: $classKind")
        )

    private fun Appendable.appendPropertyKind(propertyKind: AbiPropertyKind): Appendable =
        append(
            when (propertyKind) {
                AbiPropertyKind.VAL -> "val"
                AbiPropertyKind.CONST_VAL -> "const val"
                AbiPropertyKind.VAR -> "var"
            }
        )

    private fun Appendable.appendSignatures(declaration: AbiDeclaration): Appendable {
        settings.renderedSignatureVersions.joinTo(this, separator = ", ") { signatureVersion ->
            declaration.signatures[signatureVersion] ?: settings.whenSignatureNotFound(declaration, signatureVersion)
        }
        return this
    }

    private fun Appendable.appendValueParameterFlags(valueParameters: List<AbiValueParameter>): Appendable {
        var hadMeaningfulFlagsToRender = false

        valueParameters.forEachIndexed { index, valueParameter ->
            val flags = listOfNotNull(
                "default_arg".takeIf { valueParameter.hasDefaultArg },
                "noinline".takeIf { valueParameter.isNoinline },
                "crossinline".takeIf { valueParameter.isCrossinline }
            )
            if (flags.isEmpty()) return@forEachIndexed

            append(if (hadMeaningfulFlagsToRender) ' ' else '[')
            append("$index:")
            flags.joinTo(this, separator = ",")
            hadMeaningfulFlagsToRender = true
        }

        if (hadMeaningfulFlagsToRender)
            append(']')

        return this
    }

    companion object {
        private const val ENUM_ENTRY_REPRESENTATION = "enum entry"
    }
}
