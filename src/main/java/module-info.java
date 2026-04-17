/**
 * @author VISTALL
 * @since 11/02/2023
 */
module com.intellij.velocity
{
    requires consulo.ide.api;

    requires consulo.application.api;
    requires consulo.application.content.api;
    requires consulo.code.editor.api;
    requires consulo.color.scheme.api;
    requires consulo.component.api;
    requires consulo.document.api;
    requires consulo.file.editor.api;
    requires consulo.file.template.api;
    requires consulo.index.io;
    requires consulo.language.api;
    requires consulo.language.impl;
    requires consulo.language.editor.api;
    requires consulo.language.editor.refactoring.api;
    requires consulo.language.code.style.api;
    requires consulo.module.api;
    requires consulo.module.content.api;
    requires consulo.navigation.api;
    requires consulo.project.api;
    requires consulo.ui.api;
    requires consulo.ui.ex.api;
    requires consulo.usage.api;
    requires consulo.virtual.file.system.api;
    requires consulo.util.collection;
    requires consulo.util.dataholder;
    requires consulo.util.io;
    requires consulo.util.lang;

    requires consulo.java;
    requires com.intellij.xml;
    requires com.intellij.xml.html.api;
    requires com.intellij.properties;

    exports com.intellij.velocity;
    exports com.intellij.velocity.editorActions;
    exports com.intellij.velocity.inspections;
    exports com.intellij.velocity.inspections.wellformedness;
    exports com.intellij.velocity.lexer;
    exports com.intellij.velocity.psi;
    exports com.intellij.velocity.psi.directives;
    exports com.intellij.velocity.psi.files;
    exports com.intellij.velocity.psi.formatter;
    exports com.intellij.velocity.psi.parsers;
    exports com.intellij.velocity.psi.reference;
    exports com.intellij.velocity.spring;
    exports consulo.apache.velocity;
    exports consulo.apache.velocity.icon;
    exports consulo.apache.velocity.localize;
}