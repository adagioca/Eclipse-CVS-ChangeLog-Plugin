/*
 * Copyright (c) John C. Landers All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * *****************************************************************************
 */
package org.eclipselabs.changelog;

import java.util.Date;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

final class ChangeLogFilter extends ViewerFilter {

    private static final String SEPARATOR = "|";

    private final String author;

    private final Date fromDate;

    private final Date toDate;

    private final String comment;

    private final boolean isOr;

    public ChangeLogFilter(String author, String comment, Date fromDate, Date toDate, boolean isOr) {
        this.author = author;
        this.comment = comment;
        this.fromDate = fromDate;
        this.toDate = toDate;
        this.isOr = isOr;
    }

    public ChangeLogFilter(String filter) {
        String[] split = filter.split("\\" + SEPARATOR);
        this.author = split[0];
        this.comment = split[1];

        String year = split[2];
        String month = split[3];
        String day = split[4];
        if (year.length() > 0) {
            this.fromDate = new Date(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } else {
            fromDate = null;
        }

        year = split[5];
        month = split[6];
        day = split[7];
        if (year.length() > 0) {
            this.toDate = new Date(Integer.parseInt(year), Integer.parseInt(month), Integer.parseInt(day));
        } else {
            toDate = null;
        }

        this.isOr = Boolean.parseBoolean(split[8]);
    }

    @Override
    public String toString() {
        StringBuilder ret = new StringBuilder();

        if (author != null) {
            ret.append(author);
        }
        ret.append(SEPARATOR);
        if (comment != null) {
            ret.append(comment);
        }
        ret.append(SEPARATOR);
        if (fromDate != null) {
            ret.append(fromDate.getYear());
        }
        ret.append(SEPARATOR);
        if (fromDate != null) {
            ret.append(fromDate.getMonth());
        }
        ret.append(SEPARATOR);
        if (fromDate != null) {
            ret.append(fromDate.getDate());
        }
        ret.append(SEPARATOR);
        if (toDate != null) {
            ret.append(toDate.getYear());
        }
        ret.append(SEPARATOR);
        if (toDate != null) {
            ret.append(toDate.getMonth());
        }
        ret.append(SEPARATOR);
        if (toDate != null) {
            ret.append(toDate.getDate());
        }
        ret.append(SEPARATOR);
        ret.append(isOr);

        return ret.toString();
    }

    /**
     * @see ViewerFilter#select(Viewer, Object, Object)
     */
    @Override
    public boolean select(Viewer aviewer, Object parentElement, Object element) {
        if (element instanceof ChangeLogEntry) {
            ChangeLogEntry entry = (ChangeLogEntry) element;
            if (isOr) {
                //empty fields should be considered a non-match
                return (hasAuthor() && authorMatch(entry)) || (hasDate() && dateMatch(entry)) || (hasComment() && commentMatch(entry));
            }
            //"and" search
            //empty fields should be considered a match
            return (!hasAuthor() || authorMatch(entry)) && (!hasDate() || dateMatch(entry)) && (!hasComment() || commentMatch(entry));
        }
        return false;
    }

    protected boolean authorMatch(ChangeLogEntry entry) {
        return entry.getAuthor().equals(author);
    }

    protected boolean commentMatch(ChangeLogEntry entry) {
        return !(entry.getComment().toLowerCase().indexOf(comment.toLowerCase()) == -1);
    }

    protected boolean dateMatch(ChangeLogEntry entry) {
        boolean from = true;
        if (fromDate != null) {
            from = fromDate.before(entry.getDate());
        }
        boolean to = true;
        if (toDate != null) {
            to = toDate.after(entry.getDate());
        }
        return from && to;
    }

    protected boolean hasAuthor() {
        return !author.equals("");
    }

    protected boolean hasComment() {
        return !comment.equals("");
    }

    protected boolean hasDate() {
        return fromDate != null || toDate != null;
    }

    public String getAuthor() {
        return author;
    }

    public String getComment() {
        return comment;
    }

    public Date getFromDate() {
        return fromDate;
    }

    public Date getToDate() {
        return toDate;
    }

    public boolean isOr() {
        return isOr;
    }
}
