package com.example.notely.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.notely.BuildConfig;
import com.example.notely.model.AppVersion;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Lightweight, optional, non-blocking update checker.
 *
 * The checker runs on a background worker (never the main thread) and reports
 * through {@link ResultCallback}. Every failure mode is silent and safe: no
 * network, GitHub unavailable, malformed metadata, or a timeout all simply
 * produce a {@code null} result and the app continues normally. A network
 * failure is never reported as an available update.
 *
 * Throttling: the last check attempt time is persisted, so at most one request
 * is made per 24 hours. This avoids repeatedly annoying the user and avoids
 * repeated requests when the device is offline.
 *
 * Version discovery comes from the Gradle-configured versionName at runtime,
 * never from a hardcoded string. Only HTTPS is used, against the GitHub
 * Releases API host for dalelalmuslim/notes-app.
 */
public final class UpdateChecker {

    private static final String TAG = "UpdateChecker";
    private static final String PREFS_NAME = "notely_updates";
    private static final String KEY_LAST_CHECK_MS = "last_check_ms";
    private static final long CHECK_INTERVAL_MS = 24L * 60 * 60 * 1000;
    private static final int TIMEOUT_MS = 5000;
    private static final String API_HOST = "api.github.com";
    private static final String API_URL =
            "https://api.github.com/repos/dalelalmuslim/notes-app/releases/latest";

    private final Context context;
    private final SharedPreferences prefs;

    public UpdateChecker(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    /**
     * Checks for a newer release. The callback fires on the main thread with a
     * result of {@code null} when throttled, unreachable, or malformed, and
     * with an {@link UpdateInfo} (available or not) after a successful fetch.
     */
    public void check(final ResultCallback<UpdateInfo> callback) {
        final long now = System.currentTimeMillis();
        if (now - prefs.getLong(KEY_LAST_CHECK_MS, 0L) < CHECK_INTERVAL_MS) {
            callback.onResult(null);
            return;
        }
        AppExecutors.runOnNetworkIo(new Runnable() {
            @Override
            public void run() {
                final UpdateInfo info = fetchLatestRelease();
                AppExecutors.postOnMain(new Runnable() {
                    @Override
                    public void run() {
                        prefs.edit().putLong(KEY_LAST_CHECK_MS, now).apply();
                        callback.onResult(info);
                    }
                });
            }
        });
    }

    private UpdateInfo fetchLatestRelease() {
        try {
            AppVersion current = AppVersion.parse(currentVersionName());
            if (current == null) {
                return null;
            }
            String json = fetchLatestReleaseJson();
            if (json == null) {
                return null;
            }
            return GithubReleasesParser.parse(json, current);
        } catch (Exception e) {
            Log.w(TAG, "Update check failed");
            return null;
        }
    }

    private String currentVersionName() {
        return BuildConfig.VERSION_NAME;
    }

    private String fetchLatestReleaseJson() {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(API_URL);
            if (!"https".equalsIgnoreCase(url.getProtocol())
                    || !API_HOST.equalsIgnoreCase(url.getHost())) {
                return null;
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(TIMEOUT_MS);
            connection.setReadTimeout(TIMEOUT_MS);
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/vnd.github+json");
            connection.setRequestProperty("User-Agent", "Notely-Android");
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                return null;
            }
            InputStream in = connection.getInputStream();
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                return new String(out.toByteArray(), StandardCharsets.UTF_8);
            } finally {
                in.close();
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not reach GitHub release API");
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
