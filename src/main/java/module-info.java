/**
 * @author VISTALL
 * @since 11/02/2023
 */
module com.intellij.velocity
{
    requires consulo.ide.api;
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