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
import com.intellij.velocity.psi.PsiUtil;
import com.intellij.velocity.psi.VtlExpression;
import com.intellij.velocity.psi.VtlLoopVariable;
import com.intellij.velocity.psi.VtlOperatorExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.editor.inspection.ProblemsHolder;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

import static consulo.language.editor.inspection.ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
import static consulo.language.editor.inspection.ProblemHighlightType.WEAK_WARNING;

/**
 * @author Alexey Chmutov
 * @since 2008-06-27
 */
@ExtensionImpl
public class VtlTypesInspection extends VtlInspectionBase {
    @Override
    @RequiredReadAction
    protected void registerProblems(PsiElement element, ProblemsHolder holder) {
        if (element instanceof VtlOperatorExpression expression) {
            if (expression.getPsiType() != null) {
                return;
            }
            LocalizeValue message = expression.getIndefiniteTypeMessage();
            if (message.isNotEmpty()) {
                holder.newProblem(message)
                    .range(expression)
                    .highlightType(WEAK_WARNING)
                    .create();
            }
        }
        else if (element instanceof VtlLoopVariable loopVariable) {
            if (loopVariable.getPsiType() != null) {
                return;
            }
            VtlExpression expression = loopVariable.getIterableExpression();
            if (expression == null) {
                return;
            }
            PsiType type = expression.getPsiType();
            if (type == null) {
                return;
            }
            String typeName = PsiUtil.getPresentableText(type);
            holder.newProblem(VelocityLocalize.illegalIterableExpressionType(typeName))
                .range(expression)
                .highlightType(GENERIC_ERROR_OR_WARNING)
                .create();
        }
    }

    @Override
    @Nonnull
    public LocalizeValue getDisplayName() {
        return VelocityLocalize.vtlTypesInspection();
    }

    @Override
    @Nonnull
    public String getShortName() {
        return "VtlTypesInspection";
    }

    @Nonnull
    @Override
    public LocalizeValue getDescription() {
        return VelocityLocalize.inspectiondescriptionsVtltypesinspection();
    }
}
