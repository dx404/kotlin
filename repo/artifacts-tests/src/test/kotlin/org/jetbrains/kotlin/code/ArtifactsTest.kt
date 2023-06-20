/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.test.Test

class ArtifactsTest {

    private val kotlinVersion = System.getProperty("kotlin.version")
    private val mavenLocal = System.getProperty("maven.repo.local")
    private val localRepoPath = Paths.get(mavenLocal, "org/jetbrains/kotlin")
    private val expectedRepoPath = Paths.get("repo/artifacts-tests/src/test/resources/org/jetbrains/kotlin")

    private fun artifactExtFilter(version: String, ext: String): (Path, BasicFileAttributes) -> Boolean {
        return { path: Path, fileAttributes: BasicFileAttributes ->
            fileAttributes.isRegularFile
                    && "${path.fileName}".endsWith(ext, ignoreCase = true)
                    && path.contains(Paths.get(version))
        }
    }

    @Test
    fun verifyArtifactFiles() {
        val actualPoms = Files.find(localRepoPath, Integer.MAX_VALUE, artifactExtFilter(kotlinVersion, ".pom"))
        actualPoms.forEach { actual ->
            val expectedPomPath = actual.toExpectedPath()
            val actualString = actual.toFile().readText()
            assertEqualsToFile(expectedPomPath, actualString) { it.replace("ArtifactsTest.version", kotlinVersion) }
        }
    }

    /**
     * convert:
     * ${mavenLocal}/org/jetbrains/kotlin/artifact/version/artifact-version.pom
     * to:
     * ${expectedRepository}/org/jetbrains/kotlin/artifact/artifact.pom
     */
    private fun Path.toExpectedPath(): Path {
        val artifactDirPath = localRepoPath.relativize(this).parent.parent
        val expectedFileName = "${artifactDirPath.fileName}.pom"
        return expectedRepoPath.resolve(artifactDirPath.resolve(expectedFileName))
    }
}