/*
 * Copyright (c) John C. Landers All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * *****************************************************************************
 */
package org.eclipselabs.changelog;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableLayout;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.internal.ui.history.GenericHistoryViewDefaultPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.internal.WorkbenchPlugin;
import org.eclipse.ui.part.PageSite;
import org.eclipse.ui.part.ViewPart;

/**
 * @author jcl To change the template for this generated type comment go to
 *         Window - Preferences - Java - Code Generation - Code and Comments
 */
@SuppressWarnings("restriction")
public final class ChangeLogView extends ViewPart implements IPropertyChangeListener {

    private static final String FILE_REVISION_SEPARATOR = " ";

    public static final String VIEW_ID = ChangeLogView.class.getName();

    private static final String PREFERENCE_FILTER = VIEW_ID + ".filter";

    protected static final String PREFERENCE_SHOW_FILES_GRID = VIEW_ID + ".showFilesGrid";

    protected static final String PREFERENCE_SHOW_COMMENTS = VIEW_ID + ".showComments";

    protected static final String PREFERENCE_WRAP_COMMENTS = VIEW_ID + ".wrapComments";

    protected TreeViewer datesTreeViewer;

    protected SashForm detailSashForm;

    protected SashForm filesSashForm;

    protected TreeViewer filesTreeViewer;

    protected TextViewer filesTextViewer;

    protected TextViewer commentTextViewer;

    private ChangeLogJob changeLogJob = null;

    private Action refreshAction;

    private Action copyAction;

    protected MenuItem compareMenu;

    protected MenuItem openMenu;

    protected MenuItem copyMenu;

    private MenuItem copyAllMenu;

    protected int fileDisplayMode;

    private Action wrapCommentsAction;

    private Action showCommentsAction;

    private Action showFilesAsPlainTextAction;

    private Action showFilesAsGridAction;

    public ChangeLogView() {
        IPreferenceStore store = CVSChangeLogPlugin.getDefault().getPreferenceStore();
        store.addPropertyChangeListener(this);
        fileDisplayMode = store.getInt(ChangeLogPreferences.DISPLAY_PATH_PREFERENCE);
    }

    @Override
    public void dispose() {
        CVSChangeLogPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
        super.dispose();
    }

    public void propertyChange(PropertyChangeEvent event) {
        if (ChangeLogPreferences.DISPLAY_PATH_PREFERENCE.equals(event.getProperty())) {
            fileDisplayMode = ((Number) event.getNewValue()).intValue();
            filesTreeViewer.refresh();
            updateFilesText((Collection<FileEntry>) filesTreeViewer.getInput());
        }
    }

    @Override
    public void createPartControl(Composite parent) {
        SashForm mainSashForm = new SashForm(parent, SWT.VERTICAL);
        mainSashForm.setLayoutData(new GridData(GridData.FILL_BOTH));
        datesTreeViewer = createDatesTree(mainSashForm);
        detailSashForm = new SashForm(mainSashForm, SWT.HORIZONTAL);
        mainSashForm.setWeights(new int[] { 50, 50 });

        filesSashForm = new SashForm(detailSashForm, SWT.NONE);

        filesTreeViewer = createFilesTree(filesSashForm);
        filesTextViewer = new TextViewer(filesSashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
        filesTextViewer.setDocument(new Document());

        commentTextViewer = new TextViewer(detailSashForm, SWT.H_SCROLL | SWT.V_SCROLL | SWT.MULTI | SWT.READ_ONLY);
        commentTextViewer.setDocument(new Document());

        detailSashForm.setWeights(new int[] { 60, 40 });

        Menu menu = createMenu(parent);
        filesTreeViewer.getTree().setMenu(menu);

        contributeToToolBar();
        contributeToMenuBar();

        reconfigure();
    }

    protected void reconfigure() {
        commentTextViewer.getTextWidget().setWordWrap(wrapCommentsAction.isChecked());
        filesSashForm.setMaximizedControl(showFilesAsGridAction.isChecked() ? filesTreeViewer.getControl() : filesTextViewer.getControl());
        detailSashForm.setMaximizedControl(showCommentsAction.isChecked() ? null : filesSashForm);
    }

    private Menu createMenu(Composite parent) {
        Menu menu = new Menu(parent);

        compareMenu = new MenuItem(menu, SWT.DEFAULT);
        compareMenu.setText("Compare Revisions");
        compareMenu.setEnabled(false);
        compareMenu.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                for (FileEntry entry : (List<FileEntry>) ((IStructuredSelection) filesTreeViewer.getSelection()).toList()) {
                    openFileInCompare(entry);
                }
            }
        });

        openMenu = new MenuItem(menu, SWT.NONE);
        openMenu.setText("Open");
        openMenu.setEnabled(false);
        openMenu.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                for (FileEntry entry : (List<FileEntry>) ((IStructuredSelection) filesTreeViewer.getSelection()).toList()) {
                    openFileInEditor(entry);
                }
            }
        });

        copyMenu = new MenuItem(menu, SWT.NONE);
        copyMenu.setImage(CVSChangeLogPlugin.getDefault().getImageRegistry().get(CVSChangeLogPlugin.IMG_COPY_ENABLED));
        copyMenu.setText("Copy Selected");
        copyMenu.setEnabled(false);
        copyMenu.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                copyToClipBoard(false);
            }
        });

        copyAllMenu = new MenuItem(menu, SWT.NONE);
        copyAllMenu.setImage(CVSChangeLogPlugin.getDefault().getImageRegistry().get(CVSChangeLogPlugin.IMG_COPY_ENABLED));
        copyAllMenu.setText("Copy All");
        copyAllMenu.setEnabled(false);
        copyAllMenu.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                copyToClipBoard(true);
            }
        });

        return menu;
    }

    private void contributeToMenuBar() {
        final IPreferenceStore store = CVSChangeLogPlugin.getDefault().getPreferenceStore();

        IMenuManager mm = getViewSite().getActionBars().getMenuManager();

        showFilesAsGridAction = new Action("Show files as Grid", Action.AS_RADIO_BUTTON) {

            @Override
            public void run() {
                reconfigure();
                store.setValue(PREFERENCE_SHOW_FILES_GRID, isChecked());
            }
        };

        showFilesAsPlainTextAction = new Action("Show files as Plain Text", Action.AS_RADIO_BUTTON) {

            @Override
            public void run() {
                reconfigure();
                store.setValue(PREFERENCE_SHOW_FILES_GRID, !isChecked());
            }
        };

        showCommentsAction = new Action("Show Comments", Action.AS_CHECK_BOX) {

            @Override
            public void run() {
                reconfigure();
                store.setValue(PREFERENCE_SHOW_COMMENTS, isChecked());
            }
        };

        wrapCommentsAction = new Action("Wrap Comments", Action.AS_CHECK_BOX) {

            @Override
            public void run() {
                reconfigure();
                store.setValue(PREFERENCE_WRAP_COMMENTS, isChecked());
            }
        };

        mm.add(showFilesAsGridAction);
        mm.add(showFilesAsPlainTextAction);
        mm.add(new Separator());
        mm.add(showCommentsAction);
        mm.add(wrapCommentsAction);

        store.setDefault(PREFERENCE_SHOW_FILES_GRID, true);
        store.setDefault(PREFERENCE_SHOW_COMMENTS, true);
        store.setDefault(PREFERENCE_WRAP_COMMENTS, true);

        if (store.getBoolean(PREFERENCE_SHOW_FILES_GRID)) {
            showFilesAsGridAction.setChecked(true);
        } else {
            showFilesAsPlainTextAction.setChecked(true);
        }
        showCommentsAction.setChecked(store.getBoolean(PREFERENCE_SHOW_COMMENTS));
        wrapCommentsAction.setChecked(store.getBoolean(PREFERENCE_WRAP_COMMENTS));
    }

    private void contributeToToolBar() {

        // Create the local tool bar
        IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

        ImageRegistry imageRegistry = CVSChangeLogPlugin.getDefault().getImageRegistry();

        copyAction = new Action("Copy", imageRegistry.getDescriptor(CVSChangeLogPlugin.IMG_COPY_ENABLED)) {

            @Override
            public void run() {
                copyToClipBoard(true);
            }
        };
        copyAction.setAccelerator(SWT.CTRL | 'Z');
        copyAction.setToolTipText("Copy to clipboard");
        copyAction.setDisabledImageDescriptor(imageRegistry.getDescriptor(CVSChangeLogPlugin.IMG_COPY_DISABLED));
        copyAction.setEnabled(false);
        tbm.add(copyAction);

        refreshAction = new Action("Refresh", imageRegistry.getDescriptor(CVSChangeLogPlugin.IMG_REFRESH_ENABLED)) {

            @Override
            public void run() {
                refresh();
            }
        };
        refreshAction.setToolTipText("Refresh");
        refreshAction.setDisabledImageDescriptor(imageRegistry.getDescriptor(CVSChangeLogPlugin.IMG_REFRESH_DISABLED));
        refreshAction.setEnabled(false);
        tbm.add(refreshAction);
    }

    protected void copyToClipBoard(boolean all) {

        FileEntry[] entries;
        if (all) {
            Collection< ? > input = (Collection< ? >) filesTreeViewer.getInput();
            entries = input.toArray(new FileEntry[input.size()]);
        } else {
            List<FileEntry> list = ((IStructuredSelection) filesTreeViewer.getSelection()).toList();
            entries = list.toArray(new FileEntry[list.size()]);
        }

        // ordena conforme a ordenação do usuário
        filesTreeViewer.getSorter().sort(filesTreeViewer, entries);

        // pega o mesmo label visível para o usuário
        ITableLabelProvider labelProvider = (ITableLabelProvider) filesTreeViewer.getLabelProvider();

        StringWriter buf = new StringWriter();
        PrintWriter writer = new PrintWriter(buf);
        for (FileEntry file : entries) {
            writer.print(labelProvider.getColumnText(file, 0));
            writer.print(FILE_REVISION_SEPARATOR);
            writer.println(file.getRevision());
        }

        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        clipboard.setContents(new Object[] { buf.getBuffer().toString() }, new Transfer[] { TextTransfer.getInstance() });
        clipboard.dispose();
    }

    protected void refresh() {
        if (this.changeLogJob != null) {
            this.changeLogJob.schedule();
        }
    }

    protected TreeViewer createDatesTree(Composite parent) {
        final Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        GridData data = new GridData(GridData.FILL_BOTH);
        tree.setLayoutData(data);
        TableLayout layout = new TableLayout();
        tree.setLayout(layout);
        TreeViewer ret = new TreeViewer(tree);
        ret.setContentProvider(new ITreeContentProvider() {

            public Object[] getChildren(Object parentElement) {
                return null;
            }

            public Object getParent(Object element) {
                return null;
            }

            public boolean hasChildren(Object element) {
                return false;
            }

            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof Collection< ? >) {
                    return ((Collection< ? >) inputElement).toArray();
                }
                return new Object[0];
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

        });
        ret.setLabelProvider(new ITableLabelProvider() {

            private final DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

            public Image getColumnImage(Object element, int columnIndex) {
                if (columnIndex == 0) {
                    return CVSChangeLogPlugin.getDefault().getImageRegistry().get(CVSChangeLogPlugin.IMG_OBJ_DATE_TABLE);
                }
                return null;
            }

            public String getColumnText(Object element, int columnIndex) {
                ChangeLogEntry entry = (ChangeLogEntry) element;
                switch (columnIndex) {
                    case 0:
                        return format.format(entry.getDate());
                    case 1:
                        return entry.getAuthor();
                    case 2:
                        String msg = entry.getComment();
                        msg = msg.replace('\n', ' ');
                        msg = msg.replace('\r', ' ');
                        return msg;
                }
                return "";
            }

            public void addListener(ILabelProviderListener listener) {
            }

            public void dispose() {
            }

            public boolean isLabelProperty(Object element, String property) {
                return true;
            }

            public void removeListener(ILabelProviderListener listener) {
            }
        });

        // creation date
        TreeColumn col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("Date");
        layout.addColumnData(new ColumnPixelData(150, true));
        col.addSelectionListener(getColumnSelectionListener(ret, 0));

        // author
        col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("Author");
        layout.addColumnData(new ColumnPixelData(120, true));
        col.addSelectionListener(getColumnSelectionListener(ret, 1));

        //comment
        col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("Comment");
        layout.addColumnData(new ColumnPixelData(500, true));
        col.addSelectionListener(getColumnSelectionListener(ret, 2));

        ret.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                updateSelection(event.getSelection());
            }
        });

        ret.setSorter(new ChangeLogEntrySorter());
        setSort(ret, 0);
        setSort(ret, 0);

        String filter = CVSChangeLogPlugin.getDefault().getPreferenceStore().getString(PREFERENCE_FILTER);
        if (filter != null && filter.length() > 0) {
            ret.addFilter(new ChangeLogFilter(filter));
        }

        return ret;
    }

    protected TreeViewer createFilesTree(Composite parent) {
        final Tree tree = new Tree(parent, SWT.MULTI | SWT.FULL_SELECTION | SWT.H_SCROLL | SWT.V_SCROLL);
        tree.setHeaderVisible(true);
        tree.setLinesVisible(true);
        GridData data = new GridData(GridData.FILL_BOTH);
        tree.setLayoutData(data);
        TableLayout layout = new TableLayout();
        tree.setLayout(layout);
        TreeViewer ret = new TreeViewer(tree);
        ret.setContentProvider(new ITreeContentProvider() {

            public Object[] getChildren(Object parentElement) {
                return null;
            }

            public Object getParent(Object element) {
                return null;
            }

            public boolean hasChildren(Object element) {
                return false;
            }

            public Object[] getElements(Object inputElement) {
                if (inputElement instanceof Collection< ? >) {
                    return ((Collection< ? >) inputElement).toArray();
                }
                return new Object[0];
            }

            public void dispose() {
            }

            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
            }

        });
        ret.setLabelProvider(new ITableLabelProvider() {

            private final Map<ImageDescriptor, Image> imageCache = new HashMap<ImageDescriptor, Image>();

            public Image getColumnImage(Object element, int columnIndex) {
                FileEntry entry = (FileEntry) element;
                switch (columnIndex) {
                    case 0:
                        ImageDescriptor imageDesc = WorkbenchPlugin.getDefault().getEditorRegistry().getImageDescriptor(entry.getReposFile());
                        Image image = imageCache.get(imageDesc);
                        if (image == null) {
                            image = imageDesc.createImage();
                            imageCache.put(imageDesc, image);
                        }
                        return image;
                    case 2:
                        if (entry.getPreviousRevision().length() == 0) {
                            break;
                        }
                        //$FALL-THROUGH$
                    case 1:
                        return CVSChangeLogPlugin.getDefault().getImageRegistry().get(CVSChangeLogPlugin.IMG_OBJ_REMOTE_REVISION_TABLE);
                }
                return null;
            }

            public String getColumnText(Object element, int columnIndex) {
                FileEntry entry = (FileEntry) element;
                switch (columnIndex) {
                    case 0:

                        String filePath;
                        switch (fileDisplayMode) {
                            case ChangeLogPreferences.OPT_PROJECT_RELATIVE_PATH:
                                filePath = entry.getResource().getFullPath().toString().substring(1);
                                break;
                            case ChangeLogPreferences.OPT_CVS_FULL_PATH:
                                filePath = entry.getReposFile();
                                break;
                            case ChangeLogPreferences.OPT_FILE_NAME:
                                filePath = entry.getResource().getFullPath().lastSegment().toString();
                                break;
                            default:
                                throw new IllegalStateException("Invalid display option: " + fileDisplayMode);
                        }

                        return filePath;
                    case 1:
                        return entry.getRevision();
                    case 2:
                        return entry.getPreviousRevision();
                }
                return "";
            }

            public void addListener(ILabelProviderListener listener) {
            }

            public void dispose() {
                for (Image image : imageCache.values()) {
                    image.dispose();
                }
            }

            public boolean isLabelProperty(Object element, String property) {
                return true;
            }

            public void removeListener(ILabelProviderListener listener) {
            }
        });

        ret.addSelectionChangedListener(new ISelectionChangedListener() {

            public void selectionChanged(SelectionChangedEvent event) {
                compareMenu.setEnabled(!event.getSelection().isEmpty());
                openMenu.setEnabled(!event.getSelection().isEmpty());
                copyMenu.setEnabled(!event.getSelection().isEmpty());
            }
        });

        ret.addOpenListener(new IOpenListener() {

            public void open(OpenEvent event) {
                FileEntry entry = (FileEntry) ((IStructuredSelection) event.getSelection()).getFirstElement();
                if (entry.getPreviousRevision().length() == 0) {
                    openFileInEditor(entry);
                } else {
                    openFileInCompare(entry);
                }
            }
        });

        // file
        TreeColumn col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("File");
        col.addSelectionListener(getColumnSelectionListener(ret, 0));
        layout.addColumnData(new ColumnPixelData(450, true));

        // revision
        col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("Revision");
        col.addSelectionListener(getColumnSelectionListener(ret, 1));
        layout.addColumnData(new ColumnPixelData(80, true));

        // old revision
        col = new TreeColumn(tree, SWT.NONE);
        col.setResizable(true);
        col.setText("Previous Revision");
        col.addSelectionListener(getColumnSelectionListener(ret, 2));
        layout.addColumnData(new ColumnPixelData(80, true));

        ret.setSorter(new FilesSorter());
        setSort(ret, 0);

        return ret;
    }

    private SelectionAdapter getColumnSelectionListener(final TreeViewer viewer, final int column) {
        return new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                setSort(viewer, column);
            }
        };
    }

    protected void setSort(TreeViewer viewer, int column) {
        Tree table = viewer.getTree();
        TreeColumn oldSortColumn = table.getSortColumn();

        int dir;
        if (oldSortColumn != null && table.indexOf(oldSortColumn) == column && table.getSortDirection() != SWT.DOWN) {
            dir = SWT.DOWN;
        } else {
            dir = SWT.UP;
        }
        table.setSortColumn(table.getColumn(column));
        table.setSortDirection(dir);
        viewer.refresh();
    }

    protected void updateSelection(ISelection selection) {
        List<ChangeLogEntry> entries = ((IStructuredSelection) selection).toList();

        Collection<FileEntry> files = mergeFiles(entries);

        String comment;
        if (entries.size() > 1) {
            Set<String> comments = new LinkedHashSet<String>();
            for (ChangeLogEntry entry : entries) {
                comments.add(entry.getComment());
            }
            StringBuilder buf = new StringBuilder();
            for (String temp : comments) {
                buf.append(temp);
                buf.append("\n");
                buf.append("\n");
            }
            comment = buf.toString();
        } else {
            comment = entries.size() == 0 ? "" : entries.get(0).getComment();
        }

        filesTreeViewer.setInput(files);
        updateFilesText(files);

        commentTextViewer.getDocument().set(comment);

        copyAction.setEnabled(files.size() > 0);
        copyAction.setEnabled(files.size() > 0);
        copyAllMenu.setEnabled(files.size() > 0);
    }

    private Collection<FileEntry> mergeFiles(Collection<ChangeLogEntry> entries) {
        if (entries.isEmpty()) {
            return Collections.emptyList();
        }

        Map<IResource, FileEntry> ret = new HashMap<IResource, FileEntry>();
        for (ChangeLogEntry entry : entries) {
            for (FileEntry file : entry.getFiles()) {
                FileEntry otherFile = ret.get(file.getResource());
                if (otherFile != null) {
                    String revision = compareRevision(file.getRevision(), otherFile.getRevision()) > 0 ? file.getRevision() : otherFile.getRevision();
                    String prevRevision = compareRevision(file.getPreviousRevision(), otherFile.getPreviousRevision()) < 0 ? file.getPreviousRevision() : otherFile.getPreviousRevision();
                    file = new FileEntry(file.getResource(), file.getReposFile(), revision, prevRevision);
                }
                ret.put(file.getResource(), file);
            }
        }

        return ret.values();
    }

    protected int compareRevision(String rev1, String rev2) {
        String[] split1 = rev1.length() == 0 ? new String[0] : rev1.split("\\.");
        String[] split2 = rev2.length() == 0 ? new String[0] : rev2.split("\\.");

        for (int i = 0; i < split1.length; i++) {
            if (i > split2.length - 1) {
                return 1;
            }
            int comp = Integer.valueOf(split1[i]).compareTo(Integer.valueOf(split2[i]));
            if (comp != 0) {
                return comp;
            }
        }

        if (split2.length > split1.length) {
            return -1;
        }
        return 0;
    }

    private void updateFilesText(Collection<FileEntry> files) {
        StringBuilder buf = new StringBuilder();

        ITableLabelProvider labelProvider = (ITableLabelProvider) filesTreeViewer.getLabelProvider();

        for (FileEntry file : files) {
            buf.append(labelProvider.getColumnText(file, 0));
            buf.append(FILE_REVISION_SEPARATOR);
            buf.append(file.getRevision());
            buf.append("\n");
        }

        filesTextViewer.getDocument().set(buf.toString());
    }

    @Override
    public void setFocus() {
        if (isViewAvailable()) {
            Tree control = this.datesTreeViewer.getTree();
            if (control != null && !control.isDisposed()) {
                control.setFocus();
            }
        }
    }

    public void showChangeLog(final Collection<ChangeLogEntry> logEntries) {
        if (isViewAvailable()) {
            Display display = datesTreeViewer.getControl().getDisplay();
            if (!display.isDisposed()) {
                display.asyncExec(new Runnable() {

                    public void run() {
                        internalShowChangeLog(logEntries);
                    }
                });
            }

        }
    }

    protected void internalShowChangeLog(Collection<ChangeLogEntry> logEntries) {
        datesTreeViewer.setInput(logEntries);

        if (datesTreeViewer.getTree().getItemCount() > 0) {
            datesTreeViewer.getTree().setSelection(datesTreeViewer.getTree().getItem(0));
            updateSelection(datesTreeViewer.getSelection());
        }
    }

    /**
     * @return Returns the viewer.
     */
    public TreeViewer getViewer() {
        return datesTreeViewer;
    }

    /**
     * @return Returns the changeLogJob.
     */
    public Job getChangeLogJob() {
        return changeLogJob;
    }

    /**
     * @param changeLogJob
     *            The changeLogJob to set.
     */
    public void setChangeLogJob(ChangeLogJob changeLogJob) {
        this.changeLogJob = changeLogJob;
        refreshAction.setEnabled(changeLogJob != null);
    }

    public ChangeLogFilter getFilter() {
        ViewerFilter[] filters = datesTreeViewer.getFilters();
        if (filters.length > 0) {
            return (ChangeLogFilter) filters[0];
        }
        return null;
    }

    public void setFilter(ChangeLogFilter filter) {        
        if (needRefresh(filter)) {
            refresh();
        }
        datesTreeViewer.setFilters(new ViewerFilter[] { filter });
        CVSChangeLogPlugin.getDefault().getPreferenceStore().setValue(PREFERENCE_FILTER, filter.toString());
    }
    
    private boolean needRefresh(ChangeLogFilter newFilter) {
        ViewerFilter[] currentFilters = datesTreeViewer.getFilters();
        // larger date span demands refreshing, because the cache won't have the entries yet
        if (currentFilters.length > 0) {
            ChangeLogFilter currentFilter = (ChangeLogFilter) currentFilters[0];
            boolean fromIsBefore = false;
            boolean toIsAfter = false;
            if (currentFilter.getFromDate() != null) {
                fromIsBefore = newFilter.getFromDate() == null ? true : newFilter.getFromDate().before(currentFilter.getFromDate());
            }
            if (currentFilter.getToDate() != null) {
                toIsAfter = newFilter.getToDate() == null ? true : newFilter.getToDate().after(currentFilter.getToDate());
            }
            if (fromIsBefore || toIsAfter) {
                return true;
            }
        }
        return false;
        
    }

    protected void openFileInEditor(FileEntry entry) {
        try {
            IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), (IFile) entry.getResource());
        } catch (PartInitException e) {
            throw new RuntimeException(e);
        }
    }
    

    protected void openFileInCompare(FileEntry entry) {
        IFileRevision[] revisions = changeLogJob.getFileRevisions(entry);

        GenericHistoryViewDefaultPage fakePage = new GenericHistoryViewDefaultPage();
        fakePage.init(new PageSite(getViewSite()));
        CompareRevisionAction compareRevisionAction = new CompareRevisionAction(fakePage);
        compareRevisionAction.selectionChanged(new StructuredSelection(revisions));
        compareRevisionAction.run();
    }
    
    private boolean isViewAvailable() {
        return filesTreeViewer != null && !filesTreeViewer.getTree().isDisposed();
    }

    static class ChangeLogEntrySorter extends ViewerSorter {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            ChangeLogEntry entry1 = (ChangeLogEntry) e1;
            ChangeLogEntry entry2 = (ChangeLogEntry) e2;

            int ret;
            Tree tree = ((TreeViewer) viewer).getTree();
            int columnIndex = tree.indexOf(tree.getSortColumn());
            switch (columnIndex) {
                case 0:
                    ret = entry1.getDate().compareTo(entry2.getDate());
                    break;
                case 1:
                    ret = entry1.getAuthor().compareTo(entry2.getAuthor());
                    break;
                case 2:
                    ret = entry1.getComment().compareTo(entry2.getComment());
                    break;
                default:
                    throw new IllegalStateException("Unexpected column index: " + columnIndex);
            }
            if (tree.getSortDirection() == SWT.DOWN) {
                ret = -ret;
            }
            return ret;
        }
    }

    class FilesSorter extends ViewerSorter {

        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            FileEntry entry1 = (FileEntry) e1;
            FileEntry entry2 = (FileEntry) e2;

            int ret;
            Tree tree = ((TreeViewer) viewer).getTree();
            int columnIndex = tree.indexOf(tree.getSortColumn());
            switch (columnIndex) {
                case 0:
                    ret = super.compare(viewer, e1, e2);
                    break;
                case 1:
                    ret = compareRevision(entry1.getRevision(), entry2.getRevision());
                    break;
                case 2:
                    ret = compareRevision(entry1.getPreviousRevision(), entry2.getPreviousRevision());
                    break;
                default:
                    throw new IllegalStateException("Unexpected column index: " + columnIndex);
            }
            if (tree.getSortDirection() == SWT.DOWN) {
                ret = -ret;
            }
            return ret;
        }
    }
}
