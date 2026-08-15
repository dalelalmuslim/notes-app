package com.example.notely.data;

import com.example.notely.model.AppVersion;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Parses the GitHub Releases API "latest release" JSON payload.
 *
 * All values come from a remote endpoint, so they are validated before use:
 * the version tag must be a valid semantic version and any URL that the app
 * might hand to another app must be HTTPS on a GitHub-owned host. Malformed
 * or missing data never throws: {@link #parse(String, AppVersion)} returns
 * {@code null} for unreadable input and a {@link UpdateInfo} with
 * {@code available == false} when there is nothing safe to act on.
 */
public final class GithubReleasesParser {

    private GithubReleasesParser() {
    }

    public static UpdateInfo parse(String json, AppVersion currentVersion) {
        if (json == null || currentVersion == null) {
            return null;
        }

        Object root;
        try {
            root = Json.parse(json);
        } catch (Json.JsonParseException e) {
            return null;
        }
        if (!(root instanceof Map)) {
            return null;
        }
        Map<?, ?> map = (Map<?, ?>) root;

        String tagName = asString(map.get("tag_name"));
        if (tagName == null) {
            return null;
        }
        AppVersion latest = AppVersion.parse(tagName);
        if (latest == null) {
            return null;
        }

        String releaseNotes = asString(map.get("body"));
        String updateUrl = asString(map.get("html_url"));
        if (!isSafeHttpUrl(updateUrl)) {
            updateUrl = null;
        }
        String apkUrl = findApkUrl(map.get("assets"));

        boolean newer = latest.compareTo(currentVersion) > 0;
        boolean available = newer && (updateUrl != null || apkUrl != null);
        return new UpdateInfo(latest, tagName, releaseNotes, updateUrl, apkUrl, available);
    }

    private static String findApkUrl(Object assets) {
        if (!(assets instanceof List)) {
            return null;
        }
        for (Object item : (List<?>) assets) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> asset = (Map<?, ?>) item;
            String name = asString(asset.get("name"));
            String url = asString(asset.get("browser_download_url"));
            if (name != null
                    && name.toLowerCase(Locale.US).endsWith(".apk")
                    && isSafeHttpUrl(url)) {
                return url;
            }
        }
        return null;
    }

    private static String asString(Object value) {
        return value instanceof String ? (String) value : null;
    }

    /**
     * Validates that a URL is HTTPS and points at a GitHub-owned host.
     * Used before the app opens any remote destination supplied by the API.
     */
    static boolean isSafeHttpUrl(String url) {
        if (url == null) {
            return false;
        }
        try {
            java.net.URI uri = new java.net.URI(url);
            if (!"https".equalsIgnoreCase(uri.getScheme())) {
                return false;
            }
            String host = uri.getHost();
            if (host == null) {
                return false;
            }
            String h = host.toLowerCase(Locale.US);
            return h.equals("github.com")
                    || h.endsWith(".github.com")
                    || h.equals("githubusercontent.com")
                    || h.endsWith(".githubusercontent.com");
        } catch (Exception e) {
            return false;
        }
    }
}
