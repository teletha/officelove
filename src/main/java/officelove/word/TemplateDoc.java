/*
 * Copyright (C) 2022 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          http://opensource.org/licenses/mit-license.php
 */
package officelove.word;

import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import kiss.I;
import kiss.WiseSupplier;
import kiss.XML;
import psychopath.File;
import psychopath.Locator;

public class TemplateDoc {

    /** The path to orinal template. */
    private final String path;

    /** The original template. */
    private final XML root;

    /**
     * Build template.
     * 
     * @param filePath A template file.
     */
    public TemplateDoc(String filePath) {
        this(Locator.file(filePath));
    }

    /**
     * Build template.
     * 
     * @param file A template file.
     */
    public TemplateDoc(Path file) {
        this(Locator.file(file));
    }

    /**
     * Build template.
     * 
     * @param file A template file.
     */
    public TemplateDoc(URL file) {
        this(URLDecoder.decode(file.toString(), StandardCharsets.UTF_8), file::openStream);
    }

    /**
     * Build template.
     * 
     * @param file A template file.
     */
    public TemplateDoc(File file) {
        this(file.toString(), file::newInputStream);
    }

    /**
     * Build template.
     * 
     * @param filePath A file name.
     * @param input A file input.
     */
    private TemplateDoc(String filePath, WiseSupplier<InputStream> input) {
        this.path = filePath;
        this.root = I.xml(input.get());

        parse();
    }

    /**
     * Parse template.
     */
    private void parse() {
        for (XML start : root.find("office|annotation")) {
            String name = start.attr("office:name");
            System.out.println(name);
            XML end = root.find("office|annotation-end[office:name=\"" + name + "\"]");
            System.out.println(end);
            System.out.println(start.parent().equals(end.parent()));
        }
    }

    /**
     * Get all expression in this template.
     * 
     * @return
     */
    public List<DocExpression> getExpression() {
        return List.of();
    }
}
