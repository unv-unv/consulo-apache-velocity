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

import com.intellij.velocity.VtlFileIndex;
import com.intellij.velocity.psi.PsiUtil;
import com.intellij.velocity.psi.files.VtlFile;
import com.intellij.velocity.psi.files.VtlFileViewProvider;
import com.intellij.velocity.psi.reference.VtlReferenceExpression;
import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.access.RequiredWriteAction;
import consulo.apache.velocity.icon.VelocityIconGroup;
import consulo.apache.velocity.localize.VelocityLocalize;
import consulo.application.Result;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorPopupHelper;
import consulo.document.Document;
import consulo.fileEditor.FileEditorManager;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.editor.FileModificationService;
import consulo.language.editor.WriteCommandAction;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.template.Template;
import consulo.language.editor.template.TemplateManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.module.Module;
import consulo.module.content.ModuleRootManager;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.BaseListPopupStep;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.PopupStep;
import consulo.ui.image.Image;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.IOException;
import java.util.Collection;

/**
 * @author Alexey Chmutov
 */
public abstract class DefineInCommentIntention implements IntentionAction {
    private final LocalizeValue myText;
    public static final String VELOCITY_IMPLICIT_VM = "velocity_implicit.vm";

    public DefineInCommentIntention(@Nonnull LocalizeValue text, @Nonnull LocalizeValue familyName) {
        myText = text;
    }

    @Override
    @Nonnull
    public LocalizeValue getText() {
        return myText;
    }

    @Override
    @RequiredReadAction
    public final boolean isAvailable(@Nonnull Project project, consulo.codeEditor.Editor editor, PsiFile file) {
        return file.getViewProvider() instanceof VtlFileViewProvider
            && getReferenceElement(editor, file) != null
            && file.getModule() != null;
    }

    @Nullable
    protected PsiElement getReferenceElement(@Nonnull consulo.codeEditor.Editor editor, @Nonnull PsiFile file) {
        VtlReferenceExpression ref = Util.findReferenceExpression(editor, file);
        return ref != null && ref.multiResolve(false).length == 0 && isAvailable(ref) ? ref : null;
    }

    protected boolean isAvailable(@Nonnull VtlReferenceExpression ref) {
        return true;
    }

    @RequiredUIAccess
    protected void defineInComment(
        consulo.codeEditor.Editor editor,
        final PsiFile fileWithVarReference,
        final PsiFile fileToInsertComment,
        final boolean addFileReference
    ) {
        final consulo.language.psi.PsiElement ref = getReferenceElement(editor, fileWithVarReference);
        assert ref != null;
        final Project project = fileWithVarReference.getProject();
        if (!FileModificationService.getInstance().prepareFileForWrite(fileToInsertComment)) {
            return;
        }

        PsiDocumentManager documentManager = consulo.language.psi.PsiDocumentManager.getInstance(project);
        final Document documentToInsertComment = documentManager.getDocument(fileToInsertComment);
        assert documentToInsertComment != null;
        new WriteCommandAction(project) {
            @Override
            @RequiredWriteAction
            protected void run(Result result) throws Throwable {
                Editor editor = FileEditorManager.getInstance(project).openTextEditor(
                    OpenFileDescriptorFactory.getInstance(project)
                        .builder(fileToInsertComment.getViewProvider().getVirtualFile())
                        .build(),
                    true
                );
                assert editor != null;
                assert documentToInsertComment == editor.getDocument();
                int insertionIndex = documentToInsertComment.getText().startsWith(VtlFileIndex.IMPLICIT_INCLUDE_MARKER)
                    ? VtlFileIndex.IMPLICIT_INCLUDE_MARKER.length()
                    : 0;
                editor.getCaretModel().moveToOffset(insertionIndex);
                TemplateManager manager = TemplateManager.getInstance(project);
                Template template = manager.createTemplate("", "");
                String relativePath = addFileReference ? PsiUtil.getRelativePath(fileToInsertComment, fileWithVarReference) : null;
                prepareTemplate(template, ref, relativePath, fileToInsertComment);
                manager.startTemplate(editor, template);
            }
        }.execute();
    }

    protected abstract void prepareTemplate(
        @Nonnull Template template,
        @Nonnull PsiElement element,
        @Nullable String relativePath,
        @Nonnull PsiFile fileToInsertComment
    );

    @RequiredUIAccess
    protected void chooseTargetFile(final PsiFile file, final consulo.codeEditor.Editor editor, final boolean addFileReference) {
        final Collection<VtlFile> implicitlyIncludedFiles = VtlFileIndex.getImplicitlyIncludedFiles(file);
        if (implicitlyIncludedFiles.size() == 1) {
            defineInComment(editor, file, implicitlyIncludedFiles.iterator().next(), addFileReference);
            return;
        }

        if (implicitlyIncludedFiles.size() < 1) {
            VtlFile newTargetFile = new WriteCommandAction<VtlFile>(file.getProject()) {
                @Override
                @RequiredWriteAction
                protected void run(Result<VtlFile> result) throws Throwable {
                    consulo.virtualFileSystem.VirtualFile virtualFile = createVelocityImplicitVmFile();
                    if (virtualFile == null) {
                        return;
                    }
                    VirtualFileUtil.saveText(virtualFile, VtlFileIndex.IMPLICIT_INCLUDE_MARKER);
                    if (file.getManager().findFile(virtualFile) instanceof VtlFile vtlFile) {
                        result.setResult(vtlFile);
                    }
                }

                @Nullable
                @RequiredWriteAction
                private VirtualFile createVelocityImplicitVmFile() throws IOException {
                    Module module = file.getModule();
                    consulo.virtualFileSystem.VirtualFile[] roots = ModuleRootManager.getInstance(module)
                        .getContentFolderFiles(LanguageContentFolderScopes.all(false));
                    if (roots.length > 0) {
                        return roots[0].createChildData(this, VELOCITY_IMPLICIT_VM);
                    }
                    PsiDirectory psiDirectory = file.getContainingDirectory();
                    return psiDirectory == null ? null : psiDirectory.getVirtualFile().createChildData(this, VELOCITY_IMPLICIT_VM);
                }
            }.execute().getResultObject();
            if (newTargetFile != null) {
                defineInComment(editor, file, newTargetFile, addFileReference);
            }
            return;
        }

        BaseListPopupStep<VtlFile> step = new BaseListPopupStep<VtlFile>(
            VelocityLocalize.chooseExternalDefinitionsFile().get(),
            implicitlyIncludedFiles.toArray(new VtlFile[implicitlyIncludedFiles.size()])
        ) {
            @Nonnull
            @Override
            public String getTextFor(VtlFile value) {
                return value.getViewProvider().getVirtualFile().getName();
            }

            @Override
            @RequiredUIAccess
            public PopupStep onChosen(VtlFile selectedValue, boolean finalChoice) {
                if (finalChoice) {
                    defineInComment(editor, file, selectedValue, addFileReference);
                }
                return super.onChosen(selectedValue, finalChoice);
            }

            @Override
            public boolean isSpeedSearchEnabled() {
                return true;
            }

            @Override
            public Image getIconFor(VtlFile aValue) {
                return VelocityIconGroup.velocity();
            }
        };
        EditorPopupHelper.getInstance().showPopupInBestPositionFor(editor, JBPopupFactory.getInstance().createListPopup(step));
    }

    @Override
    public boolean startInWriteAction() {
        return true;
    }
}