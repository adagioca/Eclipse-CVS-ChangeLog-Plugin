/*
 * Copyright (c) John C. Landers All rights reserved. This program and the accompanying materials are made available
 * under the terms of the Common Public License v1.0 which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * *****************************************************************************
 */
package org.eclipselabs.changelog;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

/**
 * The main plugin class to be used in the desktop.
 */
public final class CVSChangeLogPlugin extends AbstractUIPlugin {

    private static final String IMAGE_OBJ_PATH = "icons/full/obj16/";

    private static final String IMAGE_ENABLED_PATH = "icons/full/etool16/";

    private static final String IMAGE_DISABLED_PATH = "icons/full/dtool16/";

    public static final String IMG_FILTER_ENABLED = IMAGE_ENABLED_PATH + "filter.gif";

    public static final String IMG_FILTER_DISABLED = IMAGE_DISABLED_PATH + "filter.gif";

    public static final String IMG_REFRESH_ENABLED = IMAGE_ENABLED_PATH + "refresh.gif";

    public static final String IMG_REFRESH_DISABLED = IMAGE_DISABLED_PATH + "refresh.gif";

    public static final String IMG_COPY_ENABLED = IMAGE_ENABLED_PATH + "copy.gif";

    public static final String IMG_COPY_DISABLED = IMAGE_DISABLED_PATH + "copy.gif";

    public static final String IMG_OBJ_REMOTE_REVISION_TABLE = IMAGE_OBJ_PATH + "remote_entry_tbl.gif";

    public static final String IMG_OBJ_DATE_TABLE = IMAGE_OBJ_PATH + "date_entry_tbl.gif";

    //The shared instance.
    private static CVSChangeLogPlugin plugin;

    public CVSChangeLogPlugin() {
    }

    @Override
    public void start(BundleContext context) throws Exception {
        super.start(context);
        plugin = this;

        // configura preferencia default
        IPreferenceStore store = getPreferenceStore();
        store.setDefault(ChangeLogPreferences.DISPLAY_PATH_PREFERENCE, ChangeLogPreferences.OPT_PROJECT_RELATIVE_PATH);
    }

    /**
     * Returns the shared instance.
     */
    public static CVSChangeLogPlugin getDefault() {
        return plugin;
    }

    /**
     * Creates an image and places it in the image registry.
     */
    protected void createImageDescriptor(String id, URL baseURL, ImageRegistry reg) {
        try {
            ImageDescriptor desc = ImageDescriptor.createFromURL(new URL(baseURL, id));
            reg.put(id, desc);
        } catch (MalformedURLException e) {
            Status pluginStatus = new Status(IStatus.ERROR, getDefault().getBundle().getSymbolicName(), e.getMessage(), e);
            plugin.getLog().log(pluginStatus);
        }
    }

    @Override
    protected void initializeImageRegistry(ImageRegistry reg) {

        URL baseURL = getBundle().getEntry("/");

        // objects
        createImageDescriptor(IMG_FILTER_ENABLED, baseURL, reg);
        createImageDescriptor(IMG_FILTER_DISABLED, baseURL, reg);

        createImageDescriptor(IMG_REFRESH_ENABLED, baseURL, reg);
        createImageDescriptor(IMG_REFRESH_DISABLED, baseURL, reg);

        createImageDescriptor(IMG_COPY_ENABLED, baseURL, reg);
        createImageDescriptor(IMG_COPY_DISABLED, baseURL, reg);

        createImageDescriptor(IMG_OBJ_REMOTE_REVISION_TABLE, baseURL, reg);
        createImageDescriptor(IMG_OBJ_DATE_TABLE, baseURL, reg);
    }

    public static void log(Exception e) {
        getDefault().getLog().log(createErrorStatus(e));
    }

    public static Status createErrorStatus(Exception e) {
        return new Status(IStatus.ERROR, CVSChangeLogPlugin.getDefault().getBundle().getSymbolicName(), e.getMessage(), e);
    }

}
