/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.velocity.psi.reference;

import com.intellij.java.impl.psi.AbstractQualifiedReference;
import com.intellij.java.impl.psi.impl.beanProperties.BeanProperty;
import com.intellij.java.impl.psi.impl.beanProperties.BeanPropertyElement;
import com.intellij.java.language.psi.*;
import com.intellij.velocity.editorActions.VtlTailType;
import com.intellij.velocity.psi.*;
import com.intellij.velocity.psi.directives.VtlAssignment;
import com.intellij.velocity.psi.directives.VtlMacroCall;
import com.intellij.velocity.psi.files.VtlFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.lookup.LookupElementBuilder;
import consulo.language.editor.completion.lookup.TailTypeDecorator;
import consulo.language.impl.psi.CheckUtil;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ResolveResult;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.util.IncorrectOperationException;
import consulo.localize.LocalizeValue;
import consulo.util.collection.ContainerUtil;
import consulo.util.lang.StringUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import static com.intellij.velocity.psi.PsiUtil.createVtlReferenceExpression;
import static com.intellij.velocity.psi.PsiUtil.getPresentableText;
import static com.intellij.velocity.psi.VtlElementTypes.JAVA_DOT;

/**
 * @author Alexey Chmutov
 */
public class VtlReferenceExpression extends AbstractQualifiedReference<VtlReferenceExpression> implements VtlExpression {
    public VtlReferenceExpression(@Nonnull ASTNode node) {
        super(node);
    }

    @Override
    @RequiredReadAction
    protected boolean processVariantsInner(PsiScopeProcessor processor) {
        VtlReferenceExpression qualifier = getQualifierInner();
        if (qualifier == null) {
            if (getParent() instanceof VtlMacroCall macroCall) {
                VtlFile containingFile = macroCall.getContainingFile();
                return containingFile.processAllMacrosInScope(processor, consulo.language.psi.resolve.ResolveState.initial());
            }
            return processUnqualifiedVariants(processor);
        }
        PsiType type = qualifier.getPsiType();
        if (type instanceof PsiClassType) {
            PsiClass psiClass = com.intellij.java.language.psi.util.PsiUtil.resolveClassInType(type);
            if (psiClass != null && !psiClass.processDeclarations(processor, ResolveState.initial(), null, this)) {
                return false;
            }
        }
        consulo.language.psi.PsiElement psiElement = qualifier.resolve();
        return psiElement == null
            || psiElement.processDeclarations(processor, consulo.language.psi.resolve.ResolveState.initial(), null, this);
    }

    @RequiredReadAction
    public boolean hasQualifier() {
        return getQualifierInner() != null;
    }

    @RequiredReadAction
    public boolean isQualifierResolved() {
        VtlReferenceExpression qualifier = getQualifierInner();
        return qualifier == null || qualifier.resolve() != null;
    }

    @Nullable
    @RequiredReadAction
    private VtlReferenceExpression getQualifierInner() {
        consulo.language.psi.PsiElement child = getFirstChild();
        while (child != null) {
            if (child instanceof VtlReferenceExpression refExpr) {
                return refExpr;
            }
            if (child instanceof VtlMethodCallExpression call) {
                return call.getReferenceExpression();
            }
            child = child.getNextSibling();
        }
        return null;
    }

    @Nullable
    @RequiredReadAction
    public VtlReferenceExpression getParentReferenceExpression() {
        return getParent() instanceof VtlReferenceExpression refExpr ? refExpr : null;
    }

    @Override
    @RequiredReadAction
    protected consulo.language.psi.ResolveResult[] resolveInner() {
        final String referenceName = getReferenceName();
        if (referenceName == null) {
            return consulo.language.psi.ResolveResult.EMPTY_ARRAY;
        }

        final consulo.language.psi.PsiElement parent = getParent();
        if (parent instanceof VtlAssignment assignment) {
            VtlVariable var = assignment.getAssignedVariable();
            if (var != null && assignment.getAssignedVariableElement() == this) {
                return new consulo.language.psi.ResolveResult[]{new consulo.language.psi.PsiElementResolveResult(var)};
            }
        }

        VtlVariantsProcessor<ResolveResult> processor = new VtlVariantsProcessor<>(parent, getContainingFile(), referenceName, false) {
            @Override
            protected consulo.language.psi.ResolveResult execute(consulo.language.psi.PsiNamedElement element, boolean error) {
                if (element instanceof BeanPropertyElement beanPropertyElement) {
                    return new consulo.language.psi.PsiElementResolveResult(beanPropertyElement.getMethod());
                }
                return new consulo.language.psi.PsiElementResolveResult(element, !error);
            }
        };
        processVariantsInner(processor);
        return processor.getVariants(consulo.language.psi.ResolveResult.EMPTY_ARRAY, Character.isLowerCase(referenceName.charAt(0)));
    }

    @Nonnull
    @RequiredReadAction
    public LocalizeValue getUnresolvedMessage(boolean resolvedWithError) {
        String referenceName = getReferenceName();
        consulo.language.psi.PsiElement parent = getParent();
        if (parent instanceof VtlMacroCall) {
            return resolvedWithError
                ? VelocityLocalize.errorWrongNumberOfArgumentsForMacro(referenceName)
                : VelocityLocalize.errorCannotResolveMacro(referenceName);
        }
        VtlReferenceExpression qualifier = getQualifierInner();
        if (qualifier == null) {
            return VelocityLocalize.errorCannotResolveVariable(referenceName);
        }
        String typeName = getPresentableText(qualifier.getPsiType());
        if (!(parent instanceof VtlMethodCallExpression callExpr)) {
            return VelocityLocalize.errorCannotResolveProperty(referenceName, typeName);
        }
        if (!resolvedWithError) {
            return VelocityLocalize.errorCannotResolveMethod(referenceName, typeName);
        }
        String argumentTypes = StringUtil.join(callExpr.getArgumentTypes(), PsiUtil::getPresentableText, ", ");
        return VelocityLocalize.errorNoApplicableMethod(referenceName, typeName, "(" + argumentTypes + ")");
    }

    @Nonnull
    @Override
    @RequiredReadAction
    protected VtlReferenceExpression parseReference(String newText) {
        if (!(getParent() instanceof VtlMethodCallExpression)) {
            String propertyName = VelocityNamingUtil.getPropertyName(newText, isFirstCharInLowerCase());
            if (propertyName != null) {
                newText = propertyName;
            }
        }
        return createVtlReferenceExpression(newText, getProject());
    }

    @Override
    @RequiredReadAction
    protected consulo.language.psi.PsiElement getSeparator() {
        return findChildByType(JAVA_DOT);
    }

    @Override
    @RequiredReadAction
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    @RequiredReadAction
    public consulo.language.psi.PsiElement getReferenceNameElement() {
        return findChildByType(VtlElementTypes.IDENTIFIER);
    }

    @Override
    public boolean isReferenceTo(consulo.language.psi.PsiElement element) {
        consulo.language.psi.PsiManager manager = getManager();
        for (consulo.language.psi.ResolveResult result : multiResolve(false)) {
            consulo.language.psi.PsiElement target = result.getElement();
            if (manager.areElementsEquivalent(element, target)) {
                return true;
            }
            if (target instanceof BeanPropertyElement beanPropertyElement
                && manager.areElementsEquivalent(element, beanPropertyElement.getMethod())) {
                return true;
            }
        }
        return false;
    }

    @Override
    @RequiredReadAction
    public Object[] getVariants() {
        return getVariants(false);
    }

    @RequiredReadAction
    public Object[] getVariants(boolean propertiesOnly) {
        VtlVariantsProcessor<consulo.language.psi.PsiNamedElement> processor =
            new VtlVariantsProcessor<>(getParent(), getContainingFile(), null, propertiesOnly) {
                @Override
                protected consulo.language.psi.PsiNamedElement execute(consulo.language.psi.PsiNamedElement element, boolean error) {
                    return element;
                }
            };
        processVariantsInner(processor);
        consulo.language.psi.PsiNamedElement[] variants = processor.getVariants(
            consulo.language.psi.PsiNamedElement.EMPTY_ARRAY,
            isFirstCharInLowerCase()
        );

        return ContainerUtil.map2Array(
            variants,
            Object.class,
            element -> {
                LookupElementBuilder lookupElement = LookupElementBuilder.create(element);
                if (element instanceof VtlVariable variable) {
                    PsiType type = variable.getPsiType();
                    if (type != null) {
                        lookupElement = lookupElement.withTypeText(type.getPresentableText());
                    }
                }
                else if (element instanceof BeanPropertyElement beanPropertyElement) {
                    PsiType type = beanPropertyElement.getPropertyType();
                    if (type != null) {
                        lookupElement = lookupElement.withTypeText(type.getPresentableText());
                    }
                }
                else if (element instanceof PsiMethod) {
                    return TailTypeDecorator.withTail(lookupElement, VtlTailType.METHOD_CALL_TAIL_TYPE);
                }
                else if (element instanceof VtlMacro) {
                    PsiElement sibling = getPrevSibling();
                    boolean closingBraceNeeded = sibling != null && "{".equals(sibling.getText());
                    return TailTypeDecorator.withTail(lookupElement, new VtlTailType(closingBraceNeeded));
                }
                return lookupElement;
            }
        );
    }

    protected boolean isFirstCharInLowerCase() {
        String referenceName = getReferenceName();
        return referenceName == null || Character.isLowerCase(referenceName.charAt(0));
    }

    @Nonnull
    @RequiredReadAction
    public VtlCallable[] getCallableCandidates() {
        final String referenceName = getReferenceName();
        if (referenceName == null) {
            return VtlCallable.EMPTY_ARRAY;
        }
        VtlVariantsProcessor<VtlCallable> processor = new VtlVariantsProcessor<>(getParent(), getContainingFile(), null, false) {
            @Override
            @RequiredReadAction
            protected VtlCallable execute(consulo.language.psi.PsiNamedElement element, boolean error) {
                if (!referenceName.equals(element.getName())) {
                    return null;
                }
                if (element instanceof VtlMacro macro) {
                    return macro;
                }
                if (element instanceof PsiMethod method) {
                    return new VtlMethod(method);
                }
                return null;
            }
        };
        processVariantsInner(processor);
        return processor.getVariants(VtlCallable.EMPTY_ARRAY, Character.isLowerCase(referenceName.charAt(0)));
    }

    @Nullable
    @Override
    @RequiredReadAction
    public PsiType getPsiType() {
        consulo.language.psi.PsiElement element = resolve();
        if (element instanceof VtlVariable variable) {
            return variable.getPsiType();
        }
        if (element instanceof PsiMethod method) {
            return getSubstitutedType(method, method.getReturnType());
        }
        if (element instanceof BeanProperty beanProperty) {
            return getSubstitutedType(beanProperty.getMethod(), beanProperty.getPropertyType());
        }
        return null;
    }

    @RequiredReadAction
    private PsiType getSubstitutedType(PsiMethod method, PsiType result) {
        if (!(result instanceof PsiClassType resultClassType)) {
            return result;
        }
        //if (!resultClassType.hasParameters()) { disabled due to parameter type in itself returns false from hasParameters()
        //    return result;
        //}
        PsiClassType qualifierClassType = (PsiClassType) getQualifierInner().getPsiType();
        assert qualifierClassType != null;
        PsiSubstitutor substitutor = PsiUtil.getSuperClassSubstitutor(method.getContainingClass(), qualifierClassType);
        return substitutor.substitute(resultClassType);
    }

    @Override
    @RequiredWriteAction
    public consulo.language.psi.PsiElement handleElementRename(String newElementName) throws IncorrectOperationException {
        CheckUtil.checkWritable(this);
        consulo.language.psi.PsiElement newReferenceName = parseReference(newElementName).getReferenceNameElement();
        getNode().replaceChild(getReferenceNameElement().getNode(), newReferenceName.getNode());
        return this;
    }
}

