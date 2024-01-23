/*
 * Copyright (C) 2024 The OFFICELOVE Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officelove.word;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import kiss.I;
import kiss.Managed;
import kiss.Model;
import kiss.Signal;
import kiss.Singleton;
import officelove.LibreOffice;
import psychopath.Directory;
import psychopath.File;
import psychopath.Locator;

@Managed(Singleton.class)
public abstract class TemplateDefinitions<T> {

    /**
     * Locate template directory.
     * 
     * @return
     */
    protected abstract Directory locate();

    /**
     * List up all defined templates.
     * 
     * @return
     */
    public final List<Templatable> templates() {
        return I.signal(getClass().getFields())
                .take(f -> Modifier.isFinal(f.getModifiers()))
                .take(f -> Templatable.class.isAssignableFrom(f.getType()))
                .map(f -> (Templatable) f.get(this))
                .toList();

    }

    /**
     * Print the evaluated document.
     */
    protected void printDoc(Templatable templatable, String printer, List context, T data) {
        File temp = Locator.temporaryFile();

        // evaluate template
        new Word(templatable.file()).evaluate(context).save(temp);

        // print
        LibreOffice.print(temp, printer);
    }

    /**
     * Publish the evaluated document.
     * 
     * @param file
     */
    protected void publishDoc(Templatable templatable, File file, List context, T data) {
        new Word(templatable.file()).evaluate(context).save(file);
    }

    /**
     * Select the default printer.
     * 
     * @param info
     * @return
     */
    protected String selectPrinter(T info) {
        return null;
    }

    /**
     * Template API.
     */
    public abstract class Templatable<Self> {

        /** The template name. */
        private String name;

        /** The context types. */
        private List<Class> types;

        /** The associated user data. */
        T data;

        /**
         * Find the template field.
         * 
         * @return
         */
        private Signal<Field> field() {
            return I.signal(TemplateDefinitions.this.getClass().getFields())
                    .take(f -> Modifier.isFinal(f.getModifiers()))
                    .take(f -> Templatable.class.isAssignableFrom(f.getType()))
                    .take(f -> {
                        try {
                            return f.get(TemplateDefinitions.this) == this;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .first();
        }

        /**
         * Build without user data.
         */
        protected Templatable() {
        }

        /**
         * Build with user data.
         */
        protected Templatable(T data) {
            this.data = data;
        }

        /**
         * Detect the name of template.
         * 
         * @return
         */
        public String name() {
            if (name == null) {
                name = field().map(Field::getName).to().exact();
            }
            return name;
        }

        /**
         * Detect the name of template.
         * 
         * @return
         */
        List<Class> types() {
            if (types == null) {
                types = field().flatArray(f -> Model.collectParameters(f.getGenericType(), templateType())).as(Class.class).toList();
            }
            return types;
        }

        /**
         * Determine template type.
         * 
         * @return
         */
        private Class templateType() {
            Class type = getClass();
            while (type.getSuperclass() != Templatable.class) {
                type = type.getSuperclass();
            }
            return type;
        }

        public final File file() {
            return locate().file(name() + ".docx");
        }

        /**
         * Validate this template.
         * 
         * @return
         */
        public final boolean validate() {
            return new Word(file()).validate(types());
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return name();
        }
    }

    /**
     * Template without context.
     */
    public class Template extends Templatable<Template> {

        /**
         * Build without user data.
         */
        public Template() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print() {
            printDoc(this, selectPrinter(data), new ArrayList(), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer) {
            printDoc(this, printer, new ArrayList(), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file) {
            publishDoc(this, file, new ArrayList(), data);
        }
    }

    /**
     * Template with context.
     */
    public class Template1<C1> extends Templatable<Template1<C1>> {

        /**
         * Build without user data.
         */
        public Template1() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template1(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(C1 context1) {
            printDoc(this, selectPrinter(data), I.list(context1), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer, C1 context1) {
            printDoc(this, printer, I.list(context1), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file, C1 context1) {
            publishDoc(this, file, I.list(context1), data);
        }
    }

    /**
     * Template with context.
     */
    public class Template2<C1, C2> extends Templatable<Template2<C1, C2>> {

        /**
         * Build without user data.
         */
        public Template2() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template2(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(C1 context1, C2 context2) {
            printDoc(this, selectPrinter(data), I.list(context1, context2), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer, C1 context1, C2 context2) {
            printDoc(this, printer, I.list(context1, context2), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file, C1 context1, C2 context2) {
            publishDoc(this, file, I.list(context1, context2), data);
        }
    }

    /**
     * Template with context.
     */
    public class Template3<C1, C2, C3> extends Templatable<Template3<C1, C2, C3>> {

        /**
         * Build without user data.
         */
        public Template3() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template3(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(C1 context1, C2 context2, C3 context3) {
            printDoc(this, selectPrinter(data), I.list(context1, context2, context3), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer, C1 context1, C2 context2, C3 context3) {
            printDoc(this, printer, I.list(context1, context2, context3), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file, C1 context1, C2 context2, C3 context3) {
            publishDoc(this, file, I.list(context1, context2, context3), data);
        }
    }

    /**
     * Template with context.
     */
    public class Template4<C1, C2, C3, C4> extends Templatable<Template4<C1, C2, C3, C4>> {

        /**
         * Build without user data.
         */
        public Template4() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template4(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(C1 context1, C2 context2, C3 context3, C4 context4) {
            printDoc(this, selectPrinter(data), I.list(context1, context2, context3, context4), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer, C1 context1, C2 context2, C3 context3, C4 context4) {
            printDoc(this, printer, I.list(context1, context2, context3, context4), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file, C1 context1, C2 context2, C3 context3, C4 context4) {
            publishDoc(this, file, I.list(context1, context2, context3, context4), data);
        }
    }

    /**
     * Template with context.
     */
    public class Template5<C1, C2, C3, C4, C5> extends Templatable<Template5<C1, C2, C3, C4, C5>> {

        /**
         * Build without user data.
         */
        public Template5() {
            super();
        }

        /**
         * Build with user data.
         */
        public Template5(T data) {
            super(data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(C1 context1, C2 context2, C3 context3, C4 context4, C5 context5) {
            printDoc(this, selectPrinter(data), I.list(context1, context2, context3, context4, context5), data);
        }

        /**
         * Print the evaluated document.
         */
        public void print(String printer, C1 context1, C2 context2, C3 context3, C4 context4, C5 context5) {
            printDoc(this, printer, I.list(context1, context2, context3, context4, context5), data);
        }

        /**
         * Publish the evaluated document.
         * 
         * @param file
         */
        public void publish(File file, C1 context1, C2 context2, C3 context3, C4 context4, C5 context5) {
            publishDoc(this, file, I.list(context1, context2, context3, context4, context5), data);
        }
    }
}