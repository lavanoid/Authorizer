/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.jefftharris.passwdsafe.file.PasswdFileData;
import com.jefftharris.passwdsafe.file.PasswdPolicy;
import com.jefftharris.passwdsafe.view.DialogUtils;
import com.jefftharris.passwdsafe.view.PasswdPolicyView;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Activity for managing password policies for a file
 */
public class PasswdPolicyActivity extends AbstractPasswdFileListActivity
{
    private static final int MENU_ADD =         0;
    private static final int MENU_EDIT =        1;
    private static final int MENU_DELETE =      2;

    private static final int DIALOG_ADD =       MAX_DIALOG + 1;
    private static final int DIALOG_EDIT =      MAX_DIALOG + 2;
    private static final int DIALOG_DELETE =    MAX_DIALOG + 3;

    private List<PasswdPolicy> itsPolicies;
    private Set<String> itsPolicyNames;
    private DialogValidator itsDeleteValidator;
    private EditDialog itsEditDialog;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.passwd_policy);

        if (!accessOpenFile()) {
            finish();
            return;
        }

        setTitle(PasswdSafeApp.getAppFileTitle(getUri(), this));
        // Programmatic setting for Android 1.5
        getListView().setChoiceMode(ListView.CHOICE_MODE_SINGLE);
        showPolicies();
    }


    /* (non-Javadoc)
     * @see com.jefftharris.passwdsafe.PasswdFileActivity#saveFinished(boolean)
     */
    public void saveFinished(boolean success)
    {
        setResult(PasswdSafeApp.RESULT_MODIFIED);
        showPolicies();
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        PasswdFileData fileData = getPasswdFileData();
        boolean readonlyFile =
            (fileData == null) || !fileData.isV3() || !fileData.canEdit();
        PasswdPolicy selPolicy = getSelectedPolicy();

        boolean canEdit =
            (selPolicy != null) &&
            (!readonlyFile ||
                (selPolicy.getLocation() == PasswdPolicy.Location.DEFAULT));

        boolean canDelete =
            !readonlyFile &&
            (selPolicy != null) &&
            (selPolicy.getLocation() != PasswdPolicy.Location.DEFAULT);

        MenuItem mi;
        mi = menu.findItem(MENU_ADD);
        mi.setEnabled(!readonlyFile);
        mi = menu.findItem(MENU_EDIT);
        mi.setEnabled(canEdit);
        mi = menu.findItem(MENU_DELETE);
        mi.setEnabled(canDelete);

        return super.onPrepareOptionsMenu(menu);
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        menu.add(0, MENU_ADD, 0, R.string.add_policy);
        menu.add(0, MENU_EDIT, 0, R.string.edit_policy);
        menu.add(0, MENU_DELETE, 0, R.string.delete_policy);
        return true;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        boolean rc = true;
        switch (item.getItemId()) {
        case MENU_ADD: {
            removeDialog(DIALOG_ADD);
            showDialog(DIALOG_ADD);
            break;
        }
        case MENU_EDIT: {
            removeDialog(DIALOG_EDIT);
            showDialog(DIALOG_EDIT);
            break;
        }
        case MENU_DELETE: {
            removeDialog(DIALOG_DELETE);
            showDialog(DIALOG_DELETE);
            break;
        }
        default: {
            rc = super.onOptionsItemSelected(item);
            break;
        }
        }
        return rc;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onCreateDialog(int)
     */
    @Override
    protected Dialog onCreateDialog(int id)
    {
        Dialog dialog = null;
        switch (id) {
        case DIALOG_ADD: {
            itsEditDialog = new EditDialog();
            dialog = itsEditDialog.create(null);
            break;
        }
        case DIALOG_EDIT: {
            itsEditDialog = new EditDialog();
            dialog = itsEditDialog.create(getSelectedPolicy());
            break;
        }
        case DIALOG_DELETE: {
            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
            {
                @Override
                public final void onOkClicked(DialogInterface dialog)
                {
                    dialog.dismiss();
                    deletePolicy();
                }
            };

            PasswdPolicy policy = getSelectedPolicy();
            String prompt = getString(R.string.delete_policy_msg,
                                      policy.getName());
            String title = getString(R.string.delete_policy_title);
            DialogUtils.DialogData data =
                DialogUtils.createDeletePrompt(this, dlgClick, title, prompt);
            dialog = data.itsDialog;
            itsDeleteValidator = data.itsValidator;
            break;

        }
        default: {
            dialog = super.onCreateDialog(id);
            break;
        }
        }
        return dialog;
    }


    /* (non-Javadoc)
     * @see android.app.Activity#onPrepareDialog(int, android.app.Dialog)
     */
    @Override
    protected void onPrepareDialog(int id, Dialog dialog)
    {
        switch (id) {
        case DIALOG_ADD:
        case DIALOG_EDIT: {
            itsEditDialog.reset();
            break;
        }
        case DIALOG_DELETE: {
            itsDeleteValidator.reset();
            break;
        }
        default: {
            super.onPrepareDialog(id, dialog);
            break;
        }
        }
    }


    /* (non-Javadoc)
     * @see android.app.ListActivity#onListItemClick(android.widget.ListView, android.view.View, int, long)
     */
    @Override
    protected void onListItemClick(ListView l, View v, int position, long id)
    {
        GuiUtils.invalidateOptionsMenu(this);

        PasswdPolicy policy = null;
        if ((position >= 0) && (position < itsPolicies.size())) {
            policy = itsPolicies.get(position);
        }
        showPolicy(policy);
    }


    /** Show the password policies */
    private final void showPolicies()
    {
        GuiUtils.invalidateOptionsMenu(this);

        PasswdFileData fileData = getPasswdFileData();
        itsPolicies = null;
        if (fileData != null) {
            itsPolicies = fileData.getHdrPasswdPolicies();
        }
        if (itsPolicies == null) {
            itsPolicies = new ArrayList<PasswdPolicy>();
        }

        itsPolicyNames = new HashSet<String>(itsPolicies.size());
        for (PasswdPolicy policy: itsPolicies) {
            itsPolicyNames.add(policy.getName());
        }

        itsPolicies.add(getPasswdSafeApp().getDefaultPasswdPolicy());
        sortPolicies();

        setListAdapter(new ArrayAdapter<PasswdPolicy>(
            this, android.R.layout.simple_list_item_single_choice,
            itsPolicies));
        showPolicy(null);
    }


    /** Sort the policies */
    private final void sortPolicies()
    {
        Collections.sort(
            itsPolicies,
            new Comparator<PasswdPolicy>()
            {
                public int compare(PasswdPolicy lhs, PasswdPolicy rhs)
                {
                    PasswdPolicy.Location lhsLoc = lhs.getLocation();
                    PasswdPolicy.Location rhsLoc = rhs.getLocation();
                    if (lhsLoc != rhsLoc) {
                        if (lhsLoc == PasswdPolicy.Location.DEFAULT) {
                            return -1;
                        }
                        if (rhsLoc == PasswdPolicy.Location.DEFAULT) {
                            return 1;
                        }
                    }
                    return lhs.getName().compareTo(rhs.getName());
                }
            });
    }


    /** Show the details of a policy */
    private final void showPolicy(PasswdPolicy policy)
    {
        PasswdPolicyView view =
            (PasswdPolicyView)findViewById(R.id.policy_view);
        view.showPolicy(policy);
    }


    /** Add or update a policy */
    private final void addUpdatePolicy(PasswdPolicy oldPolicy,
                                       PasswdPolicy newPolicy)
    {
        if (newPolicy.getLocation() == PasswdPolicy.Location.DEFAULT) {
            getPasswdSafeApp().setDefaultPasswdPolicy(newPolicy);
            showPolicies();
        } else {
            if (oldPolicy != null) {
                itsPolicies.remove(oldPolicy);
            }
            itsPolicies.add(newPolicy);
            sortPolicies();
            savePolicies();
        }
    }


    /** Delete the currently selected policy */
    private final void deletePolicy()
    {
        PasswdPolicy policy = getSelectedPolicy();
        if (policy.getLocation() != PasswdPolicy.Location.DEFAULT) {
            itsPolicies.remove(policy);
            savePolicies();
        }
    }


    /** Save the policies */
    private final void savePolicies()
    {
        PasswdFileData fileData = getPasswdFileData();
        if (fileData != null) {
            List<PasswdPolicy> hdrPolicies =
                new ArrayList<PasswdPolicy>(itsPolicies.size());
            for (PasswdPolicy policy: itsPolicies) {
                if (policy.getLocation() == PasswdPolicy.Location.HEADER) {
                    hdrPolicies.add(policy);
                }
            }
            fileData.setHdrPasswdPolicies(
                hdrPolicies.isEmpty() ? null : hdrPolicies);
            getPasswdFile().save();
        }
        showPolicies();
    }

    /** Get the currently selected policy */
    private final PasswdPolicy getSelectedPolicy()
    {
        PasswdPolicy policy = null;
        int selectedPos = getListView().getCheckedItemPosition();
        if ((itsPolicies != null) &&
            (selectedPos >= 0) &&
            (selectedPos < itsPolicies.size())) {
            policy = itsPolicies.get(selectedPos);
        }
        return policy;
    }


    /**
     * The EditDialog class encapsulates the functionality for the dialog to
     * add or edit a policy.
     */
    private class EditDialog
    {
        private PasswdPolicy itsPolicy;
        private View itsView;
        private DialogValidator itsValidator;
        private PasswdPolicy.Type itsOrigType = PasswdPolicy.Type.NORMAL;
        private PasswdPolicy.Type itsType = PasswdPolicy.Type.NORMAL;
        private TextView itsNameEdit;
        private TextView itsLengthEdit;
        // Lower, upper, digits, symbols
        private CheckBox[] itsOptions = new CheckBox[4];
        private TextView[] itsOptionLens = new TextView[4];
        private CheckBox itsUseCustomSymbols;
        private TextView itsCustomSymbolsEdit;

        /** Create a dialog to edit the give policy (null for an add) */
        public Dialog create(PasswdPolicy policy)
        {
            itsPolicy = policy;
            Activity act = PasswdPolicyActivity.this;
            LayoutInflater factory = LayoutInflater.from(act);
            itsView = factory.inflate(R.layout.passwd_policy_edit, null);

            itsNameEdit = (TextView)itsView.findViewById(R.id.name);
            itsLengthEdit = (TextView)itsView.findViewById(R.id.length);
            itsOptions[0] = (CheckBox)itsView.findViewById(R.id.lowercase);
            itsOptions[1] = (CheckBox)itsView.findViewById(R.id.uppercase);
            itsOptions[2] = (CheckBox)itsView.findViewById(R.id.digits);
            itsOptions[3] = (CheckBox)itsView.findViewById(R.id.symbols);
            itsOptionLens[0] = (TextView)itsView.findViewById(R.id.lowercase_len);
            itsOptionLens[1] = (TextView)itsView.findViewById(R.id.uppercase_len);
            itsOptionLens[2] = (TextView)itsView.findViewById(R.id.digits_len);
            itsOptionLens[3] = (TextView)itsView.findViewById(R.id.symbols_len);
            itsUseCustomSymbols =
                (CheckBox)itsView.findViewById(R.id.use_custom_symbols);
            itsCustomSymbolsEdit =
                (TextView)itsView.findViewById(R.id.symbols_custom);

            int titleId;
            String name;
            int len;
            boolean[] useOptions = new boolean[4];
            int[] optionLens = new int[4];
            String customSymbols;
            if (policy != null) {
                titleId = R.string.edit_policy;
                name = policy.getName();
                if (policy.getLocation() == PasswdPolicy.Location.DEFAULT) {
                    itsNameEdit.setEnabled(false);
                }
                len = policy.getLength();
                itsOrigType = policy.getType();
                useOptions[0] =
                    policy.checkFlags(PasswdPolicy.FLAG_USE_LOWERCASE);
                useOptions[1] =
                    policy.checkFlags(PasswdPolicy.FLAG_USE_UPPERCASE);
                useOptions[2] =
                    policy.checkFlags(PasswdPolicy.FLAG_USE_DIGITS);
                useOptions[3] =
                    policy.checkFlags(PasswdPolicy.FLAG_USE_SYMBOLS);
                optionLens[0] = policy.getMinLowercase();
                optionLens[1] = policy.getMinUppercase();
                optionLens[2] = policy.getMinDigits();
                optionLens[3] = policy.getMinSymbols();
                customSymbols = policy.getSpecialSymbols();
            } else {
                titleId = R.string.new_policy;
                name = "";
                len = 12;
                itsOrigType = PasswdPolicy.Type.NORMAL;
                for (int i = 0; i < useOptions.length; ++i) {
                    useOptions[i] = true;
                    optionLens[i] = 1;
                }
                customSymbols = null;
            }

            AbstractDialogClickListener dlgClick =
                new AbstractDialogClickListener()
                {
                    @Override
                    public void onOkClicked(DialogInterface dialog)
                    {
                        dialog.dismiss();
                        addUpdatePolicy(itsPolicy, createPolicy());
                    }
                };

            AlertDialog.Builder alert = new AlertDialog.Builder(act)
                .setTitle(titleId)
                .setView(itsView)
                .setPositiveButton(R.string.ok, dlgClick)
                .setNegativeButton(R.string.cancel, dlgClick)
                .setOnCancelListener(dlgClick);
            AlertDialog dialog = alert.create();

            itsValidator = new DialogValidator.AlertValidator(dialog, itsView,
                                                              act, false)
            {
                @Override
                protected String doValidation()
                {
                    String name = itsNameEdit.getText().toString();
                    if (TextUtils.isEmpty(name)) {
                        return getString(R.string.empty_name);
                    }

                    if (((itsPolicy == null) ||
                         (!itsPolicy.getName().equals(name))) &&
                        itsPolicyNames.contains(name)) {
                        return getString(R.string.duplicate_name);
                    }

                    int length;
                    try {
                        length = getTextViewInt(itsLengthEdit);
                        if (length < 4) {
                            return getString(R.string.length_min_val, 4);
                        } else if (length > 1024) {
                            return getString(R.string.length_max_val, 1024);
                        } else if ((itsType == PasswdPolicy.Type.HEXADECIMAL) &&
                                   ((length % 2) != 0) ) {
                            return getString(R.string.length_even_hex);
                        }

                    } catch (NumberFormatException e) {
                        return getString(R.string.invalid_length);
                    }

                    if (itsType != PasswdPolicy.Type.HEXADECIMAL) {
                        boolean oneSelected = false;
                        for (CheckBox option: itsOptions) {
                            if (option.isChecked()) {
                                oneSelected = true;
                                break;
                            }
                        }
                        if (!oneSelected) {
                            return getString(R.string.option_not_selected);
                        }
                    }

                    if (itsType == PasswdPolicy.Type.NORMAL) {
                        int minOptionsLen = 0;
                        for (int i = 0; i < itsOptions.length; ++i) {
                            if (itsOptions[i].isChecked()) {
                                try {
                                    int len = getTextViewInt(itsOptionLens[i]);
                                    minOptionsLen += len;
                                } catch (NumberFormatException e) {
                                    return getString(
                                        R.string.invalid_option_length);
                                }
                            }
                        }
                        if (minOptionsLen > length) {
                            return getString(R.string.password_len_short_opt);
                        }
                    }

                    if (itsUseCustomSymbols.isChecked()) {
                        String syms = itsCustomSymbolsEdit.getText().toString();
                        if (TextUtils.isEmpty(syms)) {
                            return getString(R.string.empty_custom_symbols);
                        }
                        for (int i = 0; i < syms.length(); ++i) {
                            char c = syms.charAt(i);
                            if (Character.isLetterOrDigit(c) ||
                                Character.isSpaceChar(c)) {
                                return getString(
                                    R.string.custom_symbol_not_alphanum);
                            }
                        }
                    }

                    return super.doValidation();
                }

            };

            // Must set text before registering view so validation isn't
            // triggered right away

            itsNameEdit.setText(name);
            itsValidator.registerTextView(itsNameEdit);
            setTextView(itsLengthEdit, len);
            itsValidator.registerTextView(itsLengthEdit);

            setType(itsOrigType, true);
            for (int i = 0; i < itsOptions.length; ++i) {
                setOption(itsOptions[i], useOptions[i], true);
                setTextView(itsOptionLens[i], optionLens[i]);
                itsValidator.registerTextView(itsOptionLens[i]);
            }

            setCustomSymbolsOption(customSymbols != null, true);
            itsCustomSymbolsEdit.setText(customSymbols);
            itsValidator.registerTextView(itsCustomSymbolsEdit);

            return dialog;
        }


        /** Reset the dialog validation */
        public void reset()
        {
            itsValidator.reset();
        }


        /**
         * Create a policy from the dialog fields. It is assumed that the fields
         * are valid
         */
        private PasswdPolicy createPolicy()
        {
            PasswdPolicy policy = new PasswdPolicy(
                itsNameEdit.getText().toString(),
                (itsPolicy != null) ? itsPolicy.getLocation() :
                    PasswdPolicy.Location.HEADER);
            int length = getTextViewInt(itsLengthEdit);
            policy.setLength(length);

            int flags = 0;
            int minLower = 1;
            int minUpper = 1;
            int minDigits = 1;
            int minSymbols = 1;
            String customSymbols = null;

            if (itsType != PasswdPolicy.Type.HEXADECIMAL) {
                if (itsOptions[0].isChecked()) {
                    flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
                }
                if (itsOptions[1].isChecked()) {
                    flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
                }
                if (itsOptions[2].isChecked()) {
                    flags |= PasswdPolicy.FLAG_USE_DIGITS;
                }
                if (itsOptions[3].isChecked()) {
                    flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
                }
            }

            switch (itsType) {
            case NORMAL:
            case PRONOUNCEABLE: {
                if (itsUseCustomSymbols.isChecked()) {
                    customSymbols = itsCustomSymbolsEdit.getText().toString();
                }
                break;
            }
            case EASY_TO_READ:
            case HEXADECIMAL: {
                break;
            }
            }

            switch (itsType) {
            case NORMAL: {
                if ((flags & PasswdPolicy.FLAG_USE_LOWERCASE) != 0) {
                    minLower = getTextViewInt(itsOptionLens[0]);
                }
                if ((flags & PasswdPolicy.FLAG_USE_UPPERCASE) != 0) {
                    minUpper = getTextViewInt(itsOptionLens[1]);
                }
                if ((flags & PasswdPolicy.FLAG_USE_DIGITS) != 0) {
                    minDigits = getTextViewInt(itsOptionLens[2]);
                }
                if ((flags & PasswdPolicy.FLAG_USE_SYMBOLS) != 0) {
                    minSymbols = getTextViewInt(itsOptionLens[3]);
                }
                break;
            }
            case EASY_TO_READ: {
                flags |= PasswdPolicy.FLAG_USE_EASY_VISION;
                break;
            }
            case PRONOUNCEABLE: {
                flags |= PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE;
                break;
            }
            case HEXADECIMAL: {
                flags |= PasswdPolicy.FLAG_USE_HEX_DIGITS;
                break;
            }
            }

            policy.setFlags(flags);
            policy.setMinLowercase(minLower);
            policy.setMinUppercase(minUpper);
            policy.setMinDigits(minDigits);
            policy.setMinSymbols(minSymbols);
            policy.setSpecialSymbols(customSymbols);

            return policy;
        }


        /** Set the type of policy and update the UI */
        private final void setType(PasswdPolicy.Type type, boolean init)
        {
            if ((type == itsType) && !init) {
                return;
            }

            itsType = type;
            if (init) {
                Spinner typeSpin = (Spinner)itsView.findViewById(R.id.type);
                typeSpin.setSelection(itsType.itsStrIdx);
                typeSpin.setOnItemSelectedListener(new OnItemSelectedListener()
                {
                    public void onItemSelected(AdapterView<?> parent, View arg1,
                                               int position, long id)
                    {
                        setType(PasswdPolicy.Type.fromStrIdx(position), false);
                    }

                    public void onNothingSelected(AdapterView<?> arg0)
                    {
                        setType(PasswdPolicy.Type.NORMAL, false);
                    }
                });
            }

            boolean optionsVisible = false;
            String defaultSymbols = null;
            switch (itsType) {
            case NORMAL: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_DEFAULT;
                break;
            }
            case EASY_TO_READ: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_EASY;
                break;
            }
            case PRONOUNCEABLE: {
                optionsVisible = true;
                defaultSymbols = PasswdPolicy.SYMBOLS_PRONOUNCE;
                break;
            }
            case HEXADECIMAL: {
                optionsVisible = false;
                break;
            }
            }

            setVisible(R.id.lowercase_row, optionsVisible);
            setVisible(R.id.uppercase_row, optionsVisible);
            setVisible(R.id.digits_row, optionsVisible);
            setVisible(R.id.symbols_row, optionsVisible);
            for (CheckBox option: itsOptions) {
                setOptionLenVisible(option);
            }
            setCustomSymbolsVisible();
            setTextView(R.id.symbols_default, defaultSymbols);

            if (!init) {
                itsValidator.validate();
            }
        }


        /** Set whether an option is used */
        private final void setOption(CheckBox option, boolean use, boolean init)
        {
            if (init) {
                option.setChecked(use);
                option.setOnCheckedChangeListener(new OnCheckedChangeListener()
                {
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked)
                    {
                        setOption((CheckBox)buttonView, isChecked, false);
                    }
                });
            }

            setOptionLenVisible(option);
            if (option.getId() == R.id.symbols) {
                setCustomSymbolsVisible();
            }

            if (!init) {
                itsValidator.validate();
            }
        }


        /** Set the custom symbols option */
        private final void setCustomSymbolsOption(boolean useCustom,
                                                  boolean init)
        {
            if (init) {
                itsUseCustomSymbols.setChecked(useCustom);
                itsUseCustomSymbols.setOnCheckedChangeListener(
                    new OnCheckedChangeListener()
                    {
                        public void onCheckedChanged(CompoundButton buttonView,
                                                     boolean isChecked)
                        {
                            setCustomSymbolsOption(isChecked, false);
                        }
                    });
            }

            View defView = itsView.findViewById(R.id.symbols_default);
            if (useCustom) {
                defView.setVisibility(View.GONE);
                itsCustomSymbolsEdit.setVisibility(View.VISIBLE);
                itsCustomSymbolsEdit.requestFocus();
            } else {
                defView.setVisibility(View.VISIBLE);
                itsCustomSymbolsEdit.setVisibility(View.GONE);
            }

            if (!init) {
                itsValidator.validate();
            }
        }


        /** Set the visibility of the custom symbols options */
        private final void setCustomSymbolsVisible()
        {
            boolean visible = false;
            switch (itsType) {
            case NORMAL:
            case PRONOUNCEABLE: {
                CheckBox cb = (CheckBox)itsView.findViewById(R.id.symbols);
                visible = cb.isChecked();
                break;
            }
            case EASY_TO_READ:
            case HEXADECIMAL: {
                visible = false;
                break;
            }
            }
            setVisible(R.id.custom_symbols_set, visible);
        }


        /** Set the visibility on an option's length field */
        private final void setOptionLenVisible(CheckBox option)
        {
            boolean visible;
            if (itsType == PasswdPolicy.Type.NORMAL) {
                visible = option.isChecked();
            } else {
                visible = false;
            }

            int labelId = 0;
            int lengthId = 0;
            switch (option.getId()) {
            case R.id.lowercase: {
                labelId = R.id.lowercase_label;
                lengthId = R.id.lowercase_len;
                break;
            }
            case R.id.uppercase: {
                labelId = R.id.uppercase_label;
                lengthId = R.id.uppercase_len;
                break;
            }
            case R.id.digits: {
                labelId = R.id.digits_label;
                lengthId = R.id.digits_len;
                break;
            }
            case R.id.symbols: {
                labelId = R.id.symbols_label;
                lengthId = R.id.symbols_len;
                break;
            }
            }

            if (labelId != 0) {
                setVisible(labelId, visible);
                setVisible(lengthId, visible);
            }
        }


        /** Set the visibility of a view */
        private final void setVisible(int id, boolean visible)
        {
            View v = itsView.findViewById(id);
            v.setVisibility(visible ? View.VISIBLE : View.GONE);
        }


        /** Get an integer value from a text view */
        private final int getTextViewInt(TextView tv)
            throws NumberFormatException
        {
            return Integer.valueOf(tv.getText().toString(), 10);
        }


        /** Set a text view to an integer value */
        private final void setTextView(TextView tv, int value)
        {
            tv.setText(Integer.toString(value));
        }


        /** Set a text view to a value */
        private final void setTextView(int id, String value)
        {
            TextView tv = (TextView)itsView.findViewById(id);
            tv.setText(value);
        }
    }
}