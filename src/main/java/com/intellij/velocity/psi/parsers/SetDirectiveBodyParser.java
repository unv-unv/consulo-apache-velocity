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

import com.intellij.velocity.psi.VtlCompositeStarterTokenType;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.ast.IElementType;
import consulo.language.parser.PsiBuilder;

import static com.intellij.velocity.psi.VtlElementTypes.*;

/**
 * @author Alexey Chmutov
 * @since 2008-03-27
 */
public class SetDirectiveBodyParser extends CompositeBodyParser {
    public static final SetDirectiveBodyParser INSTANCE = new SetDirectiveBodyParser();

    private SetDirectiveBodyParser() {
    }

    @Override
    public void parseBody(PsiBuilder builder, PsiBuilder.Marker bodyMarker) {
        assertToken(builder, LEFT_PAREN);
        PsiBuilder.Marker variable = builder.mark();
        IElementType elementStarter = builder.getTokenType();
        builder.advanceLexer();
        if (elementStarter == START_REFERENCE || elementStarter == START_REF_FORMAL) {
            variable.drop();
            CompositeBodyParser bodyParser = ((VtlCompositeStarterTokenType) elementStarter).getCompositeBodyParser();
            bodyParser.parseBody(builder, null);
        }
        else {
            variable.error(VelocityLocalize.tokenExpected(START_REFERENCE));
        }

        assertToken(builder, ASSIGN);
        VtlParser.parseBinaryExpression(builder);
        assertToken(builder, RIGHT_PAREN);
        bodyMarker.done(DIRECTIVE_SET);
    }
}
