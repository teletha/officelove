/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 */
package officelove.word;

import java.util.ArrayList;
import java.util.List;

import psychopath.File;

public abstract class TemplateDoc {

    /**
     * Locate the template file.
     * 
     * @return
     */
    protected abstract File file();

    /**
     * Publish this template to the specified location.
     * 
     * @param file
     */
    public void publish(File file) {
        Word word = new Word(file());
        Word evaluated = word.evaluate(this);
        evaluated.save(file);
    }

    /**
     * Validate this template.
     * 
     * @return
     */
    public List<String> validate() {
        List<String> result = new ArrayList();
        Word word = new Word(file());

        return result;
    }
}
