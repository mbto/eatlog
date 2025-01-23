/*
 * This file is generated by jOOQ.
 */
package com.github.mbto.eatlog.common.model.eatlog.tables.records;


import com.github.mbto.eatlog.common.model.eatlog.tables.Account;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.TreeSet;

import org.jooq.Field;
import org.jooq.Record1;
import org.jooq.Record12;
import org.jooq.Row12;
import org.jooq.impl.UpdatableRecordImpl;
import org.jooq.types.UInteger;
import org.jooq.types.UShort;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class AccountRecord extends UpdatableRecordImpl<AccountRecord> implements Record12<UInteger, LocalDateTime, LocalDateTime, String, String, String, String, TreeSet<String>, UInteger, Boolean, String, UShort> {

    private static final long serialVersionUID = 1L;

    /**
     * Setter for <code>eatlog.account.id</code>.
     */
    public void setId(UInteger value) {
        set(0, value);
    }

    /**
     * Getter for <code>eatlog.account.id</code>.
     */
    public UInteger getId() {
        return (UInteger) get(0);
    }

    /**
     * Setter for <code>eatlog.account.created_at</code>.
     */
    public void setCreatedAt(LocalDateTime value) {
        set(1, value);
    }

    /**
     * Getter for <code>eatlog.account.created_at</code>.
     */
    public LocalDateTime getCreatedAt() {
        return (LocalDateTime) get(1);
    }

    /**
     * Setter for <code>eatlog.account.lastauth_at</code>.
     */
    public void setLastauthAt(LocalDateTime value) {
        set(2, value);
    }

    /**
     * Getter for <code>eatlog.account.lastauth_at</code>.
     */
    public LocalDateTime getLastauthAt() {
        return (LocalDateTime) get(2);
    }

    /**
     * Setter for <code>eatlog.account.name</code>.
     */
    public void setName(String value) {
        set(3, value);
    }

    /**
     * Getter for <code>eatlog.account.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return (String) get(3);
    }

    /**
     * Setter for <code>eatlog.account.google_sub</code>.
     */
    public void setGoogleSub(String value) {
        set(4, value);
    }

    /**
     * Getter for <code>eatlog.account.google_sub</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getGoogleSub() {
        return (String) get(4);
    }

    /**
     * Setter for <code>eatlog.account.google_picture_url</code>.
     */
    public void setGooglePictureUrl(String value) {
        set(5, value);
    }

    /**
     * Getter for <code>eatlog.account.google_picture_url</code>.
     */
    @Size(max = 255)
    public String getGooglePictureUrl() {
        return (String) get(5);
    }

    /**
     * Setter for <code>eatlog.account.locale_segments</code>. Example value:
     * 'ru' or 'en' or 'en_US' or others languages with/without country
     */
    public void setLocaleSegments(String value) {
        set(6, value);
    }

    /**
     * Getter for <code>eatlog.account.locale_segments</code>. Example value:
     * 'ru' or 'en' or 'en_US' or others languages with/without country
     */
    @Size(max = 15)
    public String getLocaleSegments() {
        return (String) get(6);
    }

    /**
     * Setter for <code>eatlog.account.roles</code>.
     */
    public void setRoles(TreeSet<String> value) {
        set(7, value);
    }

    /**
     * Getter for <code>eatlog.account.roles</code>.
     */
    public TreeSet<String> getRoles() {
        return (TreeSet<String>) get(7);
    }

    /**
     * Setter for <code>eatlog.account.geoname_id</code>.
     */
    public void setGeonameId(UInteger value) {
        set(8, value);
    }

    /**
     * Getter for <code>eatlog.account.geoname_id</code>.
     */
    public UInteger getGeonameId() {
        return (UInteger) get(8);
    }

    /**
     * Setter for <code>eatlog.account.is_banned</code>.
     */
    public void setIsBanned(Boolean value) {
        set(9, value);
    }

    /**
     * Getter for <code>eatlog.account.is_banned</code>.
     */
    public Boolean getIsBanned() {
        return (Boolean) get(9);
    }

    /**
     * Setter for <code>eatlog.account.banned_reason</code>.
     */
    public void setBannedReason(String value) {
        set(10, value);
    }

    /**
     * Getter for <code>eatlog.account.banned_reason</code>.
     */
    @Size(max = 255)
    public String getBannedReason() {
        return (String) get(10);
    }

    /**
     * Setter for <code>eatlog.account.grade_eatlog</code>.
     */
    public void setGradeEatlog(UShort value) {
        set(11, value);
    }

    /**
     * Getter for <code>eatlog.account.grade_eatlog</code>.
     */
    public UShort getGradeEatlog() {
        return (UShort) get(11);
    }

    // -------------------------------------------------------------------------
    // Primary key information
    // -------------------------------------------------------------------------

    @Override
    public Record1<UInteger> key() {
        return (Record1) super.key();
    }

    // -------------------------------------------------------------------------
    // Record12 type implementation
    // -------------------------------------------------------------------------

    @Override
    public Row12<UInteger, LocalDateTime, LocalDateTime, String, String, String, String, TreeSet<String>, UInteger, Boolean, String, UShort> fieldsRow() {
        return (Row12) super.fieldsRow();
    }

    @Override
    public Row12<UInteger, LocalDateTime, LocalDateTime, String, String, String, String, TreeSet<String>, UInteger, Boolean, String, UShort> valuesRow() {
        return (Row12) super.valuesRow();
    }

    @Override
    public Field<UInteger> field1() {
        return Account.ACCOUNT.ID;
    }

    @Override
    public Field<LocalDateTime> field2() {
        return Account.ACCOUNT.CREATED_AT;
    }

    @Override
    public Field<LocalDateTime> field3() {
        return Account.ACCOUNT.LASTAUTH_AT;
    }

    @Override
    public Field<String> field4() {
        return Account.ACCOUNT.NAME;
    }

    @Override
    public Field<String> field5() {
        return Account.ACCOUNT.GOOGLE_SUB;
    }

    @Override
    public Field<String> field6() {
        return Account.ACCOUNT.GOOGLE_PICTURE_URL;
    }

    @Override
    public Field<String> field7() {
        return Account.ACCOUNT.LOCALE_SEGMENTS;
    }

    @Override
    public Field<TreeSet<String>> field8() {
        return Account.ACCOUNT.ROLES;
    }

    @Override
    public Field<UInteger> field9() {
        return Account.ACCOUNT.GEONAME_ID;
    }

    @Override
    public Field<Boolean> field10() {
        return Account.ACCOUNT.IS_BANNED;
    }

    @Override
    public Field<String> field11() {
        return Account.ACCOUNT.BANNED_REASON;
    }

    @Override
    public Field<UShort> field12() {
        return Account.ACCOUNT.GRADE_EATLOG;
    }

    @Override
    public UInteger component1() {
        return getId();
    }

    @Override
    public LocalDateTime component2() {
        return getCreatedAt();
    }

    @Override
    public LocalDateTime component3() {
        return getLastauthAt();
    }

    @Override
    public String component4() {
        return getName();
    }

    @Override
    public String component5() {
        return getGoogleSub();
    }

    @Override
    public String component6() {
        return getGooglePictureUrl();
    }

    @Override
    public String component7() {
        return getLocaleSegments();
    }

    @Override
    public TreeSet<String> component8() {
        return getRoles();
    }

    @Override
    public UInteger component9() {
        return getGeonameId();
    }

    @Override
    public Boolean component10() {
        return getIsBanned();
    }

    @Override
    public String component11() {
        return getBannedReason();
    }

    @Override
    public UShort component12() {
        return getGradeEatlog();
    }

    @Override
    public UInteger value1() {
        return getId();
    }

    @Override
    public LocalDateTime value2() {
        return getCreatedAt();
    }

    @Override
    public LocalDateTime value3() {
        return getLastauthAt();
    }

    @Override
    public String value4() {
        return getName();
    }

    @Override
    public String value5() {
        return getGoogleSub();
    }

    @Override
    public String value6() {
        return getGooglePictureUrl();
    }

    @Override
    public String value7() {
        return getLocaleSegments();
    }

    @Override
    public TreeSet<String> value8() {
        return getRoles();
    }

    @Override
    public UInteger value9() {
        return getGeonameId();
    }

    @Override
    public Boolean value10() {
        return getIsBanned();
    }

    @Override
    public String value11() {
        return getBannedReason();
    }

    @Override
    public UShort value12() {
        return getGradeEatlog();
    }

    @Override
    public AccountRecord value1(UInteger value) {
        setId(value);
        return this;
    }

    @Override
    public AccountRecord value2(LocalDateTime value) {
        setCreatedAt(value);
        return this;
    }

    @Override
    public AccountRecord value3(LocalDateTime value) {
        setLastauthAt(value);
        return this;
    }

    @Override
    public AccountRecord value4(String value) {
        setName(value);
        return this;
    }

    @Override
    public AccountRecord value5(String value) {
        setGoogleSub(value);
        return this;
    }

    @Override
    public AccountRecord value6(String value) {
        setGooglePictureUrl(value);
        return this;
    }

    @Override
    public AccountRecord value7(String value) {
        setLocaleSegments(value);
        return this;
    }

    @Override
    public AccountRecord value8(TreeSet<String> value) {
        setRoles(value);
        return this;
    }

    @Override
    public AccountRecord value9(UInteger value) {
        setGeonameId(value);
        return this;
    }

    @Override
    public AccountRecord value10(Boolean value) {
        setIsBanned(value);
        return this;
    }

    @Override
    public AccountRecord value11(String value) {
        setBannedReason(value);
        return this;
    }

    @Override
    public AccountRecord value12(UShort value) {
        setGradeEatlog(value);
        return this;
    }

    @Override
    public AccountRecord values(UInteger value1, LocalDateTime value2, LocalDateTime value3, String value4, String value5, String value6, String value7, TreeSet<String> value8, UInteger value9, Boolean value10, String value11, UShort value12) {
        value1(value1);
        value2(value2);
        value3(value3);
        value4(value4);
        value5(value5);
        value6(value6);
        value7(value7);
        value8(value8);
        value9(value9);
        value10(value10);
        value11(value11);
        value12(value12);
        return this;
    }

    // -------------------------------------------------------------------------
    // Constructors
    // -------------------------------------------------------------------------

    /**
     * Create a detached AccountRecord
     */
    public AccountRecord() {
        super(Account.ACCOUNT);
    }

    /**
     * Create a detached, initialised AccountRecord
     */
    public AccountRecord(UInteger id, LocalDateTime createdAt, LocalDateTime lastauthAt, String name, String googleSub, String googlePictureUrl, String localeSegments, TreeSet<String> roles, UInteger geonameId, Boolean isBanned, String bannedReason, UShort gradeEatlog) {
        super(Account.ACCOUNT);

        setId(id);
        setCreatedAt(createdAt);
        setLastauthAt(lastauthAt);
        setName(name);
        setGoogleSub(googleSub);
        setGooglePictureUrl(googlePictureUrl);
        setLocaleSegments(localeSegments);
        setRoles(roles);
        setGeonameId(geonameId);
        setIsBanned(isBanned);
        setBannedReason(bannedReason);
        setGradeEatlog(gradeEatlog);
    }

    /**
     * Create a detached, initialised AccountRecord
     */
    public AccountRecord(com.github.mbto.eatlog.common.model.eatlog.tables.pojos.Account value) {
        super(Account.ACCOUNT);

        if (value != null) {
            setId(value.getId());
            setCreatedAt(value.getCreatedAt());
            setLastauthAt(value.getLastauthAt());
            setName(value.getName());
            setGoogleSub(value.getGoogleSub());
            setGooglePictureUrl(value.getGooglePictureUrl());
            setLocaleSegments(value.getLocaleSegments());
            setRoles(value.getRoles());
            setGeonameId(value.getGeonameId());
            setIsBanned(value.getIsBanned());
            setBannedReason(value.getBannedReason());
            setGradeEatlog(value.getGradeEatlog());
        }
    }
}
