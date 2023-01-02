/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.generated.cases.annotations;

import com.intellij.testFramework.TestDataPath;
import org.jetbrains.kotlin.test.util.KtTestUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.analysis.api.standalone.fir.test.AnalysisApiFirStandaloneModeTestConfiguratorFactory;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfiguratorFactoryData;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiTestConfigurator;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.TestModuleKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.FrontendKind;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisSessionMode;
import org.jetbrains.kotlin.analysis.test.framework.test.configurators.AnalysisApiMode;
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.annotations.AbstractAnalysisApiAnnotationsOnTypesTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/annotations/annotationsOnTypes")
@TestDataPath("$PROJECT_ROOT")
public class FirStandaloneNormalAnalysisSourceModuleAnalysisApiAnnotationsOnTypesTestGenerated extends AbstractAnalysisApiAnnotationsOnTypesTest {
    @NotNull
    @Override
    public AnalysisApiTestConfigurator getConfigurator() {
        return AnalysisApiFirStandaloneModeTestConfiguratorFactory.INSTANCE.createConfigurator(
            new AnalysisApiTestConfiguratorFactoryData(
                FrontendKind.Fir,
                TestModuleKind.Source,
                AnalysisSessionMode.Normal,
                AnalysisApiMode.Standalone
            )
        );
    }

    @Test
    public void testAllFilesPresentInAnnotationsOnTypes() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/annotations/annotationsOnTypes"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("annotaionOnType.kt")
    public void testAnnotaionOnType() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionOnType.kt");
    }

    @Test
    @TestMetadata("annotaionOnTypeArgument.kt")
    public void testAnnotaionOnTypeArgument() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionOnTypeArgument.kt");
    }

    @Test
    @TestMetadata("annotaionOnTypeArgumentOfTypeArgument.kt")
    public void testAnnotaionOnTypeArgumentOfTypeArgument() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionOnTypeArgumentOfTypeArgument.kt");
    }

    @Test
    @TestMetadata("annotaionWithComplexArgumentOnType.kt")
    public void testAnnotaionWithComplexArgumentOnType() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionWithComplexArgumentOnType.kt");
    }

    @Test
    @TestMetadata("annotaionWithComplexArgumentOnTypeArgument.kt")
    public void testAnnotaionWithComplexArgumentOnTypeArgument() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionWithComplexArgumentOnTypeArgument.kt");
    }

    @Test
    @TestMetadata("annotaionWithLiteralArgumentOnType.kt")
    public void testAnnotaionWithLiteralArgumentOnType() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionWithLiteralArgumentOnType.kt");
    }

    @Test
    @TestMetadata("annotaionWithLiteralArgumentOnTypeArgument.kt")
    public void testAnnotaionWithLiteralArgumentOnTypeArgument() throws Exception {
        runTest("analysis/analysis-api/testData/annotations/annotationsOnTypes/annotaionWithLiteralArgumentOnTypeArgument.kt");
    }
}
