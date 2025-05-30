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
package com.intellij.velocity.psi.parsers;

import static com.intellij.velocity.psi.VtlElementTypes.*;
import static com.intellij.velocity.psi.parsers.CompositeBodyParser.CompositeEndDetector;
import static com.intellij.velocity.psi.parsers.CompositeBodyParser.assertToken;
import static com.intellij.velocity.psi.parsers.CompositeBodyParser.consumeTokenIfPresent;
import static com.intellij.velocity.psi.parsers.CompositeBodyParser.noForeachStarted;

import jakarta.annotation.Nonnull;

import consulo.language.parser.PsiBuilder;
import consulo.language.ast.IElementType;
import com.intellij.velocity.VelocityBundle;
import com.intellij.velocity.psi.VtlCompositeStarterTokenType;
import consulo.language.ast.ASTNode;
import consulo.language.parser.PsiParser;
import consulo.language.version.LanguageVersion;

/**
 * @author Alexey Chmutov
 */
public class VtlParser implements PsiParser
{

    private static final CompositeEndDetector EOF_DETECTOR = new CompositeEndDetector() {
        @Override
        public boolean isCompositeFinished(PsiBuilder builder) {
            return false;
        }
    };

    private static final CompositeEndDetector DOUBLE_QUOTE_DETECTOR = new CompositeEndDetector() {
        @Override
        public boolean isCompositeFinished(PsiBuilder builder) {
            return builder.getTokenType() == DOUBLE_QUOTE;
        }
    };

    @Nonnull
    public ASTNode parse(IElementType root, PsiBuilder builder, LanguageVersion languageVersion) {
        final PsiBuilder.Marker rootMarker = builder.mark();
        parseCompositeElements(builder, EOF_DETECTOR);
        rootMarker.done(root);
        return builder.getTreeBuilt();
    }

    static void parseCompositeElements(PsiBuilder builder, CompositeEndDetector detector) {
        while (!detector.isCompositeFinished(builder) && !builder.eof()) {
            consulo.language.ast.IElementType currentTokenType = builder.getTokenType();
            if (detector.isTokenInvalid(currentTokenType)) {
                builder.error(VelocityBundle.message("invalid.token", builder.getTokenText()));
            } else if (currentTokenType == SHARP_BREAK && noForeachStarted(builder)) {
                builder.error(VelocityBundle.message("vtl.break.should.be.within.foreach"));
            }
            if (currentTokenType instanceof VtlCompositeStarterTokenType) {
                parseComposite(builder, (VtlCompositeStarterTokenType) builder.getTokenType());
            } else {
                builder.advanceLexer();
            }
        }
    }

    static void parseComposite(PsiBuilder builder, VtlCompositeStarterTokenType compositeStarter) {
        PsiBuilder.Marker bodyMarker = builder.mark();
        CompositeBodyParser parser = compositeStarter.getCompositeBodyParser();
        builder.advanceLexer();
        parser.parseBody(builder, bodyMarker);
    }

    static void parseList(PsiBuilder builder, ListHandler handler, boolean requireSeparator) {
        boolean firstElement = true;
        while (!handler.isListFinished(builder) && !builder.eof()) {
            if (firstElement) {
                firstElement = false;
            } else if (!handler.parseSeparator(builder) && requireSeparator) {
                builder.error(VelocityBundle.message("token.expected", COMMA));
            }
            handler.parseListElement(builder);
        }
    }

//    private static void printCurrent(PsiBuilder builder) {
//        System.out.println("Type: " + builder.getTokenType() + " TokenText: " + builder.getTokenText());
//    }

    static boolean parseBinaryExpression(final PsiBuilder builder) {

        PsiBuilder.Marker expr = builder.mark();
        if (!parseRelationalExpression(builder)) {
            expr.drop();
            return false;
        }
        while (LOGICAL_OPERATIONS.contains(builder.getTokenType())) {
            builder.advanceLexer();
            if (!parseRelationalExpression(builder)) {
                builder.error(VelocityBundle.message("expression.expected"));
            }
            expr.done(BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private static boolean parseRelationalExpression(final PsiBuilder builder) {

        PsiBuilder.Marker expr = builder.mark();
        if (!parseAdditiveExpression(builder)) {
            expr.drop();
            return false;
        }
        while (RELATIONAL_OPERATIONS.contains(builder.getTokenType())) {
            builder.advanceLexer();
            if (!parseAdditiveExpression(builder)) {
                builder.error(VelocityBundle.message("expression.expected"));
            }
            expr.done(BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private static boolean parseAdditiveExpression(final PsiBuilder builder) {
        PsiBuilder.Marker expr = builder.mark();
        if (!parseMultiplicativeExpression(builder)) {
            expr.drop();
            return false;
        }
        while (ADDITIVE_OPERATIONS.contains(builder.getTokenType())) {
            builder.advanceLexer();
            if (!parseMultiplicativeExpression(builder)) {
                builder.error(VelocityBundle.message("expression.expected"));
            }
            expr.done(BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private static boolean parseMultiplicativeExpression(final PsiBuilder builder) {
        PsiBuilder.Marker expr = builder.mark();
        if (!parseUnaryExpression(builder)) {
            expr.drop();
            return false;
        }
        while (MULTIPLICATIVE_OPERATIONS.contains(builder.getTokenType())) {
            builder.advanceLexer();
            if (!parseUnaryExpression(builder)) {
                builder.error(VelocityBundle.message("expression.expected"));
            }
            expr.done(BINARY_EXPRESSION);
            expr = expr.precede();
        }
        expr.drop();
        return true;
    }

    private static boolean parseUnaryExpression(final PsiBuilder builder) {
        final consulo.language.ast.IElementType tokenType = builder.getTokenType();
        if (UNARY_OPERATIONS.contains(tokenType)) {
            final PsiBuilder.Marker expr = builder.mark();
            builder.advanceLexer();
            if (!parseUnaryExpression(builder)) {
                builder.error(VelocityBundle.message("expression.expected"));
            }
            expr.done(UNARY_EXPRESSION);
            return true;
        } else {
            return parseOperand(builder, true);
        }
    }

    static boolean parseOperand(PsiBuilder builder, boolean allowParenthesized) {
        PsiBuilder.Marker expression = builder.mark();
        final consulo.language.ast.IElementType elementStarter = builder.getTokenType();
        builder.advanceLexer();

        if (elementStarter == INTEGER) {
            expression.done(INTEGER_LITERAL);
        } else if (elementStarter == DOUBLE) {
            expression.done(DOUBLE_LITERAL);
        } else if (elementStarter == BOOLEAN) {
            expression.done(BOOLEAN_LITERAL);
        } else if (elementStarter == SINGLE_QUOTE) {
            consumeTokenIfPresent(builder, STRING_TEXT);
            assertToken(builder, SINGLE_QUOTE);
            expression.done(STRING_LITERAL);
        } else if (elementStarter == DOUBLE_QUOTE) {
            if(!consumeTokenIfPresent(builder, STRING_TEXT)) {
                parseCompositeElements(builder, DOUBLE_QUOTE_DETECTOR);
            }
            assertToken(builder, DOUBLE_QUOTE);
            expression.done(DOUBLEQUOTED_TEXT);
        } else if (elementStarter == START_REFERENCE || elementStarter == START_REF_FORMAL) {
            CompositeBodyParser parser = ((VtlCompositeStarterTokenType) elementStarter).getCompositeBodyParser();
            parser.parseBody(builder, expression);
        } else if (elementStarter == LEFT_BRACKET) {
            parseList(builder, ListHandler.LIST_HANDLER, true);
            assertToken(builder, RIGHT_BRACKET);
            expression.done(LIST_EXPRESSION);
        } else if (allowParenthesized && elementStarter == LEFT_PAREN) {
            parseBinaryExpression(builder);
            assertToken(builder, RIGHT_PAREN);
            expression.done(PARENTHESIZED_EXPRESSION);
        } else if (elementStarter == LEFT_BRACE_IN_EXPR) {
            parseList(builder, ListHandler.MAP_HANDLER, true);
            assertToken(builder, RIGHT_BRACE_IN_EXPR);
            expression.done(MAP_EXPRESSION);
        } else {
            expression.drop();
            builder.error(VelocityBundle.message("operand.expected"));
            return false;
        }
        return true;
    }
}