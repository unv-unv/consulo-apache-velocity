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

import com.intellij.java.language.psi.CommonClassNames;
import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiType;
import com.intellij.velocity.psi.PsiUtil;
import com.intellij.velocity.psi.VtlCompositeElementTypes;
import com.intellij.velocity.psi.VtlDirectiveHeader;
import com.intellij.velocity.psi.VtlVariable;
import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiReference;
import consulo.language.psi.resolve.PsiScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.language.psi.search.ReferencesSearch;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @author Alexey Chmutov
 */
public class VtlForeach extends VtlDirectiveImpl {
    private FixedNameReferenceElement velocityCountElement = null;
    private FixedNameReferenceElement velocityHasNextElement = null;

    public VtlForeach(ASTNode node) {
        super(node, "foreach", true);
    }

    @Override
    public boolean processDeclarations(
        @Nonnull PsiScopeProcessor processor,
        @Nonnull ResolveState state,
        consulo.language.psi.PsiElement lastParent,
        @Nonnull PsiElement place
    ) {
        VtlDirectiveHeader header = findHeaderOfDirective();
        consulo.language.psi.PsiElement ancestorOfPlace = place.getParent();
        while (ancestorOfPlace != null && ancestorOfPlace != this) {
            if (ancestorOfPlace == header) {
                return true;
            }
            ancestorOfPlace = ancestorOfPlace.getParent();
        }
        if (lastParent != getVelocityCountElement() && !processor.execute(getVelocityCountElement(), state)) {
            return false;
        }
        if (lastParent != getVelocityHasNextElement() && !processor.execute(getVelocityHasNextElement(), state)) {
            return false;
        }
        PsiElement loopVariable = header.findChildByType(VtlCompositeElementTypes.LOOP_VARIABLE);
        if (loopVariable != null && lastParent != loopVariable && !processor.execute(loopVariable, state)) {
            return false;
        }
        return super.processDeclarations(processor, state, lastParent, place);
    }

    private FixedNameReferenceElement getVelocityCountElement() {
        if (velocityCountElement == null) {
            velocityCountElement = new FixedNameReferenceElement("velocityCount", CommonClassNames.JAVA_LANG_INTEGER);
        }
        return velocityCountElement;
    }

    private FixedNameReferenceElement getVelocityHasNextElement() {
        if (velocityHasNextElement == null) {
            velocityHasNextElement = new FixedNameReferenceElement("velocityHasNext", CommonClassNames.JAVA_LANG_BOOLEAN);
        }
        return velocityHasNextElement;
    }

    public class FixedNameReferenceElement extends RenameableFakePsiElement implements VtlVariable {
        private final String myName;
        private final String myTypeName;

        private FixedNameReferenceElement(@Nonnull String name, @Nonnull String typeName) {
            super(VtlForeach.this.getContainingFile());
            myName = name;
            myTypeName = typeName;
        }

        @Override
        public consulo.language.psi.PsiElement getParent() {
            return VtlForeach.this;
        }

        @Nonnull
        @Override
        public PsiElement getNavigationElement() {
            return VtlForeach.this;
        }

        @Override
        @RequiredReadAction
        public Image getIcon() {
            return IconDescriptorUpdaters.getIcon(this, 0);
        }

        @Nonnull
        @Override
        @RequiredReadAction
        public String getName() {
            return myName;
        }

        @Override
        public String getTypeName() {
            return PsiUtil.getUnqualifiedName(myTypeName);
        }

        @Override
        public PsiElement setName(@Nonnull String s) throws IncorrectOperationException {
            throw new IncorrectOperationException(VelocityLocalize.operationNotAllowed());
        }

        public Collection<PsiReference> findReferences(PsiElement element) {
            return ReferencesSearch.search(element).findAll();
        }

        @Override
        @Nullable
        public PsiType getPsiType() {
            return JavaPsiFacade.getInstance(getProject()).getElementFactory().createTypeByFQClassName(myTypeName, getResolveScope());
        }
    }
}