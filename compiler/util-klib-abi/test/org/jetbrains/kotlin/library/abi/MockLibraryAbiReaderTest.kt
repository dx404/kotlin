/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.abi

import org.jetbrains.kotlin.library.abi.impl.MockLibraryAbiReader
import org.jetbrains.kotlin.test.util.convertLineSeparators
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File

class MockLibraryAbiReaderTest {
    private val fakeLibrary = File(System.getProperty("user.dir")).resolve("fake-library.klib")

    private fun readAbiTextFile(fileName: String): String = this::class.java.getResourceAsStream(fileName)
        ?.bufferedReader()
        ?.use { it.readText().convertLineSeparators() }
        ?: fail("Can't read ABI text file: $fileName")

    @Test
    fun testDefaultRendererWithSignaturesV1() {
        val expectedAbiText = readAbiTextFile("/mock-klib-abi-dump-v1.txt")

        val libraryAbi = MockLibraryAbiReader.readAbiInfo(fakeLibrary)
        val actualAbiText = libraryAbi.topLevelDeclarations.renderTopLevels(
            AbiRenderingSettings(renderedSignatureVersions = setOf(AbiSignatureVersion.V1))
        )

        assertEquals(expectedAbiText, actualAbiText)
    }

    @Test
    fun testDefaultRendererWithSignaturesV2() {
        val expectedAbiText = readAbiTextFile("/mock-klib-abi-dump-v2.txt")

        val libraryAbi = MockLibraryAbiReader.readAbiInfo(fakeLibrary)
        val actualAbiText = libraryAbi.topLevelDeclarations.renderTopLevels(
            AbiRenderingSettings(renderedSignatureVersions = setOf(AbiSignatureVersion.V2))
        )

        assertEquals(expectedAbiText, actualAbiText)
    }

    @Test
    fun testDefaultRendererWithSignaturesV1V2() {
        val expectedAbiText = readAbiTextFile("/mock-klib-abi-dump-v1v2.txt")

        val libraryAbi = MockLibraryAbiReader.readAbiInfo(fakeLibrary)
        val actualAbiText = libraryAbi.topLevelDeclarations.renderTopLevels(
            AbiRenderingSettings(renderedSignatureVersions = setOf(AbiSignatureVersion.V1, AbiSignatureVersion.V2))
        )

        assertEquals(expectedAbiText, actualAbiText)
    }

    @Test
    fun testManifestInfo() {
        val manifest = MockLibraryAbiReader.readAbiInfo(fakeLibrary).manifest

        with(manifest) {
            assertEquals("fake-library", uniqueName)
            assertEquals("NATIVE", platform)
            assertEquals(listOf("ios_arm64"), nativeTargets)
            assertEquals("1.9.20", compilerVersion)
            assertEquals("1.8.0", abiVersion)
            assertEquals("0.0.1", libraryVersion)
            assertEquals("fake-ir-provider", irProviderName)
        }
    }
}