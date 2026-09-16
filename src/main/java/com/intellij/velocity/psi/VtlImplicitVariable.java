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
package com.intellij.velocity.psi;

import com.intellij.java.language.psi.JavaPsiFacade;
import com.intellij.java.language.psi.PsiType;
import com.intellij.velocity.psi.files.VtlFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.icon.IconDescriptorUpdaters;
import consulo.language.impl.psi.RenameableFakePsiElement;
import consulo.language.psi.PsiComment;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.util.IncorrectOperationException;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;

/**
 * @author Alexey Chmutov
 */
public class VtlImplicitVariable extends RenameableFakePsiElement implements VtlVariable {
    @Nullable
    private final consulo.language.psi.PsiComment myComment;
    private final String myName;
    private String myType;
    private final VtlFile myScopeFile;

    private VtlImplicitVariable(
        @Nonnull consulo.language.psi.PsiFile containingFile,
        @Nullable PsiComment comment,
        @Nonnull String name,
        @Nullable VtlFile scopeFile
    ) {
        super(containingFile);
        myComment = comment;
        myName = name;
        myScopeFile = scopeFile;
    }

    @Nonnull
    @Override
    public String getName() {
        return myName;
    }

    @Nonnull
    @Override
    public PsiElement getNavigationElement() {
        return myComment != null ? myComment : getContainingFile();
    }

    @Override
    public PsiElement getParent() {
        return myComment;
    }

    @Override
    public String getTypeName() {
        return VelocityLocalize.typeNameVariable().get();
    }

    @Override
    public String toString() {
        return "ImplicitVariable " + myName;
    }

    public void setType(String type) {
        myType = type;
    }

    @Nullable
    @Override
    public PsiType getPsiType() {
        if (myType == null) {
            return null;
        }
        try {
            return JavaPsiFacade.getInstance(getProject()).getElementFactory().createTypeFromText(myType, myComment);
        }
        catch (IncorrectOperationException e) {
            return null;
        }
    }

    @Override
    @RequiredReadAction
    public Image getIcon() {
        return IconDescriptorUpdaters.getIcon(this, 0);
    }

    public static VtlImplicitVariable getOrCreate(
        @Nonnull Map<String, VtlImplicitVariable> mapToAddTo,
        @Nonnull PsiFile containingFile,
        @Nullable consulo.language.psi.PsiComment comment,
        String name,
        @Nullable VtlFile scopeFile
    ) {
        assert comment == null || comment.getContainingFile() == containingFile;
        return mapToAddTo.computeIfAbsent(name, s -> new VtlImplicitVariable(containingFile, comment, name, scopeFile));
    }

    public boolean isVisibleIn(@Nullable VtlFile placeFile) {
        return placeFile == null || myScopeFile == null || placeFile.isEquivalentTo(myScopeFile);
    }
}
