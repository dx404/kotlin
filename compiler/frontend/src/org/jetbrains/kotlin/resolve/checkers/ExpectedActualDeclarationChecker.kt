/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.checkers

import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.AnalysisFlags
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.ExpectActualTracker
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.descriptorUtil.isAnnotationConstructor
import org.jetbrains.kotlin.resolve.descriptorUtil.isPrimaryConstructorOfInlineClass
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.multiplatform.*
import org.jetbrains.kotlin.resolve.multiplatform.ExpectActualCompatibility.*
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement
import org.jetbrains.kotlin.resolve.source.PsiSourceFile
import org.jetbrains.kotlin.types.KotlinType
import java.io.File

class ExpectedActualDeclarationChecker(
    val moduleStructureOracle: ModuleStructureOracle,
    val argumentExtractors: Iterable<ActualAnnotationArgumentExtractor>
) : DeclarationChecker {
    interface ActualAnnotationArgumentExtractor {
        fun extractDefaultValue(parameter: ValueParameterDescriptor, expectedType: KotlinType): ConstantValue<*>?
    }

    override fun check(declaration: KtDeclaration, descriptor: DeclarationDescriptor, context: DeclarationCheckerContext) {
        if (!context.languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return
        // TODO: we need a klib based MPP aware ModuleStructureOracle. Just disable the checks for now.
        if (context.languageVersionSettings.getFlag(AnalysisFlags.expectActualLinker)) return

        // Note that this check is necessary, because for default accessors KtProperty is passed for KtDeclaration, so this
        // case won't be covered by the next check (also, it accidentally fixes KT-28385)
        if (descriptor is PropertyAccessorDescriptor) return
        if (declaration !is KtNamedDeclaration) return
        if (descriptor !is MemberDescriptor || DescriptorUtils.isEnumEntry(descriptor)) return

        val checkActualModifier = !context.languageVersionSettings.getFlag(AnalysisFlags.multiPlatformDoNotCheckActual)

        if (descriptor.isExpect) {
            checkExpectedDeclarationHasProperActuals(
                declaration, descriptor, context.trace,
                checkActualModifier, context.expectActualTracker
            )
        }
        if (descriptor.isActualOrSomeContainerIsActual()) {
            val allDependsOnModules = moduleStructureOracle.findAllDependsOnPaths(descriptor.module).flatMap { it.nodes }.toHashSet()
            checkActualDeclarationHasExpected(
                declaration,
                descriptor,
                checkActualModifier,
                context.trace,
                moduleVisibilityFilter = { it in allDependsOnModules }
            )
        }
    }

    private fun MemberDescriptor.isActualOrSomeContainerIsActual(): Boolean {
        var declaration: MemberDescriptor = this
        while (true) {
            if (declaration.isActual) return true
            declaration = declaration.containingDeclaration as? MemberDescriptor ?: return false
        }
    }

    private fun checkExpectedDeclarationHasProperActuals(
        expectPsi: KtNamedDeclaration,
        expect: MemberDescriptor,
        trace: BindingTrace,
        checkActualModifier: Boolean,
        expectActualTracker: ExpectActualTracker
    ) {
        val allActualizationPaths = moduleStructureOracle.findAllReversedDependsOnPaths(expect.module)
        val allLeafModules = allActualizationPaths.map { it.nodes.last() }.toSet()

        allLeafModules.forEach { leafModule ->
            val actuals = ExpectedActualResolver.findActualForExpected(expect, leafModule) ?: return@forEach

            checkExpectedDeclarationHasAtLeastOneActual(
                expectPsi, expect, actuals, trace, leafModule, checkActualModifier, expectActualTracker
            )

            checkExpectedDeclarationHasAtMostOneActual(
                expectPsi, expect, actuals, allActualizationPaths, trace
            )
        }
    }

    private fun checkExpectedDeclarationHasAtMostOneActual(
        expectPsi: KtNamedDeclaration,
        expect: MemberDescriptor,
        actuals: ActualsMap,
        modulePaths: List<ModulePath>,
        trace: BindingTrace,
    ) {
        val atLeastWeaklyCompatibleActuals = actuals
            .filterKeys { compatibility -> compatibility.isCompatibleOrWeakCompatible() }
            .values.flatten()

        // Eagerly return here: We won't find a duplicate in any module path in this case
        if (atLeastWeaklyCompatibleActuals.size <= 1) return

        /*
        Note that we have to check for 'duplicate actuals' separately, considering paths
        instead of only leaf-module, because currently we have to distinguish the following
        two cases:

               expect class A                      expect class A
                /            \                       /           \
               /              \                     /             \
         actual class A     actual class A   actual class A    (no actual)
               \              /                     \             /
                \            /                       \           /
           (warning or nothing, TBD)                 actual class A
                                                  (ERROR: duplicate actuals)

        If we merge behaviour (e.g. decide to report ERROR for first case too)
        for those two cases, we can drop separate logic for DUPLICATE_ACTUALS
        */
        val actualsByModulePath = modulePaths.associateWith { path ->
            atLeastWeaklyCompatibleActuals.filter { it.module in path.nodes }
        }

        actualsByModulePath.forEach { (_, actualsInPath) ->
            if (actualsInPath.size > 1) {
                trace.report(Errors.AMBIGUOUS_ACTUALS.on(
                    expectPsi,
                    expect,
                    actualsInPath
                        .map { it.module }
                        .sortedBy { it.name.asString() }
                ))
            }
        }
    }

    private fun checkExpectedDeclarationHasAtLeastOneActual(
        expectPsi: KtNamedDeclaration,
        expect: MemberDescriptor,
        actuals: ActualsMap,
        trace: BindingTrace,
        module: ModuleDescriptor,
        checkActualModifier: Boolean,
        expectActualTracker: ExpectActualTracker
    ) {
        // Only look for top level actual members; class members will be handled as a part of that expected class
        if (expect.containingDeclaration !is PackageFragmentDescriptor) return

        // Only strong incompatibilities, but this is an OptionalExpectation -- don't report it
        if (actuals.allStrongIncompatibilities() && OptionalAnnotationUtil.isOptionalAnnotationClass(expect)) return

        // Only strong incompatibilities, or error won't be reported on actual: report NO_ACTUAL_FOR_EXPECT here
        if (actuals.allStrongIncompatibilities() ||
            Compatible !in actuals && expect.hasNoActualWithDiagnostic(actuals)
        ) {
            assert(actuals.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = actuals as Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>
            trace.report(Errors.NO_ACTUAL_FOR_EXPECT.on(expectPsi, expect, module, incompatibility))
            return
        }

        // Here we have exactly one compatible actual and/or some weakly incompatible. In either case, we don't report anything on expect...
        val actualMembers = actuals.asSequence()
            .filter { it.key.isCompatibleOrWeakCompatible() }.flatMap { it.value.asSequence() }

        // ...except diagnostics regarding missing actual keyword, because in that case we won't start looking for the actual at all
        if (checkActualModifier) {
            actualMembers.forEach { reportMissingActualModifier(it, actualPsi = null, trace) }
        }

        expectActualTracker.reportExpectActual(expected = expect, actualMembers = actualMembers)
    }

    private fun reportMissingActualModifier(actual: MemberDescriptor, actualPsi: KtNamedDeclaration?, trace: BindingTrace) {
        if (actual.isActual) return
        @Suppress("NAME_SHADOWING")
        val actualPsi = actualPsi ?: (actual.source as? KotlinSourceElement)?.psi as? KtNamedDeclaration ?: return

        if (requireActualModifier(actual)) {
            trace.report(Errors.ACTUAL_MISSING.on(actualPsi))
        }
    }

    private fun checkIfExpectHasDefaultArgumentsAndActualizedWithTypealias(
        expect: MemberDescriptor,
        actualDeclaration: KtNamedDeclaration,
        trace: BindingTrace,
    ) {
        if (expect !is ClassDescriptor ||
            actualDeclaration !is KtTypeAlias ||
            expect.kind == ClassKind.ANNOTATION_CLASS
        ) return

        val members = expect.constructors + expect.unsubstitutedMemberScope
            .getContributedDescriptors(DescriptorKindFilter.FUNCTIONS)
            .filterIsInstance<FunctionDescriptor>()

        val membersWithDefaultValueParameters = members
            .filter { it.valueParameters.any { p -> p.declaresDefaultValue() }}

        if (membersWithDefaultValueParameters.isEmpty()) return

        trace.report(
            Errors.DEFAULT_ARGUMENTS_IN_EXPECT_WITH_ACTUAL_TYPEALIAS.on(
                actualDeclaration,
                expect,
                membersWithDefaultValueParameters
            )
        )
    }

    private fun MemberDescriptor.hasNoActualWithDiagnostic(
        compatibility: Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>
    ): Boolean {
        return compatibility.values.flatMapTo(hashSetOf()) { it }.all { actual ->
            val expectedOnes = ExpectedActualResolver.findExpectedForActual(actual, onlyFromThisModule(module))
            expectedOnes != null && Compatible in expectedOnes.keys
        }
    }

    private fun ExpectActualTracker.reportExpectActual(expected: MemberDescriptor, actualMembers: Sequence<MemberDescriptor>) {
        if (this is ExpectActualTracker.DoNothing) return

        val expectedFile = sourceFile(expected) ?: return
        for (actual in actualMembers) {
            val actualFile = sourceFile(actual) ?: continue
            report(expectedFile = expectedFile, actualFile = actualFile)
        }
    }

    private fun sourceFile(descriptor: MemberDescriptor): File? {
        val containingFile = descriptor.source.containingFile as? PsiSourceFile ?: return null
        return VfsUtilCore.virtualToIoFile(containingFile.psiFile.virtualFile)
    }

    private fun checkActualDeclarationHasExpected(
        actualPsi: KtNamedDeclaration,
        actual: MemberDescriptor,
        checkActualModifier: Boolean,
        trace: BindingTrace,
        moduleVisibilityFilter: ModuleFilter
    ) {
        val compatibility = ExpectedActualResolver.findExpectedForActual(actual, moduleVisibilityFilter)
            ?: return

        checkAmbiguousExpects(compatibility, trace, actualPsi, actual)

        // For top-level declaration missing actual error reported in Actual checker
        if (checkActualModifier
            && actual.containingDeclaration !is PackageFragmentDescriptor
            && compatibility.any { it.key.isCompatibleOrWeakCompatible() }
        ) {
            reportMissingActualModifier(actual, actualPsi, trace)
        }

        // Usually, actualPsi.hasActualModifier() and descriptor.isActual are the same.
        // The only one case where it isn't true is constructor of annotation class. In that case descriptor.isActual is true.
        // See the FunctionDescriptorResolver.createConstructorDescriptor
        // But in that case compatibility.allStrongIncompatibilities() == true means that in the expect class there is no constructor
        if (!actualPsi.hasActualModifier() && compatibility.allStrongIncompatibilities()) return

        // 'firstOrNull' is needed because in diagnostic tests, common sources appear twice, so the same class is duplicated
        // TODO: replace with 'singleOrNull' as soon as multi-module diagnostic tests are refactored
        val singleIncompatibility = compatibility.keys.firstOrNull()
        if (singleIncompatibility is Incompatible.ClassScopes) {
            assert(actual is ClassDescriptor || actual is TypeAliasDescriptor) {
                "Incompatible.ClassScopes is only possible for a class or a typealias: $actual"
            }

            // Do not report "expected members have no actual ones" for those expected members, for which there's a clear
            // (albeit maybe incompatible) single actual suspect, declared in the actual class.
            // This is needed only to reduce the number of errors. Incompatibility errors for those members will be reported
            // later when this checker is called for them
            fun hasSingleActualSuspect(
                expectedWithIncompatibility: Pair<MemberDescriptor, Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>>
            ): Boolean {
                val (expectedMember, incompatibility) = expectedWithIncompatibility
                val actualMember = incompatibility.values.singleOrNull()?.singleOrNull()
                return actualMember != null &&
                        actualMember.isExplicitActualDeclaration() &&
                        !incompatibility.allStrongIncompatibilities() &&
                        ExpectedActualResolver.findExpectedForActual(
                            actualMember, onlyFromThisModule(expectedMember.module)
                        )?.values?.singleOrNull()?.singleOrNull() == expectedMember
            }

            val nonTrivialUnfulfilled = singleIncompatibility.unfulfilled.filterNot(::hasSingleActualSuspect)

            if (nonTrivialUnfulfilled.isNotEmpty()) {
                val classDescriptor =
                    (actual as? TypeAliasDescriptor)?.expandedType?.constructor?.declarationDescriptor as? ClassDescriptor
                        ?: (actual as ClassDescriptor)
                trace.report(
                    Errors.NO_ACTUAL_CLASS_MEMBER_FOR_EXPECTED_CLASS.on(
                        actualPsi, classDescriptor, nonTrivialUnfulfilled
                    )
                )
            }
        } else if (Compatible !in compatibility) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible<MemberDescriptor>, Collection<MemberDescriptor>>
            trace.report(Errors.ACTUAL_WITHOUT_EXPECT.on(actualPsi, actual, incompatibility))
        } else {
            val expected = compatibility[Compatible]!!.first()
            if (expected is ClassDescriptor && expected.kind == ClassKind.ANNOTATION_CLASS) {
                val actualConstructor =
                    (actual as? ClassDescriptor)?.constructors?.singleOrNull()
                        ?: (actual as? TypeAliasDescriptor)?.constructors?.singleOrNull()?.underlyingConstructorDescriptor
                val expectedConstructor = expected.constructors.singleOrNull()
                if (expectedConstructor != null && actualConstructor != null) {
                    checkAnnotationConstructors(expectedConstructor, actualConstructor, trace, actualPsi)
                }
            }
        }
        val expectSingleCandidate = compatibility.values.singleOrNull()?.firstOrNull()
        if (expectSingleCandidate != null) {
            checkIfExpectHasDefaultArgumentsAndActualizedWithTypealias(expectSingleCandidate, actualPsi, trace)
        }
    }

    private fun checkAmbiguousExpects(
        compatibility: Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>,
        trace: BindingTrace,
        actualPsi: KtNamedDeclaration,
        actual: MemberDescriptor
    ) {
        val filesWithAtLeastWeaklyCompatibleExpects = compatibility.asSequence()
            .filter { (compatibility, _) ->
                compatibility.isCompatibleOrWeakCompatible()
            }
            .map { (_, members) -> members }
            .flatten()
            .map { it.module }
            .sortedBy { it.name.asString() }
            .toList()

        if (filesWithAtLeastWeaklyCompatibleExpects.size > 1) {
            trace.report(Errors.AMBIGUOUS_EXPECTS.on(actualPsi, actual, filesWithAtLeastWeaklyCompatibleExpects))
        }
    }

    // we don't require `actual` modifier on
    //  - annotation constructors, because annotation classes can only have one constructor
    //  - inline class primary constructors, because inline class must have primary constructor
    //  - value parameter inside primary constructor of inline class, because inline class must have one value parameter
    private fun requireActualModifier(actual: MemberDescriptor): Boolean {
        return !actual.isAnnotationConstructor() &&
                !actual.isPrimaryConstructorOfInlineClass() &&
                !isUnderlyingPropertyOfInlineClass(actual)
    }

    private fun isUnderlyingPropertyOfInlineClass(actual: MemberDescriptor): Boolean {
        return actual is PropertyDescriptor && actual.isUnderlyingPropertyOfInlineClass()
    }

    // This should ideally be handled by CallableMemberDescriptor.Kind, but default constructors have kind DECLARATION and non-empty source.
    // Their source is the containing KtClass instance though, as opposed to explicit constructors, whose source is KtConstructor
    private fun MemberDescriptor.isExplicitActualDeclaration(): Boolean =
        when (this) {
            is ConstructorDescriptor -> DescriptorToSourceUtils.getSourceFromDescriptor(this) is KtConstructor<*>
            is CallableMemberDescriptor -> kind == CallableMemberDescriptor.Kind.DECLARATION
            else -> true
        }

    private fun checkAnnotationConstructors(
        expected: ConstructorDescriptor, actual: ConstructorDescriptor, trace: BindingTrace, actualPsi: PsiElement
    ) {
        for (expectedParameterDescriptor in expected.valueParameters) {
            // Actual parameter with the same name is guaranteed to exist because this method is only called for compatible annotations
            val actualParameterDescriptor = actual.valueParameters.first { it.name == expectedParameterDescriptor.name }

            if (expectedParameterDescriptor.declaresDefaultValue() && actualParameterDescriptor.declaresDefaultValue()) {
                val expectedParameter =
                    DescriptorToSourceUtils.descriptorToDeclaration(expectedParameterDescriptor) as? KtParameter ?: continue

                val expectedValue = trace.bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expectedParameter.defaultValue)
                    ?.toConstantValue(expectedParameterDescriptor.type)

                val actualValue =
                    getActualAnnotationParameterValue(actualParameterDescriptor, trace.bindingContext, expectedParameterDescriptor.type)
                if (expectedValue != actualValue) {
                    val ktParameter = DescriptorToSourceUtils.descriptorToDeclaration(actualParameterDescriptor)
                    val target = (ktParameter as? KtParameter)?.defaultValue ?: (actualPsi as? KtTypeAlias)?.nameIdentifier ?: actualPsi
                    trace.report(Errors.ACTUAL_ANNOTATION_CONFLICTING_DEFAULT_ARGUMENT_VALUE.on(target, actualParameterDescriptor))
                }
            }
        }
    }

    private fun getActualAnnotationParameterValue(
        actualParameter: ValueParameterDescriptor, bindingContext: BindingContext, expectedType: KotlinType
    ): ConstantValue<*>? {
        val declaration = DescriptorToSourceUtils.descriptorToDeclaration(actualParameter)
        if (declaration is KtParameter) {
            return bindingContext.get(BindingContext.COMPILE_TIME_VALUE, declaration.defaultValue)?.toConstantValue(expectedType)
        }

        for (extractor in argumentExtractors) {
            extractor.extractDefaultValue(actualParameter, expectedType)?.let { return it }
        }

        return null
    }

    companion object {
        fun Map<out ExpectActualCompatibility<MemberDescriptor>, Collection<MemberDescriptor>>.allStrongIncompatibilities(): Boolean =
            this.keys.all { it is Incompatible && it.kind == IncompatibilityKind.STRONG }

        internal fun ExpectActualCompatibility<MemberDescriptor>.isCompatibleOrWeakCompatible() =
            this is Compatible ||
                    this is Incompatible && kind == IncompatibilityKind.WEAK
    }
}

private typealias ActualsMap = Map<ExpectActualCompatibility<MemberDescriptor>, List<MemberDescriptor>>
