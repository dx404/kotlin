/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.integration;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.JUnit3RunnerWithInners;
import org.jetbrains.kotlin.test.KotlinTestUtils;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/integration/ant/jvm")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class AntTaskTestGenerated extends AbstractAntTaskTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    @TestMetadata("additionalArguments")
    public void testAdditionalArguments() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/additionalArguments/");
    }

    public void testAllFilesPresentInJvm() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/integration/ant/jvm"), Pattern.compile("^([^\\.]+)$"), null, false);
    }

    @TestMetadata("doNotFailOnError")
    public void testDoNotFailOnError() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/doNotFailOnError/");
    }

    @TestMetadata("doNotIncludeRuntimeByDefault")
    public void testDoNotIncludeRuntimeByDefault() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/doNotIncludeRuntimeByDefault/");
    }

    @TestMetadata("failOnErrorByDefault")
    public void testFailOnErrorByDefault() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/failOnErrorByDefault/");
    }

    @TestMetadata("fork")
    public void testFork() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/fork/");
    }

    @TestMetadata("forkOnError")
    public void testForkOnError() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/forkOnError/");
    }

    @TestMetadata("helloWorld")
    public void testHelloWorld() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/helloWorld/");
    }

    @TestMetadata("internalMembers")
    public void testInternalMembers() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/internalMembers/");
    }

    @TestMetadata("jvmClasspath")
    public void testJvmClasspath() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/jvmClasspath/");
    }

    @TestMetadata("kt11995")
    public void testKt11995() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/kt11995/");
    }

    @TestMetadata("languageVersion")
    public void testLanguageVersion() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/languageVersion/");
    }

    @TestMetadata("mainInFiles")
    public void testMainInFiles() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/mainInFiles/");
    }

    @TestMetadata("manySourceRoots")
    public void testManySourceRoots() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/manySourceRoots/");
    }

    @TestMetadata("moduleName")
    public void testModuleName() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/moduleName/");
    }

    @TestMetadata("moduleNameDefault")
    public void testModuleNameDefault() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/moduleNameDefault/");
    }

    @TestMetadata("moduleNameWithKotlin")
    public void testModuleNameWithKotlin() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/moduleNameWithKotlin/");
    }

    @TestMetadata("noReflectForJavac")
    public void testNoReflectForJavac() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/noReflectForJavac/");
    }

    @TestMetadata("noStdlibForJavac")
    public void testNoStdlibForJavac() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/noStdlibForJavac/");
    }

    @TestMetadata("overloadResolutionOnCollectionLiteral")
    public void testOverloadResolutionOnCollectionLiteral() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/overloadResolutionOnCollectionLiteral/");
    }

    @TestMetadata("stdlibForJavacWithNoKotlin")
    public void testStdlibForJavacWithNoKotlin() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/stdlibForJavacWithNoKotlin/");
    }

    @TestMetadata("suppressWarnings")
    public void testSuppressWarnings() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/suppressWarnings/");
    }

    @TestMetadata("twoStdlibForCollectionLiterals")
    public void testTwoStdlibForCollectionLiterals() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/twoStdlibForCollectionLiterals/");
    }

    @TestMetadata("valWithInvoke")
    public void testValWithInvoke() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/valWithInvoke/");
    }

    @TestMetadata("verbose")
    public void testVerbose() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/verbose/");
    }

    @TestMetadata("version")
    public void testVersion() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/version/");
    }

    @TestMetadata("withKotlinFork")
    public void testWithKotlinFork() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/withKotlinFork/");
    }

    @TestMetadata("withKotlinNoJavaSources")
    public void testWithKotlinNoJavaSources() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/withKotlinNoJavaSources/");
    }

    @TestMetadata("wrongCallForCollectionLiteral")
    public void testWrongCallForCollectionLiteral() throws Exception {
        runTest("compiler/testData/integration/ant/jvm/wrongCallForCollectionLiteral/");
    }
}
