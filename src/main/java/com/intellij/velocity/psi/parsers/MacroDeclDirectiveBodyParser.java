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

import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.language.parser.PsiBuilder;

import static com.intellij.velocity.psi.VtlElementTypes.*;

/**
 * @author Alexey Chmutov
 * @since 2008-03-27
 */
public class MacroDeclDirectiveBodyParser extends CompositeBodyParser {
    public static final MacroDeclDirectiveBodyParser INSTANCE = new MacroDeclDirectiveBodyParser();

    private MacroDeclDirectiveBodyParser() {
    }

    @Override
    public void parseBody(PsiBuilder builder, PsiBuilder.Marker bodyMarker) {
        PsiBuilder.Marker directiveHeader = builder.mark();
        if (assertToken(builder, LEFT_PAREN)) {
            assertToken(builder, IDENTIFIER);
            if (builder.getTokenType() != RIGHT_PAREN) {
                VtlParser.parseList(builder, ListHandler.PARAMETER_LIST_HANDLER, false);
                assertToken(builder, RIGHT_PAREN);
            }
            else {
                builder.advanceLexer();
            }
        }
        else {
            builder.error(VelocityLocalize.macroDeclarationExpected());
        }
        directiveHeader.done(DIR_HEADER);
        VtlParser.parseCompositeElements(builder, COMMON_END_DETECTOR);
        finishCompositeWithEnd(builder, bodyMarker, DIRECTIVE_MACRODECL);
    }
}
