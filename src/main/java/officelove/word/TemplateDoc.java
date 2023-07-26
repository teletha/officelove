/*
 * Copyright (C) 2023 Nameless Production Committee
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 */
package officelove.word;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import kiss.I;
import kiss.model.Model;
import psychopath.File;

public abstract class TemplateDoc<M1, M2, M3, M4, M5> {

    /** The context objects. */
    private List models = Collections.EMPTY_LIST;

    /**
     * Assign context models.
     * 
     * @param model1 A context model. (may be null)
     * @return Chainable API.
     */
    public final TemplateDoc<M1, M2, M3, M4, M5> context(M1 model1) {
        return context(model1, null, null, null, null);
    }

    /**
     * Assign context models.
     * 
     * @param model1 A context model. (may be null)
     * @param model2 A context model. (may be null)
     * @return Chainable API.
     */
    public final TemplateDoc<M1, M2, M3, M4, M5> context(M1 model1, M2 model2) {
        return context(model1, model2, null, null, null);
    }

    /**
     * Assign context models.
     * 
     * @param model1 A context model. (may be null)
     * @param model2 A context model. (may be null)
     * @param model3 A context model. (may be null)
     * @return Chainable API.
     */
    public final TemplateDoc<M1, M2, M3, M4, M5> context(M1 model1, M2 model2, M3 model3) {
        return context(model1, model2, model3, null, null);
    }

    /**
     * Assign context models.
     * 
     * @param model1 A context model. (may be null)
     * @param model2 A context model. (may be null)
     * @param model3 A context model. (may be null)
     * @param model4 A context model. (may be null)
     * @return Chainable API.
     */
    public final TemplateDoc<M1, M2, M3, M4, M5> context(M1 model1, M2 model2, M3 model3, M4 model4) {
        return context(model1, model2, model3, model4, null);
    }

    /**
     * Assign context models.
     * 
     * @param model1 A context model. (may be null)
     * @param model2 A context model. (may be null)
     * @param model3 A context model. (may be null)
     * @param model4 A context model. (may be null)
     * @param model5 A context model. (may be null)
     * @return Chainable API.
     */
    public final TemplateDoc<M1, M2, M3, M4, M5> context(M1 model1, M2 model2, M3 model3, M4 model4, M5 model5) {
        model1 = Objects.requireNonNullElseGet(model1, this::provideModel1);
        model2 = Objects.requireNonNullElseGet(model2, this::provideModel2);
        model3 = Objects.requireNonNullElseGet(model3, this::provideModel3);
        model4 = Objects.requireNonNullElseGet(model4, this::provideModel4);
        model5 = Objects.requireNonNullElseGet(model5, this::provideModel5);

        models = I.signal(model1, model2, model3, model4, model5).skipNull().toList();

        return this;
    }

    /**
     * Define the built-in model.
     * 
     * @return
     */
    protected M1 provideModel1() {
        return null;
    }

    /**
     * Define the built-in model.
     * 
     * @return
     */
    protected M2 provideModel2() {
        return null;
    }

    /**
     * Define the built-in model.
     * 
     * @return
     */
    protected M3 provideModel3() {
        return null;
    }

    /**
     * Define the built-in model.
     * 
     * @return
     */
    protected M4 provideModel4() {
        return null;
    }

    /**
     * Define the built-in model.
     * 
     * @return
     */
    protected M5 provideModel5() {
        return null;
    }

    /**
     * Publish this template to the specified location.
     * 
     * @param file
     */
    public final void publish(File file) {
        Word word = new Word(file());
        Word evaluated = word.evaluate(models);
        evaluated.save(file);
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
     * Locate the template file.
     * 
     * @return
     */
    protected abstract File file();

    /**
     * Collect context types.
     * 
     * @return
     */
    protected List<Class> types() {
        return I.signal(Model.collectParameters(getClass(), TemplateDoc.class))
                .as(Class.class)
                .skip(Object.class)
                .skip(Void.class)
                .toList();
    }
}
