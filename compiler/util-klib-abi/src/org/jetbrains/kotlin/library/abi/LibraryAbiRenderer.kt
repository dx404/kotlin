/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.AbiRendererImpl

/**
 * The default rendering implementation.
 */
@ExperimentalLibraryAbiReader
fun AbiTopLevelDeclarations.renderTopLevels(settings: AbiRenderingSettings): String = buildString {
    AbiRendererImpl(this@renderTopLevels, settings).renderTo(this)
}

/**
 * @property renderingOrder The order in which member declarations are rendered.
 *
 * @param renderedSignatureVersions One might want to render only some signatures, e.g. only [AbiSignatureVersion.V2] even if
 * [AbiSignatureVersion.V1] are available.
 */
@ExperimentalLibraryAbiReader
class AbiRenderingSettings(
    renderedSignatureVersions: Set<AbiSignatureVersion>,
    val whenSignatureNotFound: (AbiDeclaration, AbiSignatureVersion) -> String = { declaration, signatureVersion ->
        error("No signature $signatureVersion for ${declaration::class.java}, $declaration")
    },
    val renderingOrder: AbiRenderingOrder = AbiRenderingOrder.Default
) {
    init {
        require(renderedSignatureVersions.isNotEmpty())
    }

    val renderedSignatureVersions: List<AbiSignatureVersion> =
        renderedSignatureVersions.sortedDescending() // The latest version always goes first.
}

@ExperimentalLibraryAbiReader
fun interface AbiRenderingOrder {
    fun computeOrderOfRendering(container: AbiDeclarationContainer): List<AbiDeclaration>

    object Default : AbiRenderingOrder {
        override fun computeOrderOfRendering(container: AbiDeclarationContainer): List<AbiDeclaration> {
            // Just follow the existing order.
            return container.declarations
        }
    }
}
