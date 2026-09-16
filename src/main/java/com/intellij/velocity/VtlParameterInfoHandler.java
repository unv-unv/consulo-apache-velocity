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
package com.intellij.velocity;

import com.intellij.java.language.psi.PsiType;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import com.intellij.velocity.psi.*;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.language.Language;
import consulo.language.ast.ASTNode;
import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.editor.parameterInfo.*;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.util.PsiTreeUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Alexey Chmutov
 */
@ExtensionImpl
public class VtlParameterInfoHandler implements ParameterInfoHandler<VtlArgumentList, VtlCallable> {
    @Override
    public boolean couldShowInLookup() {
        return true;
    }

    @Override
    public Object[] getParametersForLookup(LookupElement item, ParameterInfoContext context) {
        //todo
        return null;
    }

    @Override
    public VtlArgumentList findElementForParameterInfo(CreateParameterInfoContext context) {
        return findArgumentList(context);
    }

    @Nullable
    private static VtlArgumentList findArgumentList(ParameterInfoContext context) {
        PsiFile file = context.getFile();

        PsiDocumentManager.getInstance(file.getProject()).commitDocument(context.getEditor().getDocument());
        PsiElement elementAt = file.getViewProvider().findElementAt(context.getOffset(), VtlLanguage.INSTANCE);
        if (elementAt == null) {
            return null;
        }
        VtlCallExpression call = PsiTreeUtil.getParentOfType(elementAt, VtlCallExpression.class);
        if (call == null) {
            return null;
        }
        return call.findArgumentList();
    }

    @Override
    @RequiredReadAction
    public void showParameterInfo(@Nonnull VtlArgumentList element, CreateParameterInfoContext context) {
        consulo.language.psi.PsiElement parent = element.getParent();
        if (!(parent instanceof VtlCallExpression callExpr)) {
            return;
        }
        VtlCallable[] candidates = callExpr.getCallableCandidates();
        if (candidates.length == 0) {
            return;
        }
        context.setItemsToShow(candidates);
        context.showHint(element, element.getTextRange().getStartOffset(), this);
    }

    @Override
    public VtlArgumentList findElementForUpdatingParameterInfo(UpdateParameterInfoContext context) {
        return findArgumentList(context);
    }

    @Override
    @RequiredReadAction
    public void updateParameterInfo(@Nonnull VtlArgumentList argumentList, UpdateParameterInfoContext context) {
        assert argumentList.isValid();
        PsiElement prevOwner = context.getParameterOwner();
        int prevOwnerTextOffset = prevOwner == null ? 0 : prevOwner.getTextOffset();

        context.setParameterOwner(argumentList);
        if (prevOwnerTextOffset != 0 && prevOwnerTextOffset != argumentList.getTextOffset()) {
            context.removeHint();
            context.setCurrentParameter(-1);
            return;
        }
        int offset = context.getEditor().getCaretModel().getOffset() - argumentList.getTextRange().getStartOffset();
        if (offset < 0) {
            context.setCurrentParameter(-1);
            return;
        }
        int index = 0;
        for (ASTNode child : argumentList.getNode().getChildren(null)) {
            PsiElement psiChild = child.getPsi();
            offset -= child.getTextLength();
            if (!(psiChild instanceof VtlExpression)) {
                continue;
            }
            if (offset <= 0) {
                break;
            }
            index++;
        }
        context.setCurrentParameter(index);
    }

    @Override
    public String getParameterCloseChars() {
        return ", )";
    }

    @Override
    public boolean tracksParameterIndex() {
        return false;
    }

    @Override
    @RequiredReadAction
    public void updateUI(VtlCallable callable, ParameterInfoUIContext context) {
        PsiElement list = context.getParameterOwner();
        if (!list.isValid()) {
            return;
        }

        int index = context.getCurrentParameterIndex();
        boolean applicable = isApplicable(callable, list, index);

        int highlightStart = -1;
        int highlightEnd = -1;

        StringBuilder sb = new StringBuilder();
        VtlVariable[] variables = callable.getParameters();
        boolean isMacro = callable instanceof VtlMacro;
        String delimiter = isMacro ? " " : ", ";
        for (int i = 0; i < variables.length; i++) {
            if (i > 0) {
                sb.append(delimiter);
            }
            if (i == index) {
                highlightStart = sb.length();
            }
            VtlVariable variable = variables[i];
            PsiType type = variable.getPsiType();
            if (type != null) {
                sb.append(type.getPresentableText()).append(" ");
            }
            if (isMacro) {
                sb.append('$');
            }
            sb.append(variable.getName());
            if (i == index) {
                highlightEnd = sb.length();
            }
        }

        if (variables.length == 0) {
            sb.append(CodeInsightLocalize.parameterInfoNoParameters().get());
        }

        boolean deprecated = callable.isDeprecated();
        context.setupUIComponentPresentation(
            sb.toString(),
            highlightStart,
            highlightEnd,
            !applicable,
            deprecated,
            false,
            context.getDefaultParameterColor()
        );
    }

    private static boolean isApplicable(VtlCallable callable, PsiElement list, int index) {
        boolean applicable = callable.getParameters().length > index || index == 0 && callable.getParameters().length == 0;
        PsiElement parent = list.getParent();
        if (applicable && parent instanceof VtlMethodCallExpression methodCall) {
            VtlExpression[] arguments = methodCall.getArgumentList().getArguments();
            VtlVariable[] parameters = callable.getParameters();
            for (int i = 0; i < index; i++) {
                PsiType paramType = parameters[i].getPsiType();
                PsiType argType = arguments[i].getPsiType();
                if (argType == null && paramType == null) {
                    continue;
                }
                paramType = PsiUtil.getBoxedType(paramType, list);
                if (argType == null || paramType == null ||
                    !TypeConversionUtil.areTypesConvertible(argType, paramType)) {
                    return false;
                }
            }
        }
        return applicable;
    }

    @Nonnull
    @Override
    public Language getLanguage() {
        return VtlLanguage.INSTANCE;
    }
}
