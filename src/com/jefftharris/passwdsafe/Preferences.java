/*
 * Copyright (©) 2009-2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.io.File;

import org.pwsafe.lib.file.PwsFile;

import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.file.PasswdRecordFilter;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.text.TextUtils;

/**
 * The Preferences class defines the activity for managing preferences on the
 * application
 *
 * @author Jeff Harris
 */
public class Preferences extends PreferenceActivity
    implements SharedPreferences.OnSharedPreferenceChangeListener
{
    public static final String PREF_FILE_DIR = "fileDirPref";
    public static final String PREF_FILE_DIR_DEF =
        Environment.getExternalStorageDirectory().toString();

    public static final String PREF_FILE_CLOSE_TIMEOUT = "fileCloseTimeoutPref";
    public static final FileTimeoutPref PREF_FILE_CLOSE_TIMEOUT_DEF =
        FileTimeoutPref.TO_5_MIN;
    public static final String PREF_FILE_CLOSE_SCREEN_OFF =
                    "fileCloseScreenOffPref";
    public static final boolean PREF_FILE_CLOSE_SCREEN_OFF_DEF = false;

    public static final String PREF_FILE_BACKUP = "fileBackupPref";
    public static final FileBackupPref PREF_FILE_BACKUP_DEF =
        FileBackupPref.BACKUP_1;

    public static final String PREF_FILE_CLOSE_CLEAR_CLIPBOARD =
        "fileCloseClearClipboardPref";
    public static final boolean PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF = true;

    public static final String PREF_FILE_OPEN_READ_ONLY =
        "fileOpenReadOnly";
    public static final boolean PREF_FILE_OPEN_READ_ONLY_DEF = false;

    public static final String PREF_DEF_FILE = "defFilePref";
    public static final String PREF_DEF_FILE_DEF = "";

    public static final String PREF_GROUP_RECORDS = "groupRecordsPref";
    public static final boolean PREF_GROUP_RECORDS_DEF = true;

    public static final String PREF_PASSWD_ENC = "passwordEncodingPref";
    public static final String PREF_PASSWD_ENC_DEF =
        PwsFile.DEFAULT_PASSWORD_CHARSET;
    public static final String PREF_PASSWD_EXPIRY_NOTIF =
        "passwordExpiryNotifyPref";
    public static final PasswdRecordFilter.ExpiryFilter
        PREF_PASSWD_EXPIRY_NOTIF_DEF =
        PasswdRecordFilter.ExpiryFilter.IN_TWO_WEEKS;

    public static final String PREF_SEARCH_CASE_SENSITIVE =
        "searchCaseSensitivePref";
    public static final boolean PREF_SEARCH_CASE_SENSITIVE_DEF = false;
    public static final String PREF_SEARCH_REGEX = "searchRegexPref";
    public static final boolean PREF_SEARCH_REGEX_DEF = false;

    public static final String PREF_SHOW_HIDDEN_FILES = "showBackupFilesPref";
    public static final boolean PREF_SHOW_HIDDEN_FILES_DEF = false;

    public static final String PREF_SORT_CASE_SENSITIVE =
        "sortCaseSensitivePref";
    public static final boolean PREF_SORT_CASE_SENSITIVE_DEF = true;

    private static final String PREF_GEN_LOWER = "passwdGenLower";
    private static final boolean PREF_GEN_LOWER_DEF = true;
    private static final String PREF_GEN_UPPER = "passwdGenUpper";
    private static final boolean PREF_GEN_UPPER_DEF = true;
    private static final String PREF_GEN_DIGITS = "passwdGenDigits";
    private static final boolean PREF_GEN_DIGITS_DEF = true;
    private static final String PREF_GEN_SYMBOLS = "passwdGenSymbols";
    private static final boolean PREF_GEN_SYMBOLS_DEF = false;
    private static final String PREF_GEN_EASY = "passwdGenEasy";
    private static final boolean PREF_GEN_EASY_DEF = false;
    private static final String PREF_GEN_HEX = "passwdGenHex";
    private static final boolean PREF_GEN_HEX_DEF = false;
    private static final String PREF_GEN_LENGTH = "passwdGenLength";
    private static final String PREF_GEN_LENGTH_DEF = "8";
    public static final String PREF_DEF_PASSWD_POLICY = "defaultPasswdPolicy";
    public static final String PREF_DEF_PASSWD_POLICY_DEF = "";

    public static final String PREF_FONT_SIZE = "fontSizePref";
    public static final FontSizePref PREF_FONT_SIZE_DEF = FontSizePref.NORMAL;

    public static final String INTENT_SCREEN = "screen";
    public static final String SCREEN_PASSWORD_OPTIONS = "passwordOptions";

    private static final String TAG = "Preferences";

    private EditTextPreference itsFileDirPref;
    private ListPreference itsDefFilePref;
    private ListPreference itsFileClosePref;
    private ListPreference itsFileBackupPref;
    private ListPreference itsPasswdEncPref;
    private ListPreference itsPasswdExpiryNotifPref;
    private ListPreference itsFontSizePref;


    public static FileTimeoutPref getFileCloseTimeoutPref(SharedPreferences prefs)
    {
        try {
            return FileTimeoutPref.prefValueOf(
                prefs.getString(PREF_FILE_CLOSE_TIMEOUT,
                                PREF_FILE_CLOSE_TIMEOUT_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_CLOSE_TIMEOUT_DEF;
        }
    }

    public static boolean getFileCloseScreenOffPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_SCREEN_OFF,
                                PREF_FILE_CLOSE_SCREEN_OFF_DEF);
    }

    public static FileBackupPref getFileBackupPref(SharedPreferences prefs)
    {
        try {
            return FileBackupPref.prefValueOf(
                prefs.getString(PREF_FILE_BACKUP,
                                PREF_FILE_BACKUP_DEF.getValue()));
        } catch (IllegalArgumentException e) {
            return PREF_FILE_BACKUP_DEF;
        }
    }

    public static boolean getFileCloseClearClipboardPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_CLOSE_CLEAR_CLIPBOARD,
                                PREF_FILE_CLOSE_CLEAR_CLIPBOARD_DEF);
    }

    public static boolean getFileOpenReadOnlyPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_FILE_OPEN_READ_ONLY,
                                PREF_FILE_OPEN_READ_ONLY_DEF);
    }

    public static void setFileOpenReadOnlyPref(boolean readonly,
                                               SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putBoolean(PREF_FILE_OPEN_READ_ONLY, readonly);
        prefsEdit.commit();
    }

    public static File getFileDirPref(SharedPreferences prefs)
    {
        return new File(prefs.getString(PREF_FILE_DIR, PREF_FILE_DIR_DEF));
    }

    public static void setFileDirPref(File dir, SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(Preferences.PREF_FILE_DIR, dir.toString());
        prefsEdit.commit();
    }

    public static String getDefFilePref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_DEF_FILE, PREF_DEF_FILE_DEF);
    }

    public static FontSizePref getFontSizePref(SharedPreferences prefs)
    {
        try {
            return FontSizePref.valueOf(
                prefs.getString(PREF_FONT_SIZE, PREF_FONT_SIZE_DEF.toString()));
        } catch (IllegalArgumentException e) {
            return PREF_FONT_SIZE_DEF;
        }
    }

    public static boolean getGroupRecordsPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_GROUP_RECORDS, PREF_GROUP_RECORDS_DEF);
    }

    public static String getPasswordEncodingPref(SharedPreferences prefs)
    {
        return prefs.getString(PREF_PASSWD_ENC, PREF_PASSWD_ENC_DEF);
    }

    /** Get the password expiration notification preference */
    public static PasswdRecordFilter.ExpiryFilter
    getPasswdExpiryNotifPref(SharedPreferences prefs)
    {
        try {
            return PasswdRecordFilter.ExpiryFilter.prefValueOf(
                prefs.getString(PREF_PASSWD_EXPIRY_NOTIF,
                                PREF_PASSWD_EXPIRY_NOTIF_DEF.getPrefValue()));
        } catch (IllegalArgumentException e) {
            return PREF_PASSWD_EXPIRY_NOTIF_DEF;
        }
    }

    /** Upgrade the default password policy preference if needed */
    public static void upgradePasswdPolicy(SharedPreferences prefs,
                                           Context ctx)
    {
        if (prefs.contains(PREF_DEF_PASSWD_POLICY)) {
            PasswdSafeApp.dbginfo(TAG, "Have default policy");
            return;
        }

        SharedPreferences.Editor prefsEdit = prefs.edit();
        String policyStr = PREF_DEF_PASSWD_POLICY_DEF;
        if (prefs.contains(PREF_GEN_LOWER) ||
            prefs.contains(PREF_GEN_UPPER) ||
            prefs.contains(PREF_GEN_DIGITS) ||
            prefs.contains(PREF_GEN_SYMBOLS) ||
            prefs.contains(PREF_GEN_EASY) ||
            prefs.contains(PREF_GEN_HEX) ||
            prefs.contains(PREF_GEN_LENGTH)) {
            PasswdSafeApp.dbginfo(TAG, "Upgrade old prefs");

            int flags = 0;
            if (prefs.getBoolean(PREF_GEN_HEX, PREF_GEN_HEX_DEF)) {
                flags |= PasswdPolicy.FLAG_USE_HEX_DIGITS;
            } else {
                if (prefs.getBoolean(PREF_GEN_EASY, PREF_GEN_EASY_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_EASY_VISION;
                }

                if (prefs.getBoolean(PREF_GEN_LOWER, PREF_GEN_LOWER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_UPPER, PREF_GEN_UPPER_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
                }
                if (prefs.getBoolean(PREF_GEN_DIGITS, PREF_GEN_DIGITS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_DIGITS;
                }
                if (prefs.getBoolean(PREF_GEN_SYMBOLS, PREF_GEN_SYMBOLS_DEF)) {
                    flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
                }
            }
            int length;
            try {
                length = Integer.parseInt(prefs.getString(PREF_GEN_LENGTH,
                                                          PREF_GEN_LENGTH_DEF));
            } catch (NumberFormatException e) {
                length = Integer.parseInt(PREF_GEN_LENGTH_DEF);
            }
            PasswdPolicy policy = PasswdPolicy.createDefaultPolicy(ctx, flags,
                                                                   length);
            policyStr = policy.toHdrPolicyString();

            prefsEdit.remove(PREF_GEN_LOWER);
            prefsEdit.remove(PREF_GEN_UPPER);
            prefsEdit.remove(PREF_GEN_DIGITS);
            prefsEdit.remove(PREF_GEN_SYMBOLS);
            prefsEdit.remove(PREF_GEN_EASY);
            prefsEdit.remove(PREF_GEN_HEX);
            prefsEdit.remove(PREF_GEN_LENGTH);
        }

        PasswdSafeApp.dbginfo(TAG, "Save new default policy: " + policyStr);
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policyStr);
        prefsEdit.commit();
    }

    /** Get the default password policy preference */
    public static PasswdPolicy getDefPasswdPolicyPref(SharedPreferences prefs,
                                                      Context ctx)
    {
        String policyStr = prefs.getString(PREF_DEF_PASSWD_POLICY,
                                           PREF_DEF_PASSWD_POLICY_DEF);
        PasswdPolicy policy = null;
        if (!TextUtils.isEmpty(policyStr)) {
            try {
                policy = PasswdPolicy.parseHdrPolicy(
                    policyStr, 0, 0, PasswdPolicy.Location.DEFAULT).first;
            } catch (Exception e) {
                // Use default
            }
        }
        if (policy == null) {
            policy = PasswdPolicy.createDefaultPolicy(ctx);
        }
        return policy;
    }

    /** Set the default password policy preference */
    public static void setDefPasswdPolicyPref(PasswdPolicy policy,
                                              SharedPreferences prefs)
    {
        SharedPreferences.Editor prefsEdit = prefs.edit();
        prefsEdit.putString(PREF_DEF_PASSWD_POLICY, policy.toHdrPolicyString());
        prefsEdit.commit();
    }

    public static boolean getSearchCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_CASE_SENSITIVE,
                                PREF_SEARCH_CASE_SENSITIVE_DEF);
    }

    public static boolean getSearchRegexPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SEARCH_REGEX, PREF_SEARCH_REGEX_DEF);
    }

    public static boolean getShowHiddenFilesPref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SHOW_HIDDEN_FILES,
                                PREF_SHOW_HIDDEN_FILES_DEF);
    }

    public static boolean getSortCaseSensitivePref(SharedPreferences prefs)
    {
        return prefs.getBoolean(PREF_SORT_CASE_SENSITIVE,
                                PREF_SORT_CASE_SENSITIVE_DEF);
    }


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);

        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.registerOnSharedPreferenceChangeListener(this);

        itsFileDirPref = (EditTextPreference)findPreference(PREF_FILE_DIR);
        itsDefFilePref = (ListPreference)findPreference(PREF_DEF_FILE);
        itsFileClosePref = (ListPreference)
            findPreference(PREF_FILE_CLOSE_TIMEOUT);
        itsFileBackupPref = (ListPreference)
            findPreference(PREF_FILE_BACKUP);

        itsFileDirPref.setDefaultValue(PREF_FILE_DIR_DEF);
        updateFileDirPrefs(getFileDirPref(prefs), prefs);

        onSharedPreferenceChanged(prefs, PREF_DEF_FILE);

        Resources res = getResources();
        itsFileClosePref.setEntries(FileTimeoutPref.getDisplayNames(res));
        itsFileClosePref.setEntryValues(FileTimeoutPref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FILE_CLOSE_TIMEOUT);

        itsFileBackupPref.setEntries(FileBackupPref.getDisplayNames(res));
        itsFileBackupPref.setEntryValues(FileBackupPref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FILE_BACKUP);

        itsPasswdEncPref = (ListPreference)findPreference(PREF_PASSWD_ENC);
        String[] charsets =
            PwsFile.ALL_PASSWORD_CHARSETS.toArray(new String[0]);
        itsPasswdEncPref.setEntries(charsets);
        itsPasswdEncPref.setEntryValues(charsets);
        itsPasswdEncPref.setDefaultValue(PREF_PASSWD_ENC_DEF);
        onSharedPreferenceChanged(prefs, PREF_PASSWD_ENC);

        itsPasswdExpiryNotifPref =
            (ListPreference)findPreference(PREF_PASSWD_EXPIRY_NOTIF);
        itsPasswdExpiryNotifPref.setEntries(
            PasswdRecordFilter.ExpiryFilter.getPrefDisplayNames(res));
        itsPasswdExpiryNotifPref.setEntryValues(
            PasswdRecordFilter.ExpiryFilter.getPrefValues());
        onSharedPreferenceChanged(prefs, PREF_PASSWD_EXPIRY_NOTIF);

        itsFontSizePref = (ListPreference) findPreference(PREF_FONT_SIZE);
        itsFontSizePref.setEntries(FontSizePref.getDisplayNames(res));
        itsFontSizePref.setEntryValues(FontSizePref.getValues());
        onSharedPreferenceChanged(prefs, PREF_FONT_SIZE);

        Intent intent = getIntent();
        String screen = intent.getStringExtra(INTENT_SCREEN);
        if (screen != null) {
            Preference scr = findPreference(screen);
            getPreferenceScreen().onItemClick(null, null, scr.getOrder(), 0);
        }
    }

    /* (non-Javadoc)
     * @see android.preference.PreferenceActivity#onDestroy()
     */
    @Override
    protected void onDestroy()
    {
        SharedPreferences prefs =
            PreferenceManager.getDefaultSharedPreferences(this);
        prefs.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }

    /* (non-Javadoc)
     * @see android.content.SharedPreferences.OnSharedPreferenceChangeListener#onSharedPreferenceChanged(android.content.SharedPreferences, java.lang.String)
     */
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key)
    {
        if (key.equals(PREF_FILE_DIR)) {
            File pref = getFileDirPref(prefs);
            if (pref.toString().length() == 0) {
                pref = new File(PREF_FILE_DIR_DEF);
                itsFileDirPref.setText(pref.toString());
            }
            itsDefFilePref.setValue(PREF_DEF_FILE_DEF);
            updateFileDirPrefs(pref, prefs);
        } else if (key.equals(PREF_DEF_FILE)) {
            itsDefFilePref.setSummary(
                defFileValueToEntry(getDefFilePref(prefs)));
        } else if (key.equals(PREF_FILE_CLOSE_TIMEOUT)) {
            itsFileClosePref.setSummary(
                getFileCloseTimeoutPref(prefs).getDisplayName(getResources()));
        } else if (key.equals(PREF_FILE_BACKUP)) {
            itsFileBackupPref.setSummary(
                getFileBackupPref(prefs).getDisplayName(getResources()));
        } else if (key.equals(PREF_PASSWD_ENC)) {
            itsPasswdEncPref.setSummary(getPasswordEncodingPref(prefs));
        } else if (key.equals(PREF_PASSWD_EXPIRY_NOTIF)) {
            itsPasswdExpiryNotifPref.setSummary(
               getPasswdExpiryNotifPref(prefs).getPrefDisplayName(
                   getResources()));
        } else if (key.equals(PREF_FONT_SIZE)) {
            itsFontSizePref.setSummary(
                getFontSizePref(prefs).getDisplayName(getResources()));
        }
    }

    private final void updateFileDirPrefs(File fileDir,
                                          SharedPreferences prefs)
    {
        itsFileDirPref.setSummary(fileDir.toString());

        FileList.FileData[] files = FileList.getFiles(fileDir, false, false);
        String[] entries = new String[files.length + 1];
        String[] entryValues = new String[files.length + 1];
        entries[0] = getString(R.string.none);
        entryValues[0] = PREF_DEF_FILE_DEF;
        for (int i = 0; i < files.length; ++i) {
            entries[i + 1] = files[i].toString();
            entryValues[i + 1] = entries[i + 1];
        }

        itsDefFilePref.setEntries(entries);
        itsDefFilePref.setEntryValues(entryValues);
    }

    private final String defFileValueToEntry(String value)
    {
        if (value.equals(PREF_DEF_FILE_DEF)) {
            return getString(R.string.none);
        } else {
            return value;
        }
    }
}
