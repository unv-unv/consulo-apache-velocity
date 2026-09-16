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
package com.intellij.velocity.psi.directives;

import com.intellij.java.language.psi.PsiType;
import com.intellij.velocity.psi.VtlElementTypes;
import com.intellij.velocity.psi.VtlVariable;
import com.intellij.velocity.psi.reference.VtlReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.application.util.CachedValue;
import consulo.application.util.CachedValueProvider;
import consulo.application.util.CachedValuesManager;
import consulo.application.util.NotNullLazyValue;
import consulo.language.ast.ASTNode;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiModificationTracker;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Objects;

/**
 * @author Alexey Chmutov
 */
public abstract class VtlAssignment extends VtlDirectiveImpl {
    private final NotNullLazyValue<CachedValue<AssignedVariable>> myAssignedVariable;

    protected VtlAssignment(ASTNode node, @Nonnull String presentableName, boolean needsClosing) {
        super(node, presentableName, needsClosing);
        myAssignedVariable = new NotNullLazyValue<>() {
            @Nonnull
            @Override
            protected CachedValue<AssignedVariable> compute() {
                return CachedValuesManager.getManager(getProject()).createCachedValue(
                    () -> CachedValueProvider.Result.create(createAssignedVariable(), PsiModificationTracker.MODIFICATION_COUNT),
                    false
                );
            }
        };
    }

    @Nullable
    @RequiredReadAction
    private AssignedVariable createAssignedVariable() {
        VtlReferenceExpression varElement = getAssignedVariableElement();
        if (varElement == null) {
            return null;
        }
        if (getContainingFile().findImplicitVariable(varElement.getReferenceName()) != null) {
            return null;
        }
        return new AssignedVariable();
    }

    @Nullable
    @RequiredReadAction
    public VtlReferenceExpression getAssignedVariableElement() {
        consulo.language.psi.PsiElement element = findChildByType(VtlElementTypes.REFERENCE_EXPRESSION);
        if (element == null) {
            return null;
        }
        VtlReferenceExpression expression = (VtlReferenceExpression) element;
        return expression.hasQualifier() ? null : expression;
    }

    @Nullable
    @RequiredReadAction
    public consulo.language.psi.PsiElement getAssignedMethodCallExpression() {
        return findChildByType(VtlElementTypes.METHOD_CALL_EXPRESSION);
    }

    @Nullable
    public abstract PsiType getAssignedVariableElementType();

    @Nonnull
    @Override
    @RequiredReadAction
    public String getPresentableName() {
        String dirName = super.getPresentableName();
        PsiElement nameElement = findChildByType(VtlElementTypes.REFERENCE_EXPRESSION);
        return nameElement == null ? dirName : dirName + " '" + nameElement.getText() + "'";
    }

    public VtlVariable getAssignedVariable() {
        return myAssignedVariable.getValue().getValue();
    }

    private class AssignedVariable extends RenameableFakePsiElement implements VtlVariable {

        public AssignedVariable() {
            super(VtlAssignment.this.getContainingFile());
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public String getName() {
            VtlReferenceExpression expression = Objects.requireNonNull(getAssignedVariableElement());
            return Objects.requireNonNull(expression.getReferenceName());
        }

        @Override
        @RequiredWriteAction
        public PsiElement setName(@Nonnull String name) throws IncorrectOperationException {
            VtlReferenceExpression nameElement = Objects.requireNonNull(getAssignedVariableElement());
            nameElement.handleElementRename(name);
            return this;
        }

        @Nullable
        @Override
        @RequiredReadAction
        public PsiElement getNavigationElement() {
            VtlReferenceExpression expression = Objects.requireNonNull(getAssignedVariableElement());
            return expression.getReferenceNameElement();
        }

        @Override
        @RequiredReadAction
        public PsiElement getParent() {
            return getAssignedVariableElement();
        }

        @Override
        public String getTypeName() {
            return VelocityLocalize.typeNameVariable().get();
        }

        @Override
        @RequiredReadAction
        public Image getIcon() {
            return IconDescriptorUpdaters.getIcon(this, 0);
        }

        @Override
        public PsiType getPsiType() {
            return getAssignedVariableElementType();
        }

        @Override
        @RequiredReadAction
        public boolean isEquivalentTo(consulo.language.psi.PsiElement another) {
            if (!getClass().isInstance(another)) {
                return false;
            }
            AssignedVariable other = (AssignedVariable) another;
            return getName().equals(other.getName())
                && getContainingFile().equals(other.getContainingFile());
        }

        @Override
        @RequiredReadAction
        public String toString() {
            return "AssignedVariable " + getName();
        }
    }
}