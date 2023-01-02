/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.runners;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.test.generators.GenerateCompilerTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("compiler/testData/diagnostics/firTestWithJvmBackend")
@TestDataPath("$PROJECT_ROOT")
public class FirDiagnosticsTestWithJvmIrBackendGenerated extends AbstractFirDiagnosticsTestWithJvmIrBackend {
    @Test
    public void testAllFilesPresentInFirTestWithJvmBackend() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("compiler/testData/diagnostics/firTestWithJvmBackend"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("exceptionFromInterpreter_ir.kt")
    public void testExceptionFromInterpreter_ir() throws Exception {
        runTest("compiler/testData/diagnostics/firTestWithJvmBackend/exceptionFromInterpreter_ir.kt");
    }
}
