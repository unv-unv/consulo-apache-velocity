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
package com.intellij.velocity.inspections;

import com.intellij.velocity.psi.files.VtlFile;
import com.intellij.velocity.psi.reference.VtlReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.language.psi.ResolveResult;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import static consulo.language.editor.inspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
import static consulo.language.editor.inspection.ProblemHighlightType.LIKE_UNKNOWN_SYMBOL;

/**
 * @author Alexey Chmutov
 */
@ExtensionImpl
public class VtlReferencesInspection extends VtlInspectionBase {
    @Override
    @RequiredReadAction
    protected void registerProblems(PsiElement element, ProblemsHolder holder) {
        if (!(element instanceof VtlReferenceExpression ref)) {
            return;
        }
        if (!ref.isQualifierResolved()) {
            return;
        }
        if (ref.getContainingFile() instanceof VtlFile vtlFile && vtlFile.isIdeTemplateFile()) {
            return;
        }
        ResolveResult[] results = ref.multiResolve(false);
        boolean resolvedWithError = results.length > 0 && !results[0].isValidResult();

        if (resolvedWithError || ref.resolve() == null) {
            holder.newProblem(ref.getUnresolvedMessage(resolvedWithError))
                .range(ref)
                .highlightType(resolvedWithError ? GENERIC_ERROR_OR_WARNING : LIKE_UNKNOWN_SYMBOL)
                .create();
        }
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return VelocityLocalize.vtlReferencesInspection();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "VtlReferencesInspection";
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return VelocityLocalize.inspectiondescriptionsVtlfilereferencesinspection();
    }
}