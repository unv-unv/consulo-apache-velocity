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

import com.intellij.java.language.psi.PsiType;
import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.ast.IElementType;
import consulo.language.psi.PsiElement;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;

/**
 * @author Alexey Chmutov
 * @since 2008-06-27
 */
public class VtlOperatorExpression extends VtlCompositeElement implements VtlExpression {

    private final boolean myBinary;

    public VtlOperatorExpression(@Nonnull ASTNode node, boolean binary) {
        super(node);
        myBinary = binary;
    }

    @Override
    @RequiredReadAction
    public PsiType getPsiType() {
        VtlExpressionTypeCalculator typeCalculator = getOperationSign().getTypeCalculator();
        VtlExpression operand1 = getOperand1();
        if (operand1 == null) {
            return null;
        }
        if (!myBinary) {
            return typeCalculator.calculateUnary(operand1);
        }
        VtlExpression operand2 = getOperand2();
        if (operand2 == null) {
            return null;
        }
        return typeCalculator.calculateBinary(operand1, operand2);
    }

    @Nonnull
    @RequiredReadAction
    private VtlOperatorTokenType getOperationSign() {
        ASTNode operationNode = getNode().findChildByType(VtlElementTypes.OPERATIONS);
        assert operationNode != null : getText();
        IElementType tokenType = operationNode.getElementType();
        assert tokenType instanceof VtlOperatorTokenType : getText();
        return (VtlOperatorTokenType) tokenType;
    }

    @RequiredReadAction
    private VtlExpression getOperand1() {
        return findChildByClass(VtlExpression.class);
    }

    @RequiredReadAction
    private VtlExpression getOperand2() {
        VtlExpression first = getOperand1();
        if (first == null) {
            return null;
        }
        PsiElement second = first.getNextSibling();
        while (second != null && !(second instanceof VtlExpression)) {
            second = second.getNextSibling();
        }
        return (VtlExpression) second;
    }

    @RequiredReadAction
    public LocalizeValue getIndefiniteTypeMessage() {
        VtlExpression op1 = getOperand1();
        PsiType opType1 = op1 == null ? null : op1.getPsiType();
        if (opType1 == null) {
            return LocalizeValue.empty();
        }
        if (!myBinary) {
            return VelocityLocalize.invalidOperandType(PsiUtil.getPresentableText(opType1));
        }
        VtlExpression op2 = getOperand2();
        PsiType opType2 = op2 == null ? null : op2.getPsiType();
        if (opType2 == null) {
            return LocalizeValue.empty();
        }
        return VelocityLocalize.invalidOperandsType(PsiUtil.getPresentableText(opType1), PsiUtil.getPresentableText(opType2));
    }
}
