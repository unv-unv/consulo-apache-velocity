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

import com.intellij.java.language.psi.PsiType;
import com.intellij.velocity.psi.directives.VtlSet;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import static consulo.language.editor.inspection.ProblemHighlightType.LIKE_UNUSED_SYMBOL;

@ExtensionImpl
public class VtlDirectiveArgsInspection extends VtlInspectionBase {
    @Override
    @RequiredReadAction
    protected void registerProblems(PsiElement element, ProblemsHolder holder) {
        if (element instanceof VtlSet vtlSet) {
            if (vtlSet.getAssignedMethodCallExpression() != null) {
                holder.newProblem(VelocityLocalize.assignmentToMethodCall())
                    .range(vtlSet.getFirstChild())
                    .highlightType(LIKE_UNUSED_SYMBOL)
                    .create();
            }
            else {
                PsiType assignedType = vtlSet.getAssignedVariableElementType();
                if (PsiType.VOID.equals(assignedType)) {
                    holder.newProblem(VelocityLocalize.assignmentOfVoid())
                        .range(vtlSet.getFirstChild())
                        .highlightType(LIKE_UNUSED_SYMBOL)
                        .create();
                }
            }
        }
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return VelocityLocalize.vtlDirectiveArgsInspection();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "VtlDirectiveArgsInspection";
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return VelocityLocalize.inspectiondescriptionsVtldirectiveargsinspection();
    }
}