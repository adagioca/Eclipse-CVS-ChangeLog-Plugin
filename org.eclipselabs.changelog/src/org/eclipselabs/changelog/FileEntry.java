/*
 * Created on 17/07/2009
 */
package org.eclipselabs.changelog;

import org.eclipse.core.resources.IResource;

/**
 * @author Julio Vilmar Gesser
 */
final class FileEntry {

    private final IResource resource;

    private final String reposFile;

    private final String revision;

    private final String prevRevision;

    public FileEntry(IResource resource, String reposFile, String revision, String prevRevision) {
        this.resource = resource;
        this.reposFile = reposFile;
        this.revision = revision;
        this.prevRevision = prevRevision;
    }

    public IResource getResource() {
        return resource;
    }

    public String getReposFile() {
        return reposFile;
    }

    public String getRevision() {
        return revision;
    }

    public String getPreviousRevision() {
        return prevRevision;
    }

    @Override
    public String toString() {
        return reposFile + " " + revision;
    }
}
