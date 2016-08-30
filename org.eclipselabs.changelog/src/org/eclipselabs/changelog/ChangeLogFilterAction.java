/*
 * Copyright (c) John C. Landers All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * *****************************************************************************
 */
package org.eclipselabs.changelog;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.window.Window;
import org.eclipse.ui.IActionDelegate;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;

public final class ChangeLogFilterAction implements IViewActionDelegate {

    private ChangeLogView view;

    public void init(IViewPart viewPart) {
        this.view = (ChangeLogView) viewPart;
    }

    /**
     * @see IActionDelegate#run(IAction)
     */
    public void run(IAction action) {
        ChangeLogFilterDialog dialog = new ChangeLogFilterDialog(view);

        ChangeLogFilter filter = view.getFilter();

        if (filter != null) {
            dialog.setFilter(filter);
        }
        if (dialog.open() == Window.CANCEL) {
            return;
        }

        filter = dialog.getFilter();

        view.setFilter(filter);
    }

    /**
     * @see IActionDelegate#selectionChanged(IAction, ISelection)
     */
    public void selectionChanged(IAction action, ISelection selection) {
    }
}
