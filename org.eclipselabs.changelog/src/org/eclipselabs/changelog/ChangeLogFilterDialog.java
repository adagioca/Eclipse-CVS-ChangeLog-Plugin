/*
 * Copyright (c) John C. Landers All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * *****************************************************************************
 */
package org.eclipselabs.changelog;

import java.util.Calendar;
import java.util.Date;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.DateTime;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

/**
 * @author jcl To change the template for this generated type comment go to Window - Preferences - Java - Code
 *         Generation - Code and Comments
 */
final class ChangeLogFilterDialog extends Dialog {

    private ChangeLogFilter filter;

    //widgets
    private Button orRadio;

    private Button andRadio;

    private DateTime fromDateEditor;

    private Button disableFromDateCheck;

    private DateTime toDateEditor;

    private Button disableToDateCheck;
 
    private Text author;

    private Text comment;

    public ChangeLogFilterDialog(ChangeLogView view) {
        super(view.getViewSite().getShell());
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("ChangeLog Filter Dialog");
    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite topLevel = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = convertVerticalDLUsToPixels(IDialogConstants.VERTICAL_MARGIN);
        layout.marginWidth = convertHorizontalDLUsToPixels(IDialogConstants.HORIZONTAL_MARGIN);
        topLevel.setLayout(layout);
        //"and" and "or" search radio buttons
        Label label = new Label(topLevel, SWT.NONE);
        GridData data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        label.setLayoutData(data);
        label.setText("Matching");
        andRadio = new Button(topLevel, SWT.RADIO);
        andRadio.setText("Matching All");
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        andRadio.setLayoutData(data);
        andRadio.setSelection(true);
        orRadio = new Button(topLevel, SWT.RADIO);
        orRadio.setText("Matching Any");
        data = new GridData(GridData.FILL_HORIZONTAL);
        data.horizontalSpan = 2;
        orRadio.setLayoutData(data);
        //author
        label = new Label(topLevel, SWT.NONE);
        label.setText("Author");
        author = new Text(topLevel, SWT.BORDER);
        author.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //comment
        label = new Label(topLevel, SWT.NONE);
        label.setText("Comment");
        comment = new Text(topLevel, SWT.BORDER);
        comment.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        //"from" date
        label = new Label(topLevel, SWT.NONE);
        label.setText("From Date");

        GridLayout fdLayout = new GridLayout();
        fdLayout.numColumns = 2;
        fdLayout.marginHeight = 3;
        fdLayout.marginWidth = 3;
        Composite fdComposite = new Composite(topLevel, SWT.NONE);
        fdComposite.setLayout(fdLayout);
        fromDateEditor = new DateTime(fdComposite, SWT.NONE);
        disableFromDateCheck = new Button(fdComposite, SWT.CHECK);
        disableFromDateCheck.setText("Ignore");
        disableFromDateCheck.addSelectionListener(new SelectionListener() {
            
            public void widgetSelected(SelectionEvent e) {
                fromDateEditor.setEnabled(!disableFromDateCheck.getSelection());
            }
            
            public void widgetDefaultSelected(SelectionEvent e) {              
                fromDateEditor.setEnabled(!disableFromDateCheck.getSelection());
            }
        });
        //"to" date
        label = new Label(topLevel, SWT.NONE);
        label.setText("To Date");
        
        GridLayout tdLayout = new GridLayout();

        tdLayout.numColumns = 2;
        tdLayout.marginHeight = 3;
        tdLayout.marginWidth = 3;
        Composite tdComposite = new Composite(topLevel, SWT.NONE);
        tdComposite.setLayout(tdLayout);
        toDateEditor = new DateTime(tdComposite, SWT.NONE);
        disableToDateCheck = new Button(tdComposite, SWT.CHECK);
        disableToDateCheck.setText("Ignore");
        disableToDateCheck.addSelectionListener(new SelectionListener() {
            
            public void widgetSelected(SelectionEvent e) {
                toDateEditor.setEnabled(!disableToDateCheck.getSelection());
            }
            
            public void widgetDefaultSelected(SelectionEvent e) {
                toDateEditor.setEnabled(!disableToDateCheck.getSelection());
            }
        });
     
        initializeValues();
        Dialog.applyDialogFont(parent);
        return topLevel;
    }

    void initializeValues() {
        if (filter == null) {
            return;
        }
        if (filter.getAuthor() != null) {
            author.setText(filter.getAuthor());
        }
        if (filter.getComment() != null) {
            comment.setText(filter.getComment());
        }
        orRadio.setSelection(filter.isOr());
        andRadio.setSelection(!filter.isOr());
        Calendar calendar = Calendar.getInstance();
        if (filter.getFromDate() != null) {
            calendar.setTime(filter.getFromDate());
            fromDateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        } else {
            disableFromDateCheck.setSelection(true);
            fromDateEditor.setEnabled(false);
        }
        if (filter.getToDate() != null) {
            calendar.setTime(filter.getToDate());
            toDateEditor.setDate(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        } else {
            disableToDateCheck.setSelection(true);
            toDateEditor.setEnabled(false);
        }
    }

    /**
     * A button has been pressed. Process the dialog contents.
     */
    @Override
    protected void buttonPressed(int buttonId) {
        if (IDialogConstants.CANCEL_ID == buttonId) {
            super.buttonPressed(buttonId);
            return;
        }
        Date fromDate = null;
        if (!disableFromDateCheck.getSelection()) {
            //set the calendar with the user input
            //set the hours, minutes and seconds to 00
            //so as to cover the whole day
            Calendar calendar = Calendar.getInstance();
            calendar.set(fromDateEditor.getYear(), fromDateEditor.getMonth(), fromDateEditor.getDay(), 00, 00, 00);
            fromDate = calendar.getTime();
        }
        Date toDate = null;
        if (!disableToDateCheck.getSelection()) {
            //set the calendar with the user input
            //set the hours, minutes and seconds to 23, 59, 59
            //so as to cover the whole day
            Calendar calendar = Calendar.getInstance();
            calendar.set(toDateEditor.getYear(), toDateEditor.getMonth(), toDateEditor.getDay(), 23, 59, 59);
            toDate = calendar.getTime();
        }
        if ((toDate != null && fromDate != null) && toDate.before(fromDate)) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(toDate);
        }
        //create the filter
        filter = new ChangeLogFilter(author.getText(), comment.getText(), fromDate, toDate, orRadio.getSelection());
        super.buttonPressed(buttonId);
    }

    /**
     * Returns the filter that was created from the provided user input.
     */
    public ChangeLogFilter getFilter() {
        return filter;
    }

    /**
     * Set the intial value of the dialog to the given filter.
     */
    public void setFilter(ChangeLogFilter filter) {
        this.filter = filter;
    }
}
