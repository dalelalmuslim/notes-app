package com.example.notely.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.example.notely.model.AppVersion;

import org.junit.Test;

public class GithubReleasesParserTest {

    private static final String VALID_JSON =
            "{\"tag_name\":\"v1.1.0\","
                    + "\"body\":\"Fixes\\nand improvements\","
                    + "\"html_url\":\"https://github.com/dalelalmuslim/notes-app/releases/tag/v1.1.0\","
                    + "\"assets\":[{"
                    + "\"name\":\"app-debug.apk\","
                    + "\"browser_download_url\":\"https://github.com/dalelalmuslim/notes-app/releases/download/v1.1.0/app-debug.apk\""
                    + "}]}";

    @Test
    public void parsesValidResponseAndDetectsNewerVersion() {
        UpdateInfo info = GithubReleasesParser.parse(VALID_JSON, AppVersion.parse("1.0.0"));
        assertNotNull(info);
        assertTrue(info.available);
        assertEquals("1.1.0", info.latestVersion.toString());
        assertEquals("v1.1.0", info.tagName);
        assertEquals("Fixes\nand improvements", info.releaseNotes);
        assertEquals("https://github.com/dalelalmuslim/notes-app/releases/tag/v1.1.0", info.updateUrl);
        assertTrue(info.apkUrl.endsWith(".apk"));
    }

    @Test
    public void noUpdateWhenVersionsEqual() {
        UpdateInfo info = GithubReleasesParser.parse(VALID_JSON, AppVersion.parse("1.1.0"));
        assertNotNull(info);
        assertFalse(info.available);
    }

    @Test
    public void noUpdateWhenLatestIsOlder() {
        UpdateInfo info = GithubReleasesParser.parse(VALID_JSON, AppVersion.parse("2.0.0"));
        assertNotNull(info);
        assertFalse(info.available);
    }

    @Test
    public void malformedJson_returnsNull() {
        assertNull(GithubReleasesParser.parse(null, AppVersion.parse("1.0.0")));
        assertNull(GithubReleasesParser.parse("not json", AppVersion.parse("1.0.0")));
        assertNull(GithubReleasesParser.parse("{bad json", AppVersion.parse("1.0.0")));
    }

    @Test
    public void missingTagName_returnsNull() {
        assertNull(GithubReleasesParser.parse("{\"body\":\"x\"}", AppVersion.parse("1.0.0")));
    }

    @Test
    public void malformedVersionTag_returnsNull() {
        assertNull(GithubReleasesParser.parse(
                "{\"tag_name\":\"not-a-version\",\"html_url\":\"https://github.com/x\"}",
                AppVersion.parse("1.0.0")));
    }

    @Test
    public void nonHttpsDestination_makesUpdateUnavailable() {
        String json = "{\"tag_name\":\"v2.0.0\","
                + "\"html_url\":\"http://evil.example.com/releases/2\","
                + "\"assets\":[]}";
        UpdateInfo info = GithubReleasesParser.parse(json, AppVersion.parse("1.0.0"));
        assertNotNull(info);
        assertNull(info.updateUrl);
        assertNull(info.apkUrl);
        assertFalse(info.available);
    }

    @Test
    public void unsafeApkAsset_isRejectedButSafeUpdatePageStillShown() {
        String json = "{\"tag_name\":\"v2.0.0\","
                + "\"html_url\":\"https://github.com/dalelalmuslim/notes-app/releases/tag/v2.0.0\","
                + "\"assets\":[{\"name\":\"x.apk\",\"browser_download_url\":\"http://evil.com/x.apk\"}]}";
        UpdateInfo info = GithubReleasesParser.parse(json, AppVersion.parse("1.0.0"));
        assertNotNull(info);
        assertNull(info.apkUrl);
        assertEquals("https://github.com/dalelalmuslim/notes-app/releases/tag/v2.0.0", info.updateUrl);
        assertTrue(info.available);
    }

    @Test
    public void isSafeHttpUrl_validatesSchemeAndHost() {
        assertTrue(GithubReleasesParser.isSafeHttpUrl("https://github.com/a/b"));
        assertTrue(GithubReleasesParser.isSafeHttpUrl("https://api.github.com/a/b"));
        assertTrue(GithubReleasesParser.isSafeHttpUrl("https://release-assets.githubusercontent.com/a"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl("http://github.com/a"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl("https://evil.com/a"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl("https://github.com.evil.com/a"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl("not a url"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl("https://github.com/ev il"));
        assertFalse(GithubReleasesParser.isSafeHttpUrl(null));
    }
}
