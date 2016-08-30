/*
 * Created on Aug 4, 2004
 * 
 * TODO To change the template for this generated file go to Window - Preferences - Java - Code Style - Code Templates
 */
package org.eclipselabs.changelog;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.eclipse.core.resources.IResource;

final class ChangeLogEntry {

    private final String author;

    private final String comment;

    private final Date date;

    private final List<FileEntry> files = new ArrayList<FileEntry>();

    public ChangeLogEntry(String author, String comment, Date date) {
        this.author = author;
        this.comment = comment;
        this.date = date;
    }

    @Override
    public String toString() {
        StringBuffer ret = new StringBuffer();
        ret.append("Author: ").append(author).append("\n");
        ret.append("Date: ").append(date).append("\n\n");
        ret.append("* ").append(comment).append("\n\n");
        ret.append("Affected Files:\n");
        for (FileEntry filePath : files) {
            ret.append(filePath).append("\n");
        }
        return ret.toString();
    }

    public void addFile(IResource resource, String reposFile, String revision, String prevRevision) {
        files.add(new FileEntry(resource, reposFile, revision, prevRevision));
    }

    /**
     * @return Returns the author.
     */
    public String getAuthor() {
        return author;
    }

    /**
     * @return Returns the comment.
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return Returns the date.
     */
    public Date getDate() {
        return date;
    }

    /**
     * @return Returns the files.
     */
    public List<FileEntry> getFiles() {
        return files;
    }

}
