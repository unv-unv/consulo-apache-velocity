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
package com.intellij.velocity.psi.files;

import com.intellij.velocity.psi.VtlLanguage;
import consulo.html.language.HTMLLanguage;
import consulo.language.Language;
import consulo.language.file.FileTypeManager;
import consulo.language.file.LanguageFileType;
import consulo.language.impl.file.MultiplePsiFilesPerDocumentFileViewProvider;
import consulo.language.impl.psi.PsiFileImpl;
import consulo.language.parser.ParserDefinition;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.LanguageSubstitutors;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.ConfigurableTemplateLanguageFileViewProvider;
import consulo.language.template.TemplateDataLanguageMappings;
import consulo.language.template.TemplateLanguage;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Set;

/**
 * @author Alexey Chmutov
 */
public class VtlFileViewProvider extends MultiplePsiFilesPerDocumentFileViewProvider implements ConfigurableTemplateLanguageFileViewProvider {
    private final Language myTemplateDataLanguage;

    public VtlFileViewProvider(final PsiManager manager, final VirtualFile virtualFile, final boolean physical) {
        super(manager, virtualFile, physical);
        final Language language = getTemplateDataLanguage(virtualFile, manager.getProject());
        myTemplateDataLanguage = language instanceof TemplateLanguage ? PlainTextLanguage.INSTANCE : LanguageSubstitutors.substituteLanguage(language, virtualFile, manager.getProject());
    }

    public VtlFileViewProvider(final PsiManager manager, final VirtualFile virtualFile, final boolean physical,
                               final Language templateDataLanguage) {
        super(manager, virtualFile, physical);
        myTemplateDataLanguage = templateDataLanguage;
    }

    @Nonnull
    public Language getBaseLanguage() {
        return VtlLanguage.INSTANCE;
    }

    @Nonnull
    public Set<Language> getLanguages() {
        return Set.of(VtlLanguage.INSTANCE, getTemplateDataLanguage());
    }

    @Nullable
    protected PsiFile createFile(final Language lang) {
        if (lang == getBaseLanguage()) {
            return createFileInner(lang);
        }
        if (lang == getTemplateDataLanguage()) {
            final PsiFileImpl file = (consulo.language.impl.psi.PsiFileImpl) createFileInner(lang);
            file.setContentElementType(VtlFileElementTypes.TEMPLATE_DATA);
            return file;
        }
        return null;
    }

    private consulo.language.psi.PsiFile createFileInner(Language lang) {
        return ParserDefinition.forLanguage(lang).createFile(this);
    }

    protected VtlFileViewProvider cloneInner(final VirtualFile copy) {
        return new VtlFileViewProvider(getManager(), copy, false, myTemplateDataLanguage);
    }

    @Nonnull
    public Language getTemplateDataLanguage() {
        return myTemplateDataLanguage;
    }

    @Nonnull
    static Language getTemplateDataLanguage(@Nonnull consulo.virtualFileSystem.VirtualFile virtualFile, @Nonnull Project project) {
        final Language language = TemplateDataLanguageMappings.getInstance(project).getMapping(virtualFile);
        return language == null ? getTemplateDataLanguageByExtention(virtualFile) : language;
    }

    @Nonnull
    private static Language getTemplateDataLanguageByExtention(consulo.virtualFileSystem.VirtualFile virtualFile) {
        String name = virtualFile.getName();
        int index2 = name.lastIndexOf('.');
        if (index2 < 3) {
            return HTMLLanguage.INSTANCE;
        }
        int index1 = 1 + name.lastIndexOf('.', index2 - 1);
        if (index1 < 1) {
            return HTMLLanguage.INSTANCE;
        }
        String dataLanguageFileExtension = name.substring(index1, index2).toUpperCase();
        FileType fileType = FileTypeManager.getInstance().getStdFileType(dataLanguageFileExtension);
        if (fileType instanceof LanguageFileType) {
            return ((LanguageFileType) fileType).getLanguage();
        }
        return HTMLLanguage.INSTANCE;
    }
}
