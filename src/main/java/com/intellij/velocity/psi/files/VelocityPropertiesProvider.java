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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.jetbrains.annotations.NonNls;
import com.intellij.lang.properties.IProperty;
import com.intellij.lang.properties.psi.PropertiesFile;
import consulo.virtualFileSystem.VirtualFile;
import consulo.language.file.FileViewProvider;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.util.collection.ArrayUtil;
import consulo.util.collection.ContainerUtil;
import com.intellij.velocity.psi.VtlLanguage;

/**
 * @author Alexey Chmutov
 */
public class VelocityPropertiesProvider {
    private static final String VELOCIMACRO_LIBRARY_PROPERTY = "velocimacro.library";
    private static final String FILE_RESOURCE_LOADER_PATH_PROPERTY = "file.resource.loader.path";

    private final PropertiesFile myPropertiesFile;
    @Nullable private final VirtualFile myRuntimeRoot;

    public VelocityPropertiesProvider(@Nonnull PropertiesFile file, @Nullable VirtualFile runtimeRoot) {
        this.myPropertiesFile = file;
        this.myRuntimeRoot = runtimeRoot;
    }

    public VelocityPropertiesProvider(@Nonnull PropertiesFile file) {
        this.myPropertiesFile = file;
        this.myRuntimeRoot = null;
    }

    @Nonnull
    public List<consulo.virtualFileSystem.VirtualFile> getResourceLoaderPathListBasedOn(@Nullable VirtualFile baseFile) {
        if(myRuntimeRoot != null) {
            baseFile = myRuntimeRoot;
        }
        if (baseFile == null) {
            return Collections.emptyList();
        }
        ArrayList<VirtualFile> res = new ArrayList<VirtualFile>();
        for (String loaderPath : getResourceLoaderPathList()) {
            VirtualFile loaderPathFile = baseFile.findFileByRelativePath(loaderPath);
            if (loaderPathFile != null) {
                res.add(loaderPathFile);
            }
        }
        return res;
    }

    public String[] getResourceLoaderPathList() {
        String value = getValue(FILE_RESOURCE_LOADER_PATH_PROPERTY);
        return value.length() == 0 ? new String[]{"."} : splitAndTrim(value);
    }

    private static String[] splitAndTrim(String values) {
        String[] array = values.split(",");
        for (int i = 0; i < array.length; i++) {
            array[i] = array[i].trim();
        }
        return array;
    }

    @Nonnull
    public List<VtlFile> getVelocimacroLibraryListBasedOn(@Nullable consulo.virtualFileSystem.VirtualFile baseFile) {
        if(myRuntimeRoot != null) {
          baseFile = myRuntimeRoot;
        }
        String[] libNames = getVelocimacroLibraryNames();
        if (baseFile == null || libNames.length == 0) {
            return Collections.emptyList();
        }

        final PsiManager manager = myPropertiesFile.getContainingFile().getManager();
        ArrayList<VtlFile> res = new ArrayList<VtlFile>();
        for (consulo.virtualFileSystem.VirtualFile loaderPathFile : getResourceLoaderPathListBasedOn(baseFile)) {
            for (int i = libNames.length - 1; i >= 0; i--) {
                consulo.virtualFileSystem.VirtualFile libFile = loaderPathFile.findFileByRelativePath(libNames[i]);
                if (libFile == null) {
                    continue;
                }
                final FileViewProvider viewProvider = manager.findViewProvider(libFile);
                if (viewProvider == null) {
                    continue;
                }
                PsiFile libPsiFile = viewProvider.getPsi(VtlLanguage.INSTANCE);
                if (libPsiFile instanceof VtlFile) {
                    ContainerUtil.addIfNotNull(res, (VtlFile) libPsiFile);
                }
            }
        }
        return res;
    }

    @Nonnull
    public PropertiesFile getPropertiesFile() {
        return myPropertiesFile;
    }

    @Nonnull
    public String[] getVelocimacroLibraryNames() {
        final String value = getValue(VELOCIMACRO_LIBRARY_PROPERTY);
        return value.length() == 0 ? ArrayUtil.EMPTY_STRING_ARRAY : splitAndTrim(value);
    }

    @Nonnull
    private String getValue(@Nonnull @NonNls String key) {
        IProperty resourceLoaderPathProp = myPropertiesFile.findPropertyByKey(key);
        if (resourceLoaderPathProp == null) {
            return "";
        }
        String val = resourceLoaderPathProp.getValue();
        return val != null ? val : "";
    }
}
