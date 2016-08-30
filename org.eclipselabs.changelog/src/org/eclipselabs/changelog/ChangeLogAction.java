/*
 * Copyright (c) John C. Landers 
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 *******************************************************************************/
package org.eclipselabs.changelog;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.action.IAction;
import org.eclipse.team.internal.ccvs.ui.actions.WorkspaceAction;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;

@SuppressWarnings("restriction")
public final class ChangeLogAction extends WorkspaceAction implements ISchedulingRule {

    private final LogEntryCache lec = new RemoteLogOperation.LogEntryCache();

    @Override
    public void execute(IAction action) {
        ChangeLogView view = (ChangeLogView) showView(ChangeLogView.VIEW_ID);

        ChangeLogJob job = new ChangeLogJob("ChangeLog");
        view.setChangeLogJob(job);

        job.setView(view);
        job.setLogEntryCache(lec);
        job.setResources(getAllSelectedResources());
        job.schedule();
    }

    private IResource[] getAllSelectedResources() {
        if (this.getSelection().isEmpty()) {
            return new IResource[0];
        }
        List<IResource> resources = new ArrayList<IResource>();
        for (IResource resource : getSelectedResources()) {
            if ((resource.getType() == IResource.PROJECT) || (resource.getType() == IResource.FOLDER)) {
                try {
                    resource.accept(new ResourceVisitor(resources));
                } catch (CoreException e) {
                    CVSChangeLogPlugin.log(e);
                }
            } else {
                resources.add(resource);
            }
        }
        return resources.toArray(new IResource[resources.size()]);
    }

    private static class ResourceVisitor implements IResourceVisitor {

        List<IResource> lst = null;

        public ResourceVisitor(List<IResource> lst) {
            this.lst = lst;
        }

        public boolean visit(IResource resource) throws CoreException {
            if (resource.getType() == IResource.PROJECT) {
                return true;
            } else if (resource.getType() == IResource.FOLDER) {
                return true;
            } else if (resource.getType() == IResource.FILE) {
                this.lst.add(resource);
                return false;
            }
            return false;
        }
    }

    public boolean contains(ISchedulingRule rule) {
        return false;
    }

    public boolean isConflicting(ISchedulingRule rule) {
        if (rule instanceof ChangeLogAction) {
            return true;
        }
        return false;
    }

}
