/*
 * Created on 07/07/2009
 */
package org.eclipselabs.changelog;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * @author Julio Vilmar Gesser
 */
public final class ChangeLogPreferences extends PreferencePage implements IWorkbenchPreferencePage {

    public static final String DISPLAY_PATH_PREFERENCE = "net.sf.cvschangelog.display_path";

    public static final int OPT_PROJECT_RELATIVE_PATH = 0;

    public static final int OPT_CVS_FULL_PATH = 1;

    public static final int OPT_FILE_NAME = 2;

    public ChangeLogPreferences() {
    }

    private Button radioProjectPath;

    private Button radioCVSPath;

    private Button radioFileName;

    public void init(IWorkbench workbench) {
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        GridLayout layout = new GridLayout();
        layout.numColumns = 1;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        Group group = new Group(composite, SWT.FLAT);
        group.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        group.setLayout(new GridLayout(1, false));
        group.setText("File path display");

        radioProjectPath = new Button(group, SWT.RADIO);
        radioProjectPath.setText("Project relative path");

        radioCVSPath = new Button(group, SWT.RADIO);
        radioCVSPath.setText("CVS full path");

        radioFileName = new Button(group, SWT.RADIO);
        radioFileName.setText("File name");

        int sel = getPreferenceStore().getInt(DISPLAY_PATH_PREFERENCE);

        switch (sel) {
            case OPT_PROJECT_RELATIVE_PATH:
                radioProjectPath.setSelection(true);
                break;
            case OPT_CVS_FULL_PATH:
                radioCVSPath.setSelection(true);
                break;
            case OPT_FILE_NAME:
                radioFileName.setSelection(true);
                break;
        }

        return composite;
    }

    @Override
    protected IPreferenceStore doGetPreferenceStore() {
        return CVSChangeLogPlugin.getDefault().getPreferenceStore();
    }

    @Override
    public boolean performOk() {
        int option;
        if (radioCVSPath.getSelection()) {
            option = OPT_CVS_FULL_PATH;
        } else if (radioFileName.getSelection()) {
            option = OPT_FILE_NAME;
        } else {
            option = OPT_PROJECT_RELATIVE_PATH;
        }

        IPreferenceStore store = getPreferenceStore();
        store.setValue(DISPLAY_PATH_PREFERENCE, option);
        return true;
    }

}
