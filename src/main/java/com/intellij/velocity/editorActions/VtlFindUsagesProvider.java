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
package com.intellij.velocity.editorActions;

import com.intellij.velocity.psi.VtlLanguage;
import com.intellij.velocity.psi.VtlVariable;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.Language;
import consulo.language.findUsage.FindUsagesProvider;
import consulo.language.psi.PsiElement;
import jakarta.annotation.Nonnull;

/**
 * @author Alexey Chmutov
 */
@ExtensionImpl
public class VtlFindUsagesProvider implements FindUsagesProvider {
    @Override
    public boolean canFindUsagesFor(@Nonnull consulo.language.psi.PsiElement psiElement) {
        return psiElement instanceof VtlVariable;
    }

    @Nonnull
    @Override
    public String getType(@Nonnull PsiElement element) {
        return VelocityLocalize.typeNameVariable().get();
    }

    @Nonnull
    @Override
    public String getDescriptiveName(@Nonnull consulo.language.psi.PsiElement element) {
        return VelocityLocalize.typeNameVariable().get();
    }

    @Nonnull
    @Override
    @RequiredReadAction
    public String getNodeText(@Nonnull PsiElement element, boolean useFullName) {
        if (element instanceof VtlVariable variable) {
            return variable.getName();
        }
        return element.getText();
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return VtlLanguage.INSTANCE;
    }
}
