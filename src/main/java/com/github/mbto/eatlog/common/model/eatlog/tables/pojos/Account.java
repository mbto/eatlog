/*
 * This file is generated by jOOQ.
 */
package com.github.mbto.eatlog.common.model.eatlog.tables.pojos;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.TreeSet;

import org.jooq.types.UInteger;
import org.jooq.types.UShort;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class Account implements Serializable {

    private static final long serialVersionUID = 1L;

    private UInteger id;
    private LocalDateTime createdAt;
    private LocalDateTime lastauthAt;
    private String name;
    private String googleSub;
    private String googlePictureUrl;
    private String localeSegments;
    private TreeSet<String> roles;
    private UInteger geonameId;
    private Boolean isBanned;
    private String bannedReason;
    private UShort gradeEatlog;

    public Account() {}

    public Account(Account value) {
        this.id = value.id;
        this.createdAt = value.createdAt;
        this.lastauthAt = value.lastauthAt;
        this.name = value.name;
        this.googleSub = value.googleSub;
        this.googlePictureUrl = value.googlePictureUrl;
        this.localeSegments = value.localeSegments;
        this.roles = value.roles;
        this.geonameId = value.geonameId;
        this.isBanned = value.isBanned;
        this.bannedReason = value.bannedReason;
        this.gradeEatlog = value.gradeEatlog;
    }

    public Account(
        UInteger id,
        LocalDateTime createdAt,
        LocalDateTime lastauthAt,
        String name,
        String googleSub,
        String googlePictureUrl,
        String localeSegments,
        TreeSet<String> roles,
        UInteger geonameId,
        Boolean isBanned,
        String bannedReason,
        UShort gradeEatlog
    ) {
        this.id = id;
        this.createdAt = createdAt;
        this.lastauthAt = lastauthAt;
        this.name = name;
        this.googleSub = googleSub;
        this.googlePictureUrl = googlePictureUrl;
        this.localeSegments = localeSegments;
        this.roles = roles;
        this.geonameId = geonameId;
        this.isBanned = isBanned;
        this.bannedReason = bannedReason;
        this.gradeEatlog = gradeEatlog;
    }

    /**
     * Getter for <code>eatlog.account.id</code>.
     */
    public UInteger getId() {
        return this.id;
    }

    /**
     * Setter for <code>eatlog.account.id</code>.
     */
    public void setId(UInteger id) {
        this.id = id;
    }

    /**
     * Getter for <code>eatlog.account.created_at</code>.
     */
    public LocalDateTime getCreatedAt() {
        return this.createdAt;
    }

    /**
     * Setter for <code>eatlog.account.created_at</code>.
     */
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Getter for <code>eatlog.account.lastauth_at</code>.
     */
    public LocalDateTime getLastauthAt() {
        return this.lastauthAt;
    }

    /**
     * Setter for <code>eatlog.account.lastauth_at</code>.
     */
    public void setLastauthAt(LocalDateTime lastauthAt) {
        this.lastauthAt = lastauthAt;
    }

    /**
     * Getter for <code>eatlog.account.name</code>.
     */
    @NotNull
    @Size(max = 32)
    public String getName() {
        return this.name;
    }

    /**
     * Setter for <code>eatlog.account.name</code>.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Getter for <code>eatlog.account.google_sub</code>.
     */
    @NotNull
    @Size(max = 255)
    public String getGoogleSub() {
        return this.googleSub;
    }

    /**
     * Setter for <code>eatlog.account.google_sub</code>.
     */
    public void setGoogleSub(String googleSub) {
        this.googleSub = googleSub;
    }

    /**
     * Getter for <code>eatlog.account.google_picture_url</code>.
     */
    @Size(max = 255)
    public String getGooglePictureUrl() {
        return this.googlePictureUrl;
    }

    /**
     * Setter for <code>eatlog.account.google_picture_url</code>.
     */
    public void setGooglePictureUrl(String googlePictureUrl) {
        this.googlePictureUrl = googlePictureUrl;
    }

    /**
     * Getter for <code>eatlog.account.locale_segments</code>. Example value:
     * 'ru' or 'en' or 'en_US' or others languages with/without country
     */
    @Size(max = 15)
    public String getLocaleSegments() {
        return this.localeSegments;
    }

    /**
     * Setter for <code>eatlog.account.locale_segments</code>. Example value:
     * 'ru' or 'en' or 'en_US' or others languages with/without country
     */
    public void setLocaleSegments(String localeSegments) {
        this.localeSegments = localeSegments;
    }

    /**
     * Getter for <code>eatlog.account.roles</code>.
     */
    public TreeSet<String> getRoles() {
        return this.roles;
    }

    /**
     * Setter for <code>eatlog.account.roles</code>.
     */
    public void setRoles(TreeSet<String> roles) {
        this.roles = roles;
    }

    /**
     * Getter for <code>eatlog.account.geoname_id</code>.
     */
    public UInteger getGeonameId() {
        return this.geonameId;
    }

    /**
     * Setter for <code>eatlog.account.geoname_id</code>.
     */
    public void setGeonameId(UInteger geonameId) {
        this.geonameId = geonameId;
    }

    /**
     * Getter for <code>eatlog.account.is_banned</code>.
     */
    public Boolean getIsBanned() {
        return this.isBanned;
    }

    /**
     * Setter for <code>eatlog.account.is_banned</code>.
     */
    public void setIsBanned(Boolean isBanned) {
        this.isBanned = isBanned;
    }

    /**
     * Getter for <code>eatlog.account.banned_reason</code>.
     */
    @Size(max = 255)
    public String getBannedReason() {
        return this.bannedReason;
    }

    /**
     * Setter for <code>eatlog.account.banned_reason</code>.
     */
    public void setBannedReason(String bannedReason) {
        this.bannedReason = bannedReason;
    }

    /**
     * Getter for <code>eatlog.account.grade_eatlog</code>.
     */
    public UShort getGradeEatlog() {
        return this.gradeEatlog;
    }

    /**
     * Setter for <code>eatlog.account.grade_eatlog</code>.
     */
    public void setGradeEatlog(UShort gradeEatlog) {
        this.gradeEatlog = gradeEatlog;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final Account other = (Account) obj;
        if (this.id == null) {
            if (other.id != null)
                return false;
        }
        else if (!this.id.equals(other.id))
            return false;
        if (this.createdAt == null) {
            if (other.createdAt != null)
                return false;
        }
        else if (!this.createdAt.equals(other.createdAt))
            return false;
        if (this.lastauthAt == null) {
            if (other.lastauthAt != null)
                return false;
        }
        else if (!this.lastauthAt.equals(other.lastauthAt))
            return false;
        if (this.name == null) {
            if (other.name != null)
                return false;
        }
        else if (!this.name.equals(other.name))
            return false;
        if (this.googleSub == null) {
            if (other.googleSub != null)
                return false;
        }
        else if (!this.googleSub.equals(other.googleSub))
            return false;
        if (this.googlePictureUrl == null) {
            if (other.googlePictureUrl != null)
                return false;
        }
        else if (!this.googlePictureUrl.equals(other.googlePictureUrl))
            return false;
        if (this.localeSegments == null) {
            if (other.localeSegments != null)
                return false;
        }
        else if (!this.localeSegments.equals(other.localeSegments))
            return false;
        if (this.roles == null) {
            if (other.roles != null)
                return false;
        }
        else if (!this.roles.equals(other.roles))
            return false;
        if (this.geonameId == null) {
            if (other.geonameId != null)
                return false;
        }
        else if (!this.geonameId.equals(other.geonameId))
            return false;
        if (this.isBanned == null) {
            if (other.isBanned != null)
                return false;
        }
        else if (!this.isBanned.equals(other.isBanned))
            return false;
        if (this.bannedReason == null) {
            if (other.bannedReason != null)
                return false;
        }
        else if (!this.bannedReason.equals(other.bannedReason))
            return false;
        if (this.gradeEatlog == null) {
            if (other.gradeEatlog != null)
                return false;
        }
        else if (!this.gradeEatlog.equals(other.gradeEatlog))
            return false;
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.id == null) ? 0 : this.id.hashCode());
        result = prime * result + ((this.createdAt == null) ? 0 : this.createdAt.hashCode());
        result = prime * result + ((this.lastauthAt == null) ? 0 : this.lastauthAt.hashCode());
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result + ((this.googleSub == null) ? 0 : this.googleSub.hashCode());
        result = prime * result + ((this.googlePictureUrl == null) ? 0 : this.googlePictureUrl.hashCode());
        result = prime * result + ((this.localeSegments == null) ? 0 : this.localeSegments.hashCode());
        result = prime * result + ((this.roles == null) ? 0 : this.roles.hashCode());
        result = prime * result + ((this.geonameId == null) ? 0 : this.geonameId.hashCode());
        result = prime * result + ((this.isBanned == null) ? 0 : this.isBanned.hashCode());
        result = prime * result + ((this.bannedReason == null) ? 0 : this.bannedReason.hashCode());
        result = prime * result + ((this.gradeEatlog == null) ? 0 : this.gradeEatlog.hashCode());
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Account (");

        sb.append(id);
        sb.append(", ").append(createdAt);
        sb.append(", ").append(lastauthAt);
        sb.append(", ").append(name);
        sb.append(", ").append(googleSub);
        sb.append(", ").append(googlePictureUrl);
        sb.append(", ").append(localeSegments);
        sb.append(", ").append(roles);
        sb.append(", ").append(geonameId);
        sb.append(", ").append(isBanned);
        sb.append(", ").append(bannedReason);
        sb.append(", ").append(gradeEatlog);

        sb.append(")");
        return sb.toString();
    }
}
