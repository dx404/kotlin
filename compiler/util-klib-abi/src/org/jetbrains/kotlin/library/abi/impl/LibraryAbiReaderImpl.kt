/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi.impl

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignatureRenderer
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.library.*
import org.jetbrains.kotlin.library.abi.*
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.utils.compact
import java.io.File
import org.jetbrains.kotlin.konan.file.File as KFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrEnumEntry as ProtoEnumEntry
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty
import org.jetbrains.kotlin.backend.common.serialization.proto.IrValueParameter as ProtoValueParameter
import org.jetbrains.kotlin.backend.common.serialization.IrFlags as ProtoFlags

internal class LibraryAbiReaderImpl(libraryFile: File) {
    private val library = resolveSingleFileKlib(KFile(libraryFile.absolutePath))

    fun readAbi(): LibraryAbi {
        val supportedSignatureVersions = readSupportedSignatureVersions()

        return LibraryAbi(
            manifest = readManifest(),
            supportedSignatureVersions = supportedSignatureVersions,
            topLevelDeclarations = LibraryDeserializer(library, supportedSignatureVersions).deserialize()
        )
    }

    private fun readManifest(): LibraryManifest {
        val versions = library.versions
        return LibraryManifest(
            uniqueName = library.uniqueName,
            platform = library.builtInsPlatform,
            nativeTargets = library.nativeTargets.sorted(),
            compilerVersion = versions.compilerVersion,
            abiVersion = versions.abiVersion?.toString(),
            libraryVersion = versions.libraryVersion,
            irProviderName = library.irProviderName
        )
    }

    private fun readSupportedSignatureVersions(): Set<AbiSignatureVersion> {
        fun resolveSignatureVersion(signatureVersion: String): AbiSignatureVersion =
            AbiSignatureVersion.entries.firstOrNull { it.alias == signatureVersion }
                ?: error("Unsupported signature version: $signatureVersion")

        val signatureVersions = library.signatureVersions
        return if (signatureVersions.isNotEmpty())
            signatureVersions.mapTo(hashSetOf()) { signatureVersion -> resolveSignatureVersion(signatureVersion.lowercase()) }
        else
            setOf(AbiSignatureVersion.V1) // The default one.
    }
}

private class LibraryDeserializer(private val library: KotlinLibrary, supportedSignatureVersions: Set<AbiSignatureVersion>) {
    private val interner = IrInterningService()
    private val needV1Signatures = AbiSignatureVersion.V1 in supportedSignatureVersions
    private val needV2Signatures = AbiSignatureVersion.V2 in supportedSignatureVersions

    inner class FileDeserializer(private val fileIndex: Int) {
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))
        private val signatureDeserializer = IdSignatureDeserializer(fileReader, fileSignature = null, interner)

        fun deserializeTo(output: MutableList<AbiDeclaration>) {
            val proto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, IrLibraryFileFromBytes.extensionRegistryLite)
            proto.declarationIdList.mapNotNullTo(output) { topLevelDeclarationId ->
                deserializeDeclaration(fileReader.declaration(topLevelDeclarationId))
            }
        }

        private fun deserializeDeclaration(proto: ProtoDeclaration): AbiDeclaration? = when (proto.declaratorCase) {
            ProtoDeclaration.DeclaratorCase.IR_CLASS -> deserializeClass(proto.irClass)
            ProtoDeclaration.DeclaratorCase.IR_CONSTRUCTOR -> deserializeFunction(proto.irConstructor.base, isConstructor = true)
            ProtoDeclaration.DeclaratorCase.IR_FUNCTION -> deserializeFunction(proto.irFunction.base)
            ProtoDeclaration.DeclaratorCase.IR_PROPERTY -> deserializeProperty(proto.irProperty)
            ProtoDeclaration.DeclaratorCase.IR_ENUM_ENTRY -> deserializeEnumEntry(proto.irEnumEntry)
            else -> null
        }

        private fun deserializeClass(proto: ProtoClass): AbiClass? {
            if (!getVisibilityStatus(proto.base).isPubliclyVisible)
                return null

            val memberDeclarations = ArrayList<AbiDeclaration>()
            proto.declarationList.mapNotNullTo(memberDeclarations) { memberDeclaration ->
                deserializeDeclaration(memberDeclaration)
            }

            val flags = ClassFlags.decode(proto.base.flags)

            return AbiClassImpl(
                signatures = deserializeSignatures(proto.base),
                modality = flags.modality,
                kind = flags.kind,
                isInner = flags.isInner,
                isValue = flags.isValue,
                isFunction = flags.isFun,
                superTypes = emptySet(), // TODO
                declarations = memberDeclarations.compact()
            )
        }

        private fun deserializeEnumEntry(proto: ProtoEnumEntry): AbiEnumEntry {
            return AbiEnumEntryImpl(signatures = deserializeSignatures(proto.base))
        }

        private fun deserializeFunction(
            proto: ProtoFunctionBase,
            parentVisibilityStatus: VisibilityStatus? = null,
            isConstructor: Boolean = false
        ): AbiFunction? {
            if (!getVisibilityStatus(proto.base, parentVisibilityStatus).isPubliclyVisible)
                return null

            val flags = FunctionFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            return AbiFunctionImpl(
                signatures = deserializeSignatures(proto.base),
                modality = flags.modality,
                isConstructor = isConstructor,
                isInline = flags.isInline,
                isSuspend = flags.isSuspend,
                valueParameters = deserializeValueParameters(proto)
            )
        }

        private fun deserializeProperty(proto: ProtoProperty): AbiProperty? {
            val visibilityStatus = getVisibilityStatus(proto.base)
            if (!visibilityStatus.isPubliclyVisible)
                return null

            val flags = PropertyFlags.decode(proto.base.flags)
            if (flags.isFakeOverride) // TODO: FO of class with supertype from interop library
                return null

            return AbiPropertyImpl(
                signatures = deserializeSignatures(proto.base),
                modality = flags.modality,
                kind = when {
                    flags.isConst -> AbiPropertyKind.CONST_VAL
                    flags.isVar -> AbiPropertyKind.VAR
                    else -> AbiPropertyKind.VAL
                },
                getter = if (proto.hasGetter()) deserializeFunction(proto.getter.base, parentVisibilityStatus = visibilityStatus) else null,
                setter = if (proto.hasSetter()) deserializeFunction(proto.setter.base, parentVisibilityStatus = visibilityStatus) else null
            )
        }

        private fun deserializeSignatures(proto: ProtoDeclarationBase): AbiSignatures {
            val signature = deserializeIdSignature(proto.symbol)

            return AbiSignaturesImpl(
                signatureV1 = if (needV1Signatures) signature.render(IdSignatureRenderer.LEGACY) else null,
                signatureV2 = if (needV2Signatures) signature.render(IdSignatureRenderer.DEFAULT) else null
            )
        }

        private fun getVisibilityStatus(proto: ProtoDeclarationBase, parentVisibilityStatus: VisibilityStatus? = null): VisibilityStatus =
            when (ProtoFlags.VISIBILITY.get(proto.flags.toInt())) {
                ProtoBuf.Visibility.PUBLIC, ProtoBuf.Visibility.PROTECTED -> VisibilityStatus.PUBLIC
                ProtoBuf.Visibility.INTERNAL -> when {
                    parentVisibilityStatus == VisibilityStatus.INTERNAL_PUBLISHED_API -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    proto.annotationList.any { deserializeIdSignature(it.symbol).isPublishedApi() } -> VisibilityStatus.INTERNAL_PUBLISHED_API
                    else -> VisibilityStatus.NON_PUBLIC
                }
                else -> VisibilityStatus.NON_PUBLIC
            }

        private fun deserializeIdSignature(symbol: Long): IdSignature {
            val signatureId = BinarySymbolData.decode(symbol).signatureId
            return signatureDeserializer.deserializeIdSignature(signatureId)
        }

        private fun deserializeValueParameters(proto: ProtoFunctionBase): List<AbiValueParameter> {
            val allValueParameters: List<ProtoValueParameter> = proto.valueParameterList
            if (allValueParameters.isEmpty())
                return emptyList()

            val contextReceiverParametersCount = if (proto.hasContextReceiverParametersCount()) proto.contextReceiverParametersCount else 0

            val valueParameterRange = contextReceiverParametersCount until allValueParameters.size
            if (valueParameterRange.isEmpty())
                return emptyList()

            return valueParameterRange.mapTo(
                ArrayList(/*initialCapacity = */ valueParameterRange.last - valueParameterRange.first)
            ) { index ->
                val valueParameter = allValueParameters[index]
                val flags = ValueParameterFlags.decode(valueParameter.base.flags)

                AbiValueParameterImpl(
                    hasDefaultArg = valueParameter.hasDefaultValue(),
                    isNoinline = flags.isNoInline,
                    isCrossinline = flags.isCrossInline
                )
            }
        }
    }

    fun deserialize(): AbiTopLevelDeclarations {
        val topLevels = ArrayList<AbiDeclaration>()

        for (fileIndex in 0 until library.fileCount()) {
            FileDeserializer(fileIndex).deserializeTo(topLevels)
        }

        return AbiTopLevelDeclarationsImpl(topLevels.compact())
    }

    private enum class VisibilityStatus(val isPubliclyVisible: Boolean) {
        PUBLIC(true), INTERNAL_PUBLISHED_API(true), NON_PUBLIC(false)
    }

    companion object {
        private const val PUBLISHED_API_PACKAGE_NAME = "kotlin"
        private const val PUBLISHED_API_DECLARATION_NAME = "PublishedApi.<init>"

        private fun IdSignature.isPublishedApi(): Boolean =
            this is IdSignature.CommonSignature
                    && packageFqName == PUBLISHED_API_PACKAGE_NAME
                    && declarationFqName == PUBLISHED_API_DECLARATION_NAME
    }
}
