package com.example.notely.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.example.notely.R;
import com.example.notely.data.UpdateInfo;

/**
 * Shared presentation for update results.
 *
 * Everything the update system may show the user is centralized here so the
 * main screen and Settings behave identically. Only validated information is
 * ever presented, and any destination opened is the HTTPS GitHub URL validated
 * by the release parser. The app never downloads, executes, or installs files.
 */
public final class UpdatePrompts {

    private static final int MAX_RELEASE_NOTES_LENGTH = 400;

    private UpdatePrompts() {
    }

    /**
     * Shows the "new version available" dialog for {@code info}.
     */
    public static void showAvailable(final Activity activity, final UpdateInfo info) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.update_title)
                .setMessage(buildMessage(activity, info))
                .setPositiveButton(R.string.update_action,
                        new android.content.DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(android.content.DialogInterface dialog, int which) {
                                openDestination(activity, info);
                            }
                        })
                .setNegativeButton(R.string.update_later, null)
                .show();
    }

    /**
     * Builds the localized message describing the available update.
     */
    public static String buildMessage(Context context, UpdateInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append(context.getString(R.string.update_current_version,
                currentVersionName(context)));
        sb.append('\n');
        sb.append(context.getString(R.string.update_latest_version,
                info.latestVersion.toString()));
        if (info.releaseNotes != null && !info.releaseNotes.trim().isEmpty()) {
            String notes = info.releaseNotes.trim();
            if (notes.length() > MAX_RELEASE_NOTES_LENGTH) {
                notes = notes.substring(0, MAX_RELEASE_NOTES_LENGTH) + "…";
            }
            sb.append("\n\n");
            sb.append(context.getString(R.string.update_release_notes, notes));
        }
        return sb.toString();
    }

    /**
     * Opens the validated update destination, or explains why that was not
     * possible. Never opens an unvalidated URL.
     */
    public static void openDestination(Context context, UpdateInfo info) {
        String url = info.updateUrl != null ? info.updateUrl : info.apkUrl;
        if (url == null) {
            Toast.makeText(context, R.string.update_unavailable, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (Exception e) {
            Toast.makeText(context, R.string.update_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private static String currentVersionName(Context context) {
        try {
            return context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
