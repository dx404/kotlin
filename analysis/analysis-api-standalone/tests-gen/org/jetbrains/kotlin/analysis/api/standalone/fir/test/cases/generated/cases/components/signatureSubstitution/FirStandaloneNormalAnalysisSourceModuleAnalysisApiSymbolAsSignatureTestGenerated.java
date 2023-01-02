/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.standalone.fir.test.cases.generated.cases.components.signatureSubstitution;

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
import org.jetbrains.kotlin.analysis.api.impl.base.test.cases.components.signatureSubstitution.AbstractAnalysisApiSymbolAsSignatureTest;
import org.jetbrains.kotlin.test.TestMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

/** This class is generated by {@link org.jetbrains.kotlin.generators.tests.analysis.api.GenerateAnalysisApiTestsKt}. DO NOT MODIFY MANUALLY */
@SuppressWarnings("all")
@TestMetadata("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature")
@TestDataPath("$PROJECT_ROOT")
public class FirStandaloneNormalAnalysisSourceModuleAnalysisApiSymbolAsSignatureTestGenerated extends AbstractAnalysisApiSymbolAsSignatureTest {
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
    public void testAllFilesPresentInSymbolAsSignature() throws Exception {
        KtTestUtil.assertAllTestsPresentByMetadataWithExcluded(this.getClass(), new File("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature"), Pattern.compile("^(.+)\\.kt$"), null, true);
    }

    @Test
    @TestMetadata("function.kt")
    public void testFunction() throws Exception {
        runTest("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature/function.kt");
    }

    @Test
    @TestMetadata("propertyGetter.kt")
    public void testPropertyGetter() throws Exception {
        runTest("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature/propertyGetter.kt");
    }

    @Test
    @TestMetadata("propertyNoAccessors.kt")
    public void testPropertyNoAccessors() throws Exception {
        runTest("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature/propertyNoAccessors.kt");
    }

    @Test
    @TestMetadata("propertySetter.kt")
    public void testPropertySetter() throws Exception {
        runTest("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature/propertySetter.kt");
    }

    @Test
    @TestMetadata("propertyWithAccessors.kt")
    public void testPropertyWithAccessors() throws Exception {
        runTest("analysis/analysis-api/testData/components/signatureSubstitution/symbolAsSignature/propertyWithAccessors.kt");
    }
}
