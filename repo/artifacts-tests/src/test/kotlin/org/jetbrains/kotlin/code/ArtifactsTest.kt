/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code

import org.apache.maven.model.io.xpp3.MavenXpp3Reader
import org.apache.maven.model.io.xpp3.MavenXpp3Writer
import org.jetbrains.kotlin.test.services.JUnit5Assertions.assertEqualsToFile
import java.io.StringReader
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.attribute.BasicFileAttributes
import kotlin.test.Test

private typealias PomFilter = (Path, BasicFileAttributes) -> Boolean

class ArtifactsTest {

    private val mavenReader = MavenXpp3Reader()
    private val mavenWriter = MavenXpp3Writer()
    private val kotlinVersion = System.getProperty("kotlin.version")
    private val mavenLocal = System.getProperty("maven.repo.local")
    private val localRepoPath = Paths.get(mavenLocal, "org", "jetbrains", "kotlin")
    private val expectedRepoPath = Paths.get("repo", "artifacts-tests", "src", "test", "resources", "org", "jetbrains", "kotlin")
    private val versionRegex = "-\\d+\\.\\d+\\.\\d+(-SNAPSHOT)?".toRegex()

    private val pomFilterWithVersion: PomFilter = { path: Path, fileAttributes: BasicFileAttributes ->
        fileAttributes.isRegularFile
                && "${path.fileName}".endsWith(".pom", ignoreCase = true)
                && path.contains(Paths.get(kotlinVersion))
    }

    @Test
    fun verifyArtifactFiles() {
        val actualPoms = Files.find(localRepoPath, Integer.MAX_VALUE, pomFilterWithVersion)
        actualPoms.forEach { actual ->
            val expectedPomPath = actual.toExpectedPomPath()
            val actualString = actual.toFile().readText()
            assertEqualsToFile(expectedPomPath, actualString, ::pomSanitizer)
        }
    }

    /**
     * convert:
     * ${mavenLocal}/org/jetbrains/kotlin/artifact/version/artifact-version.pom
     * to:
     * ${expectedRepository}/org/jetbrains/kotlin/artifact/artifact.pom
     */
    private fun Path.toExpectedPomPath(): Path {
        val expectedFileName = this.fileName.toString().replace(versionRegex, "")
        val artifactDirPath = localRepoPath.relativize(this).parent.parent
        return expectedRepoPath.resolve(artifactDirPath.resolve(expectedFileName))
    }

    /**
     * replace a version for each artifact we publish
     * exception for org.jetbrains.kotlin:kotlin-reflect:1.6.10 where it is required for IDEA compatibility
     */
    private fun pomSanitizer(pom: String): String {
        val model = mavenReader.read(StringReader(pom))
        if (model.version != null) model.version = kotlinVersion
        model.dependencies.forEach {
            if (it.groupId == "org.jetbrains.kotlin") {
                if (it.artifactId != "kotlin-reflect" && it.version != "1.6.10") {
                    it.version = kotlinVersion
                }
            }
        }
        if (model.parent != null) {
            model.parent.version = kotlinVersion
        }
        return StringWriter().apply { mavenWriter.write(this, model) }.toString()
    }
}