/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt.cli.test;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.GenerateTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("plugins/kapt4/testData/integration")
@TestDataPath("$PROJECT_ROOT")
public class KaptToolIntegrationTestGenerated extends AbstractKaptToolIntegrationTest {
    @Test
    public void testAllFilesPresentInIntegration() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("plugins/kapt4/testData/integration"), Pattern.compile("^([^\\.]+)$"), null, false);
    }

    @Test
    @TestMetadata("argfile")
    public void testArgfile() throws Exception {
        runTest("plugins/kapt4/testData/integration/argfile/");
    }

    @Test
    @TestMetadata("correctErrorTypesOff")
    public void testCorrectErrorTypesOff() throws Exception {
        runTest("plugins/kapt4/testData/integration/correctErrorTypesOff/");
    }

    @Test
    @TestMetadata("correctErrorTypesOn")
    public void testCorrectErrorTypesOn() throws Exception {
        runTest("plugins/kapt4/testData/integration/correctErrorTypesOn/");
    }

    @Test
    @TestMetadata("defaultPackage")
    public void testDefaultPackage() throws Exception {
        runTest("plugins/kapt4/testData/integration/defaultPackage/");
    }

    @Test
    @TestMetadata("kotlinFileGeneration")
    public void testKotlinFileGeneration() throws Exception {
        runTest("plugins/kapt4/testData/integration/kotlinFileGeneration/");
    }

    @Test
    @TestMetadata("kotlinFileGenerationDefaultOutput")
    public void testKotlinFileGenerationDefaultOutput() throws Exception {
        runTest("plugins/kapt4/testData/integration/kotlinFileGenerationDefaultOutput/");
    }

    @Test
    @TestMetadata("kt33800")
    public void testKt33800() throws Exception {
        runTest("plugins/kapt4/testData/integration/kt33800/");
    }

    @Test
    @TestMetadata("separateStubAptCompilation")
    public void testSeparateStubAptCompilation() throws Exception {
        runTest("plugins/kapt4/testData/integration/separateStubAptCompilation/");
    }

    @Test
    @TestMetadata("simple")
    public void testSimple() throws Exception {
        runTest("plugins/kapt4/testData/integration/simple/");
    }
}
