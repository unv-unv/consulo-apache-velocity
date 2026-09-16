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

import com.intellij.velocity.psi.VtlElementTypes;
import com.intellij.velocity.psi.VtlExpression;
import com.intellij.velocity.psi.VtlMacro;
import com.intellij.velocity.psi.VtlParameterDeclaration;
import com.intellij.velocity.psi.directives.VtlMacroImpl;
import com.intellij.velocity.psi.directives.VtlParse;
import com.intellij.velocity.psi.files.VtlFile;
import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.document.util.TextRange;
import consulo.language.ast.ASTNode;
import consulo.language.editor.annotation.AnnotationHolder;
import consulo.language.editor.annotation.Annotator;
import consulo.language.psi.PsiElement;
import consulo.language.psi.resolve.BaseScopeProcessor;
import consulo.language.psi.resolve.ResolveState;
import consulo.localize.LocalizeValue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexey Chmutov
 */
public class VtlMacroAnnotator implements Annotator {
    private static final String V_IDENT = "([a-zA-Z_][a-zA-Z_0-9-]*)";
    private static final Pattern WOULD_BE_MACRO_CALL_PATTERN = Pattern.compile("(#" + V_IDENT + ")|(#\\{" + V_IDENT + "\\})");

    @Override
    @RequiredReadAction
    public void annotate(PsiElement element, AnnotationHolder holder) {
        if (element instanceof VtlMacroImpl macro) {
            VtlFile file = macro.getContainingFile();
            String macroName = macro.getName();
            if (macroName != null && file.getNumberOfMacros(macroName) > 1) {
                holder.newError(VelocityLocalize.macroIsAlreadyDefined(macroName, file.getName()))
                    .range(macro.getNameElementRange())
                    .create();
            }
        }
        else if (element instanceof VtlParameterDeclaration param) {
            String paramName = param.getName();
            if (paramName == null) {
                return;
            }
            consulo.language.psi.PsiElement sibling = param.getPrevSibling();
            while (sibling != null) {
                if (sibling instanceof VtlParameterDeclaration paramDecl
                    && paramName.equals(paramDecl.getName())) {
                    LocalizeValue msg = VelocityLocalize.duplicatedParameterName(paramName);
                    holder.newError(msg).range(param.getTextRange()).create();
                    holder.newError(msg).range(sibling.getTextRange()).create();
                }
                sibling = sibling.getPrevSibling();
            }
        }
        else if (element instanceof VtlParse parse) {
            VtlFile parsedFile = parse.resolveFile();
            if (parsedFile == null) {
                return;
            }
            VtlExpression parsedFileElement = parse.getArgumentList().getArguments()[0];
            VtlFile containingFile = parse.getContainingFile();
            for (String macroName : containingFile.getDefinedMacroNames()) {
                if (parsedFile.getNumberOfMacros(macroName) > 0) {
                    LocalizeValue msg =
                        VelocityLocalize.macroDeclarationWillBeIgnored(macroName, containingFile.getName(), parsedFile.getName());
                    holder.newWarn(msg).range(parsedFileElement).create();
                }
            }
        }
        else {
            ASTNode node = element.getNode();
            if (node == null || node.getElementType() != VtlElementTypes.TEMPLATE_TEXT) {
                return;
            }
            Matcher matcher = WOULD_BE_MACRO_CALL_PATTERN.matcher(node.getText());
            if (!matcher.find()) {
                return;
            }
            if (!(element.getContainingFile() instanceof VtlFile vtlFile)) {
                return;
            }
            do {
                int index = matcher.start(2) != -1 ? 2 : 4;
                final String macroName = matcher.group(index);
                consulo.language.psi.resolve.BaseScopeProcessor processor = new BaseScopeProcessor() {
                    @Override
                    public boolean execute(PsiElement element, ResolveState state) {
                        return !(element instanceof VtlMacro macro && macroName.equals(macro.getName()));
                    }
                };
                if (!vtlFile.processAllMacrosInScope(processor, consulo.language.psi.resolve.ResolveState.initial())) {
                    TextRange range = new consulo.document.util.TextRange(matcher.start(), matcher.end())
                        .shiftRight(element.getTextOffset());
                    holder.newError(VelocityLocalize.willBeConsideredAsMacroCall()).range(range).create();
                }
            }
            while (matcher.find());
        }
    }
}