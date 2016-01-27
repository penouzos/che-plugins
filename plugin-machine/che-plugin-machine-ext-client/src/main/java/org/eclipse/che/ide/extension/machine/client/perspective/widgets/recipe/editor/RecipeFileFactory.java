/*******************************************************************************
 * Copyright (c) 2012-2016 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.extension.machine.client.perspective.widgets.recipe.editor;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.ide.dto.DtoFactory;

import javax.validation.constraints.NotNull;

/**
 * The factory that provides an ability to create instances of {@link RecipeFile}. The main idea of this class is to simplify work flow of
 * using  {@link RecipeFile}.
 *
 * @author Valeriy Svydenko
 */
@Singleton
public class RecipeFileFactory {

    public static final String NAME = "Dockerfile";
    public static final String PATH = "machine_recipe";
    public static final String TYPE = "text/x-dockerfile";

    private final DtoFactory dtoFactory;

    @Inject
    public RecipeFileFactory(DtoFactory dtoFactory) {
        this.dtoFactory = dtoFactory;
    }

    /**
     * Create a new instance of {@link RecipeFile} for a given href.
     *
     * @param content
     *         script of the recipe
     * @return an instance of {@link RecipeFile}
     * @throws IllegalStateException
     *         when no project is opened
     */
    @NotNull
    public RecipeFile newInstance(@NotNull String content) {
        return newInstance(content, NAME, PATH);
    }

    @NotNull
    private RecipeFile newInstance(@NotNull String content, @NotNull String name, @NotNull String path) {
        ItemReference recipeFileItem = dtoFactory.createDto(ItemReference.class)
                                                 .withName(name)
                                                 .withPath(path)
                                                 .withMediaType(TYPE);

        return new RecipeFile(content, recipeFileItem);
    }

}
