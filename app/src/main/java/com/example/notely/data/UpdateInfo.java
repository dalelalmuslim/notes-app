package com.example.notely.data;

import com.example.notely.model.AppVersion;

/**
 * Result of an update check. {@link #available} is true only when the latest
 * remote release is a newer semantic version and there is at least one safe
 * (validated HTTPS) destination to open.
 */
public final class UpdateInfo {

    public final AppVersion latestVersion;
    public final String tagName;
    public final String releaseNotes;
    public final String updateUrl;
    public final String apkUrl;
    public final boolean available;

    UpdateInfo(AppVersion latestVersion, String tagName, String releaseNotes,
               String updateUrl, String apkUrl, boolean available) {
        this.latestVersion = latestVersion;
        this.tagName = tagName;
        this.releaseNotes = releaseNotes;
        this.updateUrl = updateUrl;
        this.apkUrl = apkUrl;
        this.available = available;
    }
}
