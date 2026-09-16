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

import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.icon.VelocityIconGroup;
import consulo.apache.velocity.localize.VelocityLocalize;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import consulo.document.util.TextRange;
import consulo.language.psi.PsiElement;
import com.intellij.velocity.psi.VtlElementTypes;
import com.intellij.velocity.psi.VtlMacro;
import com.intellij.velocity.psi.VtlParameterDeclaration;
import com.intellij.velocity.psi.VtlPresentableNamedElement;
import com.intellij.velocity.psi.VtlVariable;
import consulo.language.ast.ASTNode;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.ui.image.Image;

/**
 * @author Alexey Chmutov
 */
public class VtlMacroImpl extends VtlPresentableNamedElement implements VtlDirective, VtlMacro {
    public VtlMacroImpl(@Nonnull ASTNode node) {
        super(node);
    }

    @Override
    public boolean processDeclarations(
        @Nonnull PsiScopeProcessor processor,
        @Nonnull ResolveState state,
        PsiElement lastParent,
        @Nonnull PsiElement place
    ) {
        if (!super.processDeclarations(processor, state, lastParent, place)) {
            return false;
        }
        for (VtlVariable declaration : getParameters()) {
            if (!processor.execute(declaration, state)) {
                return false;
            }
        }
        return true;
    }

    @Nullable
    @Override
    protected PsiElement getNameElement() {
        return findHeaderOfDirective().findChildByType(VtlElementTypes.IDENTIFIER);
    }

    @Nullable
    @RequiredReadAction
    public TextRange getNameElementRange() {
        PsiElement nameElement = getNameElement();
        return nameElement == null ? null : nameElement.getTextRange();
    }

    @Nonnull
    @Override
    public String getPresentableName() {
        return "macro '" + getName() + "'";
    }

    @Nonnull
    @Override
    public VtlParameterDeclaration[] getParameters() {
        return findHeaderOfDirective().findChildrenByClass(VtlParameterDeclaration.class);
    }

    @Override
    public boolean isDeprecated() {
        return false;
    }

    @Override
    @RequiredReadAction
    public int getFoldingStartOffset() {
        return getNode().getTextRange().getStartOffset() + "#macro".length();
    }

    @Override
    @RequiredReadAction
    public int getFoldingEndOffset() {
        return getNode().getTextRange().getEndOffset() - "#end".length();
    }

    @Override
    public boolean needsClosing() {
        return true;
    }

    @Override
    public String getTypeName() {
        return VelocityLocalize.typeNameMacro().get();
    }

    @Override
    public Image getIcon() {
        return VelocityIconGroup.sharp();
    }
}