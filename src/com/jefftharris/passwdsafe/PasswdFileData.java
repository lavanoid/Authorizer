package com.jefftharris.passwdsafe;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.pwsafe.lib.exception.EndOfFileException;
import org.pwsafe.lib.exception.InvalidPassphraseException;
import org.pwsafe.lib.exception.UnsupportedFileVersionException;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileFactory;
import org.pwsafe.lib.file.PwsFileV3;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV3;

import android.util.Log;

public class PasswdFileData
{
    public String itsFileName;
    public PwsFile itsPwsFile;
    public final ArrayList<Map<String, String>> itsGroupData =
        new ArrayList<Map<String, String>>();
    public final ArrayList<ArrayList<HashMap<String, Object>>> itsChildData =
        new ArrayList<ArrayList<HashMap<String, Object>>>();

    static final String RECORD = "record";
    static final String TITLE = "title";
    static final String GROUP = "group";

    private static final String TAG = "PasswdFileData";

    public PasswdFileData(String fileName, StringBuilder passwd)
        throws Exception
    {
        itsFileName = fileName;
        loadFile(passwd);
   }

    public void close()
    {
        itsFileName = null;
        itsPwsFile.dispose();
        itsGroupData.clear();
        itsChildData.clear();
    }

    public PwsRecord getRecord(int groupPos, int childPos)
    {
        ArrayList<HashMap<String, Object>> groupChildren =
            itsChildData.get(groupPos);
        HashMap<String, Object> child = groupChildren.get(childPos);
        return (PwsRecord)child.get(RECORD);
    }

    public final String getGroup(PwsRecord rec)
    {
        if (itsPwsFile != null) {
            switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.GROUP).toString();
            default:
                return "TODO";
            }
        } else {
            return "";
        }
        // TODO: Fill in other versions
    }

    public final String getTitle(PwsRecord rec)
    {
        if (itsPwsFile != null) {
            switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.TITLE).toString();
            default:
                return "TODO";
            }
        } else {
            return "";
        }
    }

    public final String getUUID(PwsRecord rec)
    {
        if (itsPwsFile != null) {
            switch (itsPwsFile.getFileVersionMajor()) {
            case PwsFileV3.VERSION:
                return rec.getField(PwsRecordV3.UUID).toString();
            default:
                return "TODO";
            }
        } else {
            return "";
        }
    }

    private void loadFile(StringBuilder passwd)
        throws IOException, NoSuchAlgorithmException,
            EndOfFileException, InvalidPassphraseException,
            UnsupportedFileVersionException
    {
        Log.i(TAG, "before load file");
        itsPwsFile = PwsFileFactory.loadFile(itsFileName, passwd);
        passwd.delete(0, passwd.length());
        passwd = null;
        Log.i(TAG, "after load file");

        TreeMap<String, ArrayList<PwsRecord>> recsByGroup =
            new TreeMap<String, ArrayList<PwsRecord>>();
        Iterator<PwsRecord> recIter = itsPwsFile.getRecords();
        while (recIter.hasNext()) {
            PwsRecord rec = recIter.next();
            String group = getGroup(rec);
            ArrayList<PwsRecord> groupList = recsByGroup.get(group);
            if (groupList == null) {
                groupList = new ArrayList<PwsRecord>();
                recsByGroup.put(group, groupList);
            }
            groupList.add(rec);
        }
        Log.i(TAG, "groups sorted");

        RecordMapComparator comp = new RecordMapComparator();
        for (Map.Entry<String, ArrayList<PwsRecord>> entry :
                recsByGroup.entrySet()) {
            Log.i(TAG, "process group:" + entry.getKey());
            Map<String, String> groupInfo =
                Collections.singletonMap(GROUP, entry.getKey());
            itsGroupData.add(groupInfo);

            ArrayList<HashMap<String, Object>> children =
                new ArrayList<HashMap<String, Object>>();
            for (PwsRecord rec : entry.getValue()) {
                HashMap<String, Object> recInfo = new HashMap<String, Object>();
                recInfo.put(TITLE, getTitle(rec));
                recInfo.put(RECORD, rec);
                children.add(recInfo);
            }

            Collections.sort(children, comp);
            itsChildData.add(children);
        }
        Log.i(TAG, "file loaded");
    }

    static final class RecordMapComparator implements
                    Comparator<HashMap<String, Object>>
    {
        public int compare(HashMap<String, Object> arg0,
                           HashMap<String, Object> arg1)
        {
            String title0 = arg0.get(TITLE).toString();
            String title1 = arg1.get(TITLE).toString();
            return title0.compareTo(title1);
        }
    }
}
