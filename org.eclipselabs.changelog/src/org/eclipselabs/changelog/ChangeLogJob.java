/*
 * Created on 12/07/2009
 */
package org.eclipselabs.changelog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ILogEntry;
import org.eclipse.team.internal.ccvs.core.filehistory.CVSFileRevision;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;

/**
 * @author Julio Vilmar Gesser
 */
@SuppressWarnings("restriction")
final class ChangeLogJob extends Job {

    private static final Comparator<ILogEntry> LOG_ENTRY_COMPARATOR = new Comparator<ILogEntry>() {

        public int compare(ILogEntry o1, ILogEntry o2) {
            ILogEntry rev1 = o1;
            ILogEntry rev2 = o2;
            return rev1.getDate().compareTo(rev2.getDate());
        }
    };

    private static final long MAX_TIME_DIF = 3l * 60l * 1000l; //min * seg * milis = 3 min

    private LogEntryCache lec;

    private ChangeLogView view = null;

    private IResource[] resources;

    public ChangeLogJob(String name) {
        super(name);
    }

    public void setResources(IResource[] resources) {
        this.resources = resources;
    }

    public void setView(ChangeLogView view) {
        this.view = view;
    }

    public void setLogEntryCache(LogEntryCache lec) {
        this.lec = lec;
    }

    private Status displayErrorMessage(Exception e) {
        return CVSChangeLogPlugin.createErrorStatus(e);
    }

    @Override
    protected IStatus run(IProgressMonitor monitor) {
        try {
            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            List<ICVSRemoteResource> remoteResources = new ArrayList<ICVSRemoteResource>();
            for (IResource resource : resources) {
                ICVSRemoteResource remote = CVSWorkspaceRoot.getRemoteResourceFor(resource);
                if (remote != null) {
                    remoteResources.add(remote);
                }
            }

            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            ICVSRemoteResource[] remoteArray = remoteResources.toArray(new ICVSRemoteResource[remoteResources.size()]);
            
            CVSTag startTag = createStartTag();
            CVSTag endTag = createEndTag();
            RemoteLogOperation rLogOperation = new RemoteLogOperation(view, remoteArray, startTag, endTag, lec);
            rLogOperation.execute(monitor);

            Map<String, Map<String, Map<ILogEntry, IResource>>> logEntries = new HashMap<String, Map<String, Map<ILogEntry, IResource>>>();

            for (IResource resource : resources) {
                loadLogEntriesorResource(resource, logEntries);
                if (monitor.isCanceled()) {
                    throw new OperationCanceledException();
                }
            }
            final List<ChangeLogEntry> changeLogEntries = buildChangeLog(logEntries);

            if (monitor.isCanceled()) {
                throw new OperationCanceledException();
            }

            view.showChangeLog(changeLogEntries);

        } catch (CVSException e) {
            return (displayErrorMessage(e));
        } catch (TeamException e) {
            return (displayErrorMessage(e));
        } catch (OperationCanceledException ex) {
            return new Status(IStatus.CANCEL, CVSChangeLogPlugin.getDefault().getBundle().getSymbolicName(), "Cancelling CVS ChangeLog operation.");
        } catch (InterruptedException e) {
            return (displayErrorMessage(e));
        }
        return new Status(IStatus.OK, CVSChangeLogPlugin.getDefault().getBundle().getSymbolicName(), "Finished");
    }

    private CVSTag createStartTag() {
        ChangeLogFilter filter = view.getFilter();
        if (filter == null) {
            return null;
        }
        Date startDate = filter.getFromDate();
        if (startDate == null) {
            startDate = filter.getToDate() == null ? null : new Date(0); 
        }
        return startDate == null ? null : new CVSTag(startDate);
    }

    private CVSTag createEndTag() {
        ChangeLogFilter filter = view.getFilter();
        if (filter == null) {
            return null;
        }
        Date endDate = filter.getToDate();
        if (endDate == null) {
            endDate = filter.getFromDate() == null ? null : new Date();
        }
        return endDate == null ? null : new CVSTag(endDate);
    }
    
    private List<ChangeLogEntry> buildChangeLog(Map<String, Map<String, Map<ILogEntry, IResource>>> authors) {

        /*
         *           Map:changeLog
         *            /         \
         *  ChangeGroupKey    Map:files
         *  (date,comment)   /         \
         *               ILogEntry   IResource
         */
        Map<ChangeGroupKey, Map<ILogEntry, IResource>> changeLog = new HashMap<ChangeGroupKey, Map<ILogEntry, IResource>>();

        for (Map<String, Map<ILogEntry, IResource>> msgs : authors.values()) {
            for (Map<ILogEntry, IResource> entriesMap : msgs.values()) {

                List<ILogEntry> entries = new ArrayList<ILogEntry>(entriesMap.keySet());
                Collections.sort(entries, LOG_ENTRY_COMPARATOR);

                ILogEntry previousRev = null;
                Map<ILogEntry, IResource> files = new HashMap<ILogEntry, IResource>();

                // seconds times milliseconds.
                for (ILogEntry entry : entries) {
                    if (previousRev != null) {
                        if ((entry.getDate().getTime() - previousRev.getDate().getTime()) > MAX_TIME_DIF) {
                            files = new HashMap<ILogEntry, IResource>();
                            changeLog.put(new ChangeGroupKey(entry), files);
                        }
                    } else {
                        changeLog.put(new ChangeGroupKey(entry), files);
                    }
                    files.put(entry, entriesMap.get(entry));
                    previousRev = entry;
                }
            }
        }

        List<ChangeLogEntry> changeLogEntries = new ArrayList<ChangeLogEntry>();

        for (Map<ILogEntry, IResource> revs : changeLog.values()) {
            ChangeLogEntry entry = null;
            for (Entry<ILogEntry, IResource> revEntry : revs.entrySet()) {
                ILogEntry rev = revEntry.getKey();
                IResource resource = revEntry.getValue();
                if (entry == null) {
                    entry = new ChangeLogEntry(rev.getAuthor(), rev.getComment(), rev.getDate());
                    changeLogEntries.add(entry);
                }

                String prevRevision = "";
                ICVSRemoteFile prevRemoteFile;
                try {
                    prevRemoteFile = lec.getImmediatePredecessor(rev.getRemoteFile());
                    if (prevRemoteFile != null) {
                        prevRevision = prevRemoteFile.getRevision();
                    }
                } catch (TeamException e) {
                    CVSChangeLogPlugin.log(e);
                }
                entry.addFile(resource, rev.getRemoteFile().getRepositoryRelativePath(), rev.getRevision(), prevRevision);

            }
        }
        return changeLogEntries;
    }

    private boolean isSameBranch(String localRevision, String remoteRevision) {
        int localDigits[] = Util.convertToDigits(localRevision);
        if (localDigits.length == 0) {
            return false;
        }
        int remoteDigits[] = Util.convertToDigits(remoteRevision);
        if (remoteDigits.length == 0) {
            return false;
        }

        if (remoteDigits.length > localDigits.length) {
            return false;
        }
        boolean retval = true;
        int i;
        for (i = 0; i < (remoteDigits.length - 1); i++) {
            if (remoteDigits[i] != localDigits[i]) {
                retval = false;
            }
        }
        if (remoteDigits[i] > localDigits[i]) {
            retval = false;
        }
        return retval;
    }

    /**
     * The returning map follows the structure below:
     * 
     * <pre>
     *           Map:authors
     *         /             \
     *   String:name      Map:commits
     *                   /            \
     *             String:comment   Map:files
     *                              /       \
     *                       ILogEntry   IResource
     *</pre>
     */
    private void loadLogEntriesorResource(IResource resource, Map<String, Map<String, Map<ILogEntry, IResource>>> authors) throws CVSException, TeamException {
        ICVSRemoteResource remote = CVSWorkspaceRoot.getRemoteResourceFor(resource);
        if (remote == null) {
            return;
        }
        ResourceSyncInfo syncInfo = remote.getSyncInfo();

        ILogEntry entries[] = lec.getLogEntries(remote);
        if (entries != null) {
            for (ILogEntry entry : entries) {
                if (syncInfo == null || isSameBranch(syncInfo.getRevision(), entry.getRevision())) {
                    Map<String, Map<ILogEntry, IResource>> comments = authors.get(entry.getAuthor());
                    if (comments == null) {
                        comments = new HashMap<String, Map<ILogEntry, IResource>>();
                        authors.put(entry.getAuthor(), comments);
                    }
                    Map<ILogEntry, IResource> list = comments.get(entry.getComment());
                    if (list == null) {
                        list = new HashMap<ILogEntry, IResource>();
                        comments.put(entry.getComment(), list);
                    }
                    list.put(entry, resource);
                }
            }
        }
    }

    IFileRevision[] getFileRevisions(FileEntry entry) {
        IFileRevision[] revisions = new IFileRevision[2];

        ICVSRemoteResource remote;
        try {
            remote = CVSWorkspaceRoot.getRemoteResourceFor(entry.getResource());
        } catch (CVSException e) {
            throw new RuntimeException(e);
        }
        ILogEntry[] logEntries = lec.getLogEntries(remote);
        for (ILogEntry logEntry : logEntries) {
            if (logEntry.getRevision().equals(entry.getRevision())) {
                revisions[0] = new CVSFileRevision(logEntry);
            } else if (logEntry.getRevision().equals(entry.getPreviousRevision())) {
                revisions[1] = new CVSFileRevision(logEntry);
            }
            if (revisions[0] != null && revisions[1] != null) {
                break;
            }
        }
        return revisions;
    }

    private static class ChangeGroupKey {

        private final Date date;

        private final String comment;

        public ChangeGroupKey(ILogEntry entry) {
            this(entry.getDate(), entry.getComment());
        }

        public ChangeGroupKey(Date date, String comment) {
            super();
            this.date = date;
            this.comment = comment;
        }

        @Override
        public int hashCode() {
            return date.hashCode() + comment.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            ChangeGroupKey other = (ChangeGroupKey) obj;
            return date.equals(other.date) && comment.equals(other.comment);
        }

    }
}
