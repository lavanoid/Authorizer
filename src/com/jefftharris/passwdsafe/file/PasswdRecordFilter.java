/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.pwsafe.lib.file.PwsRecord;

import android.content.Context;
import android.text.format.DateUtils;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.util.Utils;

/** A filter for records */
public final class PasswdRecordFilter
{
    /** Type of filter */
    public enum Type
    {
        QUERY,
        EXPIRATION
    }

    /** Expiration filter type */
    public enum ExpiryFilter
    {
        // Order must match expire_filters string array
        EXPIRED,
        TODAY,
        IN_A_WEEK,
        IN_A_MONTH,
        IN_A_YEAR,
        ANY,
        CUSTOM;

        /** Get the filter value from its value index */
        public static ExpiryFilter fromIdx(int idx)
        {
            if ((idx >= 0) && (idx < values().length)) {
                return values()[idx];
            }
            return ANY;
        }
    }

    /** Default options to match */
    public static final int OPTS_DEFAULT =          0;
    /** Record can not have an alias referencing it */
    public static final int OPTS_NO_ALIAS =         1 << 0;
    /** Record can not have a shortcut referencing it */
    public static final int OPTS_NO_SHORTCUT =      1 << 1;

    /** Filter type */
    private final Type itsType;

    /** Regex to match on various fields */
    public final Pattern itsSearchQuery;

    /** Expiration filter type */
    private final ExpiryFilter itsExpiryFilter;

    /** The expiration time to match on a record's expiration */
    private final long itsExpiryAtMillis;

    /** Filter options */
    public final int itsOptions;

    // TODO: make all fields private

    public static final String QUERY_MATCH = "";
    private String QUERY_MATCH_TITLE;
    private String QUERY_MATCH_USERNAME;
    private String QUERY_MATCH_URL;
    private String QUERY_MATCH_EMAIL;
    private String QUERY_MATCH_NOTES;

    /** Constructor for a query */
    public PasswdRecordFilter(Pattern query, int opts)
    {
        itsType = Type.QUERY;
        itsSearchQuery = query;
        itsExpiryFilter = ExpiryFilter.ANY;
        itsExpiryAtMillis = 0;
        itsOptions = opts;
    }

    /** Constructor for expiration */
    public PasswdRecordFilter(ExpiryFilter filter, Date customDate, int opts)
    {
        itsType = Type.EXPIRATION;
        itsSearchQuery = null;
        itsExpiryFilter = filter;
        Calendar expiry = Calendar.getInstance();
        switch (itsExpiryFilter) {
        case EXPIRED: {
            break;
        }
        case TODAY: {
            expiry.add(Calendar.DAY_OF_MONTH, 1);
            expiry.set(Calendar.HOUR_OF_DAY, 0);
            expiry.set(Calendar.MINUTE, 0);
            expiry.set(Calendar.SECOND, 0);
            expiry.set(Calendar.MILLISECOND, 0);
            break;
        }
        case IN_A_WEEK: {
            expiry.add(Calendar.WEEK_OF_YEAR, 1);
            break;
        }
        case IN_A_MONTH: {
            expiry.add(Calendar.MONTH, 1);
            break;
        }
        case IN_A_YEAR: {
            expiry.add(Calendar.YEAR, 1);
            break;
        }
        case ANY: {
            expiry.setTimeInMillis(Long.MAX_VALUE);
            break;
        }
        case CUSTOM: {
            expiry.setTime(customDate);
            break;
        }
        }

        itsExpiryAtMillis = expiry.getTimeInMillis();
        itsOptions = opts;
    }


    /**
     * Filter a record
     * @return A non-null string if the record matches the filter; null if it
     * does not
     */
    public final String filterRecord(PwsRecord rec,
                                     PasswdFileData fileData,
                                     Context ctx)
    {
        String queryMatch = null;
        switch (itsType) {
        case QUERY: {
            if (itsSearchQuery != null) {
                if (QUERY_MATCH_TITLE == null) {
                    QUERY_MATCH_TITLE = ctx.getString(R.string.title);
                    QUERY_MATCH_USERNAME = ctx.getString(R.string.username);
                    QUERY_MATCH_URL = ctx.getString(R.string.url);
                    QUERY_MATCH_EMAIL = ctx.getString(R.string.email);
                    QUERY_MATCH_NOTES = ctx.getString(R.string.notes);
                }

                if (filterField(fileData.getTitle(rec))) {
                    queryMatch = QUERY_MATCH_TITLE;
                } else if (filterField(fileData.getUsername(rec))) {
                    queryMatch = QUERY_MATCH_USERNAME;
                } else if (filterField(fileData.getURL(rec))) {
                    queryMatch = QUERY_MATCH_URL;
                } else if (filterField(fileData.getEmail(rec))) {
                    queryMatch = QUERY_MATCH_EMAIL;
                } else if (filterField(fileData.getNotes(rec))) {
                    queryMatch = QUERY_MATCH_NOTES;
                }
            } else {
                queryMatch = QUERY_MATCH;
            }
            break;
        }
        case EXPIRATION: {
            Date expiry = fileData.getPasswdExpiryTime(rec);
            if (expiry == null) {
                break;
            }
            long expire = expiry.getTime();
            if (expire < itsExpiryAtMillis) {
                queryMatch = DateUtils.getRelativeDateTimeString(
                    ctx, expire, DateUtils.HOUR_IN_MILLIS,
                    DateUtils.WEEK_IN_MILLIS, 0).toString();
            }
            break;
        }
        }

        if ((queryMatch != null) &&
            (itsOptions != PasswdRecordFilter.OPTS_DEFAULT)) {
            PasswdRecord passwdRec = fileData.getPasswdRecord(rec);
            if (passwdRec != null) {
                for (PwsRecord ref: passwdRec.getRefsToRecord()) {
                    PasswdRecord passwdRef = fileData.getPasswdRecord(ref);
                    if (passwdRef == null) {
                        continue;
                    }
                    switch (passwdRef.getType()) {
                    case NORMAL: {
                        break;
                    }
                    case ALIAS: {
                        if (hasOptions(PasswdRecordFilter.OPTS_NO_ALIAS)) {
                            queryMatch = null;
                        }
                        break;
                    }
                    case SHORTCUT: {
                        if (hasOptions(PasswdRecordFilter.OPTS_NO_SHORTCUT)) {
                            queryMatch = null;
                        }
                        break;
                    }
                    }
                    if (queryMatch == null) {
                        break;
                    }
                }
            }
        }

        return queryMatch;
    }


    /** Does the record filter have a search query */
    public final boolean hasSearchQuery()
    {
        switch (itsType) {
        case QUERY: {
            return itsSearchQuery != null;
        }
        case EXPIRATION: {
            return true;
        }
        }
        return true;
    }


    /** Convert the filter to a string */
    public final String toString(Context ctx)
    {
        switch (itsType) {
        case QUERY: {
            if (itsSearchQuery != null) {
                return itsSearchQuery.pattern();
            }
            break;
        }
        case EXPIRATION: {
            switch (itsExpiryFilter) {
            case EXPIRED: {
                return ctx.getString(R.string.password_expired);
            }
            case TODAY: {
                return ctx.getString(R.string.password_expires_today);
            }
            case IN_A_WEEK:
            case IN_A_MONTH:
            case IN_A_YEAR:
            case CUSTOM: {
                return ctx.getString(
                    R.string.password_expires_before,
                    Utils.formatDate(itsExpiryAtMillis, ctx, true, true));
            }
            case ANY: {
                return ctx.getString(R.string.password_with_expiration);
            }
            }
        }
        }
        return "";
    }


    /** Does the filter have the given options */
    private final boolean hasOptions(int opts)
    {
        return (itsOptions & opts) != 0;
    }


    /** Match a field against the search query */
    private final boolean filterField(String field)
    {
        if (field != null) {
            Matcher m = itsSearchQuery.matcher(field);
            return m.find();
        } else {
            return false;
        }
    }
}
