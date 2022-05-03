/*
 * Copyright (C) 2020 offishell Development Team
 *
 * Licensed under the MIT License (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *          https://opensource.org/licenses/MIT
 */
package officeman.model;

import java.nio.file.Path;
import java.util.HashMap;

import kiss.I;
import kiss.Managed;
import kiss.Singleton;
import kiss.Storable;

@SuppressWarnings("serial")
@Managed(value = Singleton.class)
public class Workspace extends HashMap<String, Path> implements Storable<Workspace> {

    /**
    * 
    */
    private Workspace() {
        restore();
    }

    /**
     * @param category
     * @return
     */
    public static Path by(String category) {
        Workspace dir = I.make(Workspace.class).restore();
        Path path = Path.of("workspace/" + category);
        dir.store();

        return path;
    }
}