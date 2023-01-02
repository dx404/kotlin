/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

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
@TestMetadata("compiler/testData/codegen/topLevelMemberInvocation")
@TestDataPath("$PROJECT_ROOT")
@RunWith(JUnit3RunnerWithInners.class)
public class TopLevelMembersInvocationTestGenerated extends AbstractTopLevelMembersInvocationTest {
    private void runTest(String testDataFilePath) throws Exception {
        KotlinTestUtils.runTest(this::doTest, this, testDataFilePath);
    }

    public void testAllFilesPresentInTopLevelMemberInvocation() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/codegen/topLevelMemberInvocation"), Pattern.compile("^([^\\.]+)$"), null, false);
    }

    @TestMetadata("extensionFunction")
    public void testExtensionFunction() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/extensionFunction/");
    }

    @TestMetadata("functionDifferentPackage")
    public void testFunctionDifferentPackage() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/functionDifferentPackage/");
    }

    @TestMetadata("functionInMultiFilePackage")
    public void testFunctionInMultiFilePackage() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/functionInMultiFilePackage/");
    }

    @TestMetadata("functionSamePackage")
    public void testFunctionSamePackage() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/functionSamePackage/");
    }

    @TestMetadata("property")
    public void testProperty() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/property/");
    }

    @TestMetadata("propertyWithGetter")
    public void testPropertyWithGetter() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/propertyWithGetter/");
    }

    @TestMetadata("twoModules")
    public void testTwoModules() throws Exception {
        runTest("compiler/testData/codegen/topLevelMemberInvocation/twoModules/");
    }
}
