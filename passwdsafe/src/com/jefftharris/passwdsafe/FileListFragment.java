/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;
import com.jefftharris.passwdsafe.util.FileComparator;
import com.jefftharris.passwdsafe.view.GuiUtils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v4.view.MenuItemCompat;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

/**
 * The FileListFragment allows the user to choose which file to open
 */
public class FileListFragment extends ListFragment
{
    /** Listener interface for the owning activity */
    public interface Listener
    {
        /** Open a file */
        public void openFile(Uri uri, String fileName);

        /** Does the activity have a menu */
        public boolean activityHasMenu();
    }

    /** File data information for the list */
    public static final class FileData
    {
        public final File itsFile;
        public FileData(File f)
        {
            itsFile = f;
        }
        @Override
        public final String toString()
        {
            return itsFile.getName();
        }
    }

    private static final String TAG = "FileListFragment";

    private static final String TITLE = "title";
    private static final String ICON = "icon";

    private File itsDir;
    private LinkedList<File> itsDirHistory = new LinkedList<File>();
    private Listener itsListener;

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity)
    {
        super.onAttach(activity);
        itsListener = (Listener)activity;
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState)
    {
        if (itsListener.activityHasMenu()) {
            setHasOptionsMenu(true);
        }
        View view = inflater.inflate(R.layout.fragment_file_list,
                                     container, false);

        View.OnClickListener parentListener = new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doParentPressed();
            }
        };
        View v = view.findViewById(R.id.up_icon);
        v.setOnClickListener(parentListener);
        v = view.findViewById(R.id.current_group_label);
        v.setOnClickListener(parentListener);

        v = view.findViewById(R.id.home);
        v.setOnClickListener(new View.OnClickListener()
        {
            public final void onClick(View v)
            {
                doHomePressed();
            }
        });

        return view;
    }


    private FileLoaderCb itsLoaderCb;

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);
        if (PasswdSafeApp.DEBUG_AUTO_FILE != null) {
            openFile(new File(PasswdSafeApp.DEBUG_AUTO_FILE));
        }

        itsLoaderCb = new FileLoaderCb();
        LoaderManager lm = getLoaderManager();
        lm.initLoader(0, null, itsLoaderCb);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume()
    {
        super.onResume();
        PasswdSafeApp app = (PasswdSafeApp)getActivity().getApplication();
        ActivityPasswdFile file =
            app.accessPasswdFile(null, new PasswdFileActivity()
            {
                public void showProgressDialog()
                {
                }

                public void removeProgressDialog()
                {
                }

                public void saveFinished(boolean success)
                {
                }

                public Activity getActivity()
                {
                    return FileListFragment.this.getActivity();
                }
            });
        file.close();

        itsDirHistory.clear();
        showFiles();
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onCreateOptionsMenu(android.view.Menu, android.view.MenuInflater)
     */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.fragment_file_list, menu);
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem mi = menu.findItem(R.id.menu_file_new);
        MenuItemCompat.setShowAsAction(mi,
                                       MenuItemCompat.SHOW_AS_ACTION_IF_ROOM);
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public void onPrepareOptionsMenu(Menu menu)
    {
        MenuItem mi = menu.findItem(R.id.menu_parent);
        if (mi != null) {
            mi.setEnabled((itsDir != null) && (itsDir.getParentFile() != null));
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch (item.getItemId()) {
        case R.id.menu_file_new: {
            if (itsDir != null) {
                startActivity(new Intent(PasswdSafeUtil.NEW_INTENT,
                                         Uri.fromFile(itsDir)));
            }
            return true;
        }
        case R.id.menu_home: {
            doHomePressed();
            return true;
        }
        case R.id.menu_parent: {
            doParentPressed();
            return true;
        }
        default: {
            return super.onOptionsItemSelected(item);
        }
        }
    }


    /* (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id)
    {
        @SuppressWarnings("unchecked")
        Map<String, Object> item =
                (HashMap<String, Object>)l.getItemAtPosition(position);
        if (item == null) {
            return;
        }

        FileData file = (FileData) item.get(TITLE);
        if (file == null) {
            return;
        }

        if (file.itsFile.isDirectory()) {
            changeDir(file.itsFile, true);
        } else {
            PasswdSafeUtil.dbginfo(TAG, "Open file: %s", file.itsFile);
            openFile(file.itsFile);
        }
    }


    /**
     * @return true if a directory was popped, false to use default behavior
     */
    public final boolean doBackPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doBackPressed");
        if (itsDirHistory.size() == 0) {
            return false;
        }
        changeDir(itsDirHistory.removeFirst(), false);
        return true;
    }


    /** Get the files in a directory */
    public static FileData[] getFiles(File dir,
                                      final boolean showHiddenFiles,
                                      final boolean showDirs)
    {
        File[] files = dir.listFiles(new FileFilter() {
            public final boolean accept(File pathname) {
                String filename = pathname.getName();
                if (pathname.isDirectory()) {
                    if (!showDirs) {
                        return false;
                    }
                    if (!showHiddenFiles &&
                        (filename.startsWith(".") ||
                         filename.equalsIgnoreCase("LOST.DIR"))) {
                        return false;
                    }
                    return true;
                }
                if (filename.endsWith(".psafe3") || filename.endsWith(".dat")) {
                    return true;
                }
                if (showHiddenFiles &&
                    (filename.endsWith(".psafe3~") ||
                     filename.endsWith(".dat~") ||
                     filename.endsWith(".ibak"))) {
                    return true;
                }
                return false;
            }
        });

        FileData[] data;
        if (files != null) {
            Arrays.sort(files, new FileComparator());
            data = new FileData[files.length];
            for (int i = 0; i < files.length; ++i) {
                data[i] = new FileData(files[i]);
            }
        } else {
            data = new FileData[0];
        }

        return data;
    }

    /** Show the files in the current directory */
    private final void showFiles()
    {
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state) &&
            !Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            itsDir = null;
        } else {
            itsDir = getFileDir();
        }

        LoaderManager lm = getLoaderManager();
        lm.restartLoader(0, null, itsLoaderCb);
    }

    private final void updateFiles(List<Map<String, Object>> fileData)
    {
        ListAdapter adapter = null;
        if (fileData != null) {
            adapter = new SimpleAdapter(getActivity(), fileData,
                                        R.layout.file_list_item,
                                        new String[] { TITLE, ICON },
                                        new int[] { R.id.text, R.id.icon });
        }

        View rootView = getView();
        View groupPanel = rootView.findViewById(R.id.current_group_panel);
        TextView groupLabel =
                (TextView)rootView.findViewById(R.id.current_group_label);
        TextView emptyLabel =
                (TextView)rootView.findViewById(android.R.id.empty);
        if (itsDir == null) {
            groupPanel.setVisibility(View.GONE);
            groupLabel.setText("");
            emptyLabel.setText(R.string.ext_storage_not_mounted);
        } else {
            groupPanel.setVisibility(View.VISIBLE);
            groupLabel.setText(itsDir.toString());
            emptyLabel.setText(R.string.no_files);
        }

        View selectFileLabel = rootView.findViewById(R.id.select_file_label);
        if ((adapter != null) && !adapter.isEmpty()) {
            selectFileLabel.setVisibility(View.VISIBLE);
        } else {
            selectFileLabel.setVisibility(View.GONE);
        }

        setListAdapter(adapter);

        // Open the default file
        if (getListAdapter() != null) {
            Activity act = getActivity();
            PasswdSafeApp app = (PasswdSafeApp)act.getApplication();
            if (app.checkOpenDefault()) {
                SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(act);
                String defFileName = Preferences.getDefFilePref(prefs);
                File defFile = new File(itsDir, defFileName);
                if (defFile.isFile() && defFile.canRead()) {
                    openFile(defFile);
                }
            }
        }
    }


    /** Open the given file */
    private final void openFile(File file)
    {
        itsListener.openFile(Uri.fromFile(file), file.getName());
    }


    /** Handle the action to navigate to the parent directory */
    private final void doParentPressed()
    {
        PasswdSafeUtil.dbginfo(TAG, "doParentPressed");
        if (itsDir != null) {
            File newdir = itsDir.getParentFile();
            if (newdir != null) {
                changeDir(newdir, true);
            }
        }
    }


    /** Handle the action to navigate to the home directory */
    private final void doHomePressed()
    {
        changeDir(Environment.getExternalStorageDirectory(), true);
    }


    /** Get the files in the given directory */
    private static FileData[] getFiles(File dir, Context ctx)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(ctx);
        boolean showHiddenFiles =
            Preferences.getShowHiddenFilesPref(prefs);
        return getFiles(dir, showHiddenFiles, true);
    }


    /** Change to the given directory */
    private final void changeDir(File newDir, boolean saveHistory)
    {
        if (saveHistory && (itsDir != null)) {
            itsDirHistory.addFirst(itsDir);
        }
        setFileDir(newDir);
        showFiles();
        GuiUtils.invalidateOptionsMenu(getActivity());
    }


    /** Get the directory for listing files */
    private final File getFileDir()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(getActivity());
        return Preferences.getFileDirPref(prefs);
    }


    /** Set the directory for listing files */
    private final void setFileDir(File dir)
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(getActivity());
        Preferences.setFileDirPref(dir, prefs);
    }

    private static class FileLoader
            extends AsyncTaskLoader<List<Map<String, Object>>>
    {
        private File itsDir;

        public FileLoader(File dir, Context context)
        {
            super(context);
            itsDir = dir;
        }

        @Override
        protected void onAbandon()
        {
            // TODO Auto-generated method stub
            super.onAbandon();
        }

        @Override
        protected void onReset()
        {
            // TODO Auto-generated method stub
            super.onReset();
        }

        @Override
        protected void onStartLoading()
        {
            // TODO Auto-generated method stub
            super.onStartLoading();
        }

        @Override
        protected void onStopLoading()
        {
            // TODO Auto-generated method stub
            super.onStopLoading();
        }

        @Override
        public void onCanceled(List<Map<String, Object>> data)
        {
            // TODO Auto-generated method stub
            super.onCanceled(data);
        }

        @Override
        public List<Map<String, Object>> loadInBackground()
        {
            if (itsDir == null) {
                return null;
            }

            FileData[] data = getFiles(itsDir, getContext());
            List<Map<String, Object>> fileData =
                new ArrayList<Map<String, Object>>();
            for (FileData file: data) {
                HashMap<String, Object> item = new HashMap<String, Object>();
                item.put(TITLE, file);
                item.put(ICON, file.itsFile.isDirectory() ?
                               R.drawable.folder_rev : R.drawable.login_rev);
                fileData.add(item);
            }
            return fileData;
        }
    }

    private class FileLoaderCb
            implements LoaderCallbacks<List<Map<String, Object>>>
    {
        @Override
        public Loader<List<Map<String, Object>>> onCreateLoader(int id,
                                                                Bundle args)
        {
            return new FileLoader(itsDir, getActivity());
        }

        @Override
        public void onLoadFinished(Loader<List<Map<String, Object>>> loader,
                                   List<Map<String, Object>> data)
        {
            updateFiles(data);
        }

        @Override
        public void onLoaderReset(Loader<List<Map<String, Object>>> loader)
        {
            updateFiles(null);
        }
    }
}
