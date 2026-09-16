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

import com.intellij.java.language.psi.*;
import com.intellij.java.language.psi.util.TypeConversionUtil;
import consulo.annotation.access.RequiredReadAction;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.ast.ASTNode;
import consulo.language.psi.PsiElement;
import consulo.language.psi.scope.GlobalSearchScope;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author Alexey Chmutov
 */
public class VtlLoopVariable extends VtlPresentableNamedElement implements VtlVariable {
    public VtlLoopVariable(ASTNode node) {
        super(node);
    }

    @Override
    public String getTypeName() {
        return VelocityLocalize.typeNameLoopVariable().get();
    }

    @Override
    @RequiredReadAction
    public PsiType getPsiType() {
        return extractTypeFromIterable(getIterableExpression());
    }

    @Nullable
    @RequiredReadAction
    public VtlExpression getIterableExpression() {
        PsiElement wouldBeIterable = getNextSibling();
        while (wouldBeIterable != null) {
            if (wouldBeIterable instanceof VtlExpression expression) {
                return expression;
            }
            wouldBeIterable = wouldBeIterable.getNextSibling();
        }
        return null;
    }

    @Nullable
    private static PsiType extractTypeFromIterable(VtlExpression expr) {
        if (expr == null) {
            return null;
        }
        PsiType type = expr.getPsiType();
        if (type == null) {
            return null;
        }
        if (type instanceof PsiArrayType arrayType) {
            return arrayType.getComponentType();
        }
        if (!(type instanceof PsiClassType classType)) {
            return null;
        }
        PsiElementFactory factory = JavaPsiFacade.getInstance(expr.getProject()).getElementFactory();
        GlobalSearchScope scope = expr.getResolveScope();

        for (Object[] iterable : VELOCITY_ITERABLES) {
            PsiClassType iterableClassType = factory.createTypeByFQClassName((String) iterable[0], scope);
            if (!TypeConversionUtil.isAssignable(iterableClassType, classType)) {
                continue;
            }
            PsiClass iterableClass = iterableClassType.resolve();
            if (iterableClass == null) {
                continue;
            }
            PsiSubstitutor substitutor = PsiUtil.getSuperClassSubstitutor(iterableClass, classType);
            PsiTypeParameter[] parameters = iterableClass.getTypeParameters();
            int paramIndex = (Integer) iterable[1];
            PsiType result = paramIndex < parameters.length ? substitutor.substitute(parameters[paramIndex]) : null;
            return result != null ? result : factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_OBJECT, scope);
        }
        return null;
    }

    private static final Object[][] VELOCITY_ITERABLES = {
        {CommonClassNames.JAVA_UTIL_ITERATOR, 0},
        {CommonClassNames.JAVA_UTIL_COLLECTION, 0},
        {CommonClassNames.JAVA_UTIL_MAP, 1}
    };

    public static String[] getVelocityIterables(@Nonnull String className) {
        return new String[]{
            "java.util.Iterator<" + className + ">",
            "java.util.Collection<" + className + ">",
            "java.util.Map<?, " + className + ">",
            className + "[]",
        };
    }
}