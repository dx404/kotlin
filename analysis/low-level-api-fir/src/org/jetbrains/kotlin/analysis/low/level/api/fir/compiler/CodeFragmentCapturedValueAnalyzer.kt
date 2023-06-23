/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.compiler

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.LLFirResolveSession
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.resolveToFirSymbol
import org.jetbrains.kotlin.analysis.low.level.api.fir.util.parentsCodeFragmentAware
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirResolvable
import org.jetbrains.kotlin.fir.expressions.FirVariableAssignment
import org.jetbrains.kotlin.fir.expressions.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.labelName
import org.jetbrains.kotlin.fir.references.FirSuperReference
import org.jetbrains.kotlin.fir.references.FirThisReference
import org.jetbrains.kotlin.fir.references.toResolvedCallableSymbol
import org.jetbrains.kotlin.fir.resolve.constructFunctionType
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.builder.buildResolvedTypeRef
import org.jetbrains.kotlin.fir.types.specialFunctionTypeKind
import org.jetbrains.kotlin.fir.visitors.FirDefaultVisitorVoid
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.psi.*
import java.util.*
import kotlin.collections.LinkedHashMap

class CodeFragmentCapturedSymbol(
    val value: CodeFragmentCapturedValue,
    val symbol: FirBasedSymbol<*>,
    val typeRef: FirTypeRef
)

internal object CodeFragmentCapturedValueAnalyzer {
    fun analyze(resolveSession: LLFirResolveSession, codeFragment: FirCodeFragment): List<CodeFragmentCapturedSymbol> {
        val selfSymbols = CodeFragmentDeclarationCollector().apply { codeFragment.accept(this) }.symbols.toSet()
        return CodeFragmentCapturedValueVisitor(resolveSession, selfSymbols).apply { codeFragment.accept(this) }.values
    }
}

private class CodeFragmentDeclarationCollector : FirDefaultVisitorVoid() {
    private val collectedSymbols = mutableListOf<FirBasedSymbol<*>>()

    val symbols: List<FirBasedSymbol<*>>
        get() = Collections.unmodifiableList(collectedSymbols)

    override fun visitElement(element: FirElement) {
        if (element is FirDeclaration) {
            collectedSymbols += element.symbol
        }

        element.acceptChildren(this)
    }
}

private class CodeFragmentCapturedValueVisitor(
    private val resolveSession: LLFirResolveSession,
    private val selfSymbols: Set<FirBasedSymbol<*>>,
) : FirDefaultVisitorVoid() {
    private val mappings = LinkedHashMap<FirBasedSymbol<*>, CodeFragmentCapturedSymbol>()
    private val assignmentLhs = mutableListOf<FirBasedSymbol<*>>()

    val values: List<CodeFragmentCapturedSymbol>
        get() = mappings.values.toList()

    private val session: FirSession
        get() = resolveSession.useSiteFirSession

    override fun visitElement(element: FirElement) {
        processElement(element)

        val lhs = (element as? FirVariableAssignment)?.lValue?.toResolvedCallableSymbol()
        if (lhs != null) {
            assignmentLhs.add(lhs)
        }

        element.acceptChildren(this)

        if (lhs != null) {
            require(assignmentLhs.removeLast() == lhs)
        }
    }

    private fun processElement(element: FirElement) {
        when (element) {
            is FirSuperReference -> {
                val symbol = (element.superTypeRef as? FirResolvedTypeRef)?.toRegularClassSymbol(session)
                if (symbol != null && symbol !in selfSymbols) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = CodeFragmentCapturedValue.SuperClass(symbol.classId, isCrossingInlineBounds)
                    register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, element.superTypeRef))
                }
            }
            is FirThisReference -> {
                val symbol = element.boundSymbol
                if (symbol != null && symbol !in selfSymbols) {
                    when (symbol) {
                        is FirRegularClassSymbol -> {
                            if (symbol.classKind != ClassKind.OBJECT) {
                                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                val capturedValue = CodeFragmentCapturedValue.ContainingClass(symbol.classId, isCrossingInlineBounds)
                                val typeRef = buildResolvedTypeRef { type = symbol.defaultType() }
                                register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, typeRef))
                            }
                        }
                        is FirFunctionSymbol<*> -> {
                            if (element.contextReceiverNumber >= 0) {
                                val contextReceiver = symbol.resolvedContextReceivers[element.contextReceiverNumber]
                                val labelName = contextReceiver.labelName
                                if (labelName != null) {
                                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                    val capturedValue = CodeFragmentCapturedValue.ContextReceiver(labelName, isCrossingInlineBounds)
                                    register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, contextReceiver.typeRef))
                                }
                            } else {
                                val labelName = element.labelName ?: (symbol as? FirAnonymousFunctionSymbol)?.label?.name
                                val typeRef = symbol.receiverParameter?.typeRef ?: error("Receiver parameter not found")
                                if (labelName != null) {
                                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                                    val capturedValue = CodeFragmentCapturedValue.ExtensionReceiver(labelName, isCrossingInlineBounds)
                                    register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, typeRef))
                                }
                            }
                        }
                    }
                }
            }
            is FirResolvable -> {
                val symbol = element.calleeReference.toResolvedCallableSymbol()
                if (symbol != null && symbol !in selfSymbols) {
                    processCall(element, symbol)
                }
            }
        }
    }

    private fun processCall(element: FirElement, symbol: FirCallableSymbol<*>) {
        when (symbol) {
            is FirValueParameterSymbol -> {
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.Local(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirPropertySymbol -> {
                if (symbol.isLocal) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = when {
                        symbol.hasDelegate -> CodeFragmentCapturedValue.LocalDelegate(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                        else -> CodeFragmentCapturedValue.Local(symbol.name, symbol.isMutated, isCrossingInlineBounds)
                    }
                    register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
                }
            }
            is FirBackingFieldSymbol -> {
                val propertyName = symbol.propertySymbol.name
                val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                val capturedValue = CodeFragmentCapturedValue.BackingField(propertyName, symbol.isMutated, isCrossingInlineBounds)
                register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
            }
            is FirNamedFunctionSymbol -> {
                if (symbol.isLocal) {
                    val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
                    val capturedValue = CodeFragmentCapturedValue.LocalFunction(symbol.name, isCrossingInlineBounds)
                    val firFunction = symbol.fir
                    val functionType = firFunction.constructFunctionType(firFunction.specialFunctionTypeKind(session))
                    val typeRef = buildResolvedTypeRef { type = functionType }
                    register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, typeRef))
                }
            }
        }

        if (symbol.callableId == StandardClassIds.Callables.coroutineContext) {
            val isCrossingInlineBounds = isCrossingInlineBounds(element, symbol)
            val capturedValue = CodeFragmentCapturedValue.CoroutineContext(isCrossingInlineBounds)
            register(symbol, CodeFragmentCapturedSymbol(capturedValue, symbol, symbol.resolvedReturnTypeRef))
        }
    }

    private fun register(symbol: FirBasedSymbol<*>, mapping: CodeFragmentCapturedSymbol) {
        val previousMapping = mappings[symbol]

        if (previousMapping != null) {
            val previousValue = previousMapping.value
            val newValue = mapping.value

            require(previousValue.javaClass == newValue.javaClass)

            // Only replace non-mutated value with a mutated one.
            if (previousValue.isMutated || !newValue.isMutated) {
                return
            }
        }

        mappings[symbol] = mapping
    }

    private fun isCrossingInlineBounds(element: FirElement, symbol: FirBasedSymbol<*>): Boolean {
        val callSite = element.source?.psi ?: return false
        val declarationSite = symbol.fir.source?.psi ?: return false
        val commonParent = findCommonParentContextAware(callSite, declarationSite) ?: return false

        for (elementInBetween in callSite.parentsCodeFragmentAware) {
            if (elementInBetween === commonParent) {
                break
            }

            if (elementInBetween is KtFunction) {
                val symbolInBetween = elementInBetween.resolveToFirSymbol(resolveSession)
                if (symbolInBetween is FirCallableSymbol<*> && !symbolInBetween.isInline) {
                    return true
                }
            }
        }

        return false
    }

    private fun findCommonParentContextAware(callSite: PsiElement, declarationSite: PsiElement): PsiElement? {
        val directParent = PsiTreeUtil.findCommonParent(callSite, declarationSite)
        if (directParent != null) {
            return directParent
        }

        val codeFragment = callSite.containingFile as? KtCodeFragment ?: return null
        val codeFragmentContext = codeFragment.context ?: return null
        return PsiTreeUtil.findCommonParent(codeFragmentContext, declarationSite)
    }

    private val FirBasedSymbol<*>.isMutated: Boolean
        get() = assignmentLhs.lastOrNull() == this
}