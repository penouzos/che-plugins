/*******************************************************************************
 * Copyright (c) 2012-2015 Codenvy, S.A.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Codenvy, S.A. - initial API and implementation
 *******************************************************************************/
package org.eclipse.che.ide.ext.git.client.reset.files;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.eclipse.che.ide.ext.git.client.GitLocalizationConstant;
import org.eclipse.che.api.git.gwt.client.GitServiceClient;
import org.eclipse.che.api.git.shared.IndexFile;
import org.eclipse.che.api.git.shared.Status;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.app.CurrentProject;
import org.eclipse.che.ide.api.notification.NotificationManager;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.ext.git.client.GitOutputPartPresenter;
import org.eclipse.che.ide.rest.AsyncRequestCallback;
import org.eclipse.che.ide.rest.DtoUnmarshallerFactory;
import org.eclipse.che.ide.ui.dialogs.DialogFactory;

import java.util.ArrayList;
import java.util.List;

import static org.eclipse.che.api.git.shared.ResetRequest.ResetType;

/**
 * Presenter for resetting files from index.
 * <p/>
 * When user tries to reset files from index:
 * 1. Find Git work directory by selected item in browser tree.
 * 2. Get status for found work directory.
 * 3. Display files ready for commit in grid. (Checked items will be reseted from index).
 *
 * @author Ann Zhuleva
 */
@Singleton
public class ResetFilesPresenter implements ResetFilesView.ActionDelegate {
    private final GitOutputPartPresenter console;
    private final DtoFactory              dtoFactory;
    private final DtoUnmarshallerFactory  dtoUnmarshallerFactory;
    private       DialogFactory           dialogFactory;
    private       ResetFilesView          view;
    private       GitServiceClient        service;
    private       AppContext              appContext;
    private       GitLocalizationConstant constant;
    private       NotificationManager     notificationManager;
    private       CurrentProject          project;
    private       List<IndexFile>        indexedFiles;

    /** Create presenter. */
    @Inject
    public ResetFilesPresenter(ResetFilesView view,
                               GitServiceClient service,
                               AppContext appContext,
                               GitOutputPartPresenter console,
                               GitLocalizationConstant constant,
                               NotificationManager notificationManager,
                               DtoFactory dtoFactory,
                               DtoUnmarshallerFactory dtoUnmarshallerFactory,
                               DialogFactory dialogFactory) {
        this.view = view;
        this.console = console;
        this.dtoFactory = dtoFactory;
        this.dtoUnmarshallerFactory = dtoUnmarshallerFactory;
        this.dialogFactory = dialogFactory;
        this.view.setDelegate(this);
        this.service = service;
        this.appContext = appContext;
        this.constant = constant;
        this.notificationManager = notificationManager;
    }

    /** Show dialog. */
    public void showDialog() {
        project = appContext.getCurrentProject();

        service.status(project.getRootProject(),
                       new AsyncRequestCallback<Status>(dtoUnmarshallerFactory.newUnmarshaller(Status.class)) {
                           @Override
                           protected void onSuccess(Status result) {
                               if (result.isClean()) {
                                   dialogFactory.createMessageDialog(constant.messagesWarningTitle(), constant.indexIsEmpty(), null).show();
                                   return;
                               }

                               List<IndexFile> values = new ArrayList<>();
                               List<String> valuesTmp = new ArrayList<>();

                               valuesTmp.addAll(result.getAdded());
                               valuesTmp.addAll(result.getChanged());
                               valuesTmp.addAll(result.getRemoved());

                               for (String value : valuesTmp) {
                                   IndexFile indexFile = dtoFactory.createDto(IndexFile.class);
                                   indexFile.setPath(value);
                                   indexFile.setIndexed(true);
                                   values.add(indexFile);
                               }

                               if (values.isEmpty()) {
                                   dialogFactory.createMessageDialog(constant.messagesWarningTitle(), constant.indexIsEmpty(), null).show();
                                   return;
                               }

                               view.setIndexedFiles(values);
                               indexedFiles = values;
                               view.showDialog();
                           }

                           @Override
                           protected void onFailure(Throwable exception) {
                               String errorMassage = exception.getMessage() != null ? exception.getMessage() : constant.statusFailed();
                               console.printError(errorMassage);
                               notificationManager.notify(errorMassage, project.getRootProject());
                           }
                       });
    }

    /** {@inheritDoc} */
    @Override
    public void onResetClicked() {
        List<String> files = new ArrayList<>();
        for (IndexFile indexFile : indexedFiles) {
            if (!indexFile.isIndexed()) {
                files.add(indexFile.getPath());
            }
        }

        if (files.isEmpty()) {
            view.close();
            console.printInfo(constant.nothingToReset());
            notificationManager.notify(constant.nothingToReset(), project.getRootProject());
            return;
        }
        view.close();

        service.reset(project.getRootProject(), "HEAD", ResetType.MIXED, files, new AsyncRequestCallback<Void>() {
            @Override
            protected void onSuccess(Void result) {
                console.printInfo(constant.resetFilesSuccessfully());
                notificationManager.notify(constant.resetFilesSuccessfully(), project.getRootProject());
            }

            @Override
            protected void onFailure(Throwable exception) {
                String errorMassage = exception.getMessage() != null ? exception.getMessage() : constant.resetFilesFailed();
                console.printError(errorMassage);
                notificationManager.notify(errorMassage, project.getRootProject());
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void onCancelClicked() {
        view.close();
    }
}