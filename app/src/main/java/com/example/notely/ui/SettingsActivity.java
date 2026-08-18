package com.example.notely.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.notely.R;
import com.example.notely.data.AppSettings;
import com.example.notely.data.NoteRepository;
import com.example.notely.data.ResultCallback;
import com.example.notely.data.UpdateChecker;
import com.example.notely.data.UpdateInfo;
import com.example.notely.model.Note;

import java.util.List;

/**
 * Settings screen: language, theme, trash, updates and about information.
 *
 * Language and theme changes persist through {@link AppSettings} and take
 * effect immediately by recreating this activity; the other activities pick
 * the change up when they resume via {@link BaseActivity}.
 */
public final class SettingsActivity extends BaseActivity {

    private AppSettings settings;
    private UpdateChecker updateChecker;
    private NoteRepository repository;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        settings = AppSettings.get(this);
        updateChecker = new UpdateChecker(this);
        repository = new NoteRepository(this);

        TextView headerTitle = findViewById(R.id.header_title);
        headerTitle.setText(R.string.settings_title);
        findViewById(R.id.header_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        initLanguage();
        initTheme();
        initTrash();

        TextView aboutVersion = findViewById(R.id.about_version);
        aboutVersion.setText(getString(R.string.about_version, versionName()));

        findViewById(R.id.settings_check_updates).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkForUpdates();
            }
        });
    }

    private void initTrash() {
        findViewById(R.id.trash_row).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SettingsActivity.this, TrashActivity.class));
            }
        });
        final TextView countView = findViewById(R.id.trash_count);
        repository.getTrashedNotes(new ResultCallback<List<Note>>() {
            @Override
            public void onResult(List<Note> notes) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                countView.setText(String.valueOf(notes.size()));
            }
        });
    }

    private void initLanguage() {
        RadioGroup group = findViewById(R.id.language_group);
        int checked = settings.isArabic() ? R.id.language_arabic : R.id.language_english;
        ((RadioButton) findViewById(checked)).setChecked(true);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rg, int checkedId) {
                settings.setLanguage(checkedId == R.id.language_arabic
                        ? AppSettings.LANGUAGE_ARABIC : AppSettings.LANGUAGE_ENGLISH);
                recreate();
            }
        });
    }

    private void initTheme() {
        RadioGroup group = findViewById(R.id.theme_group);
        int checked = R.id.theme_system;
        String theme = settings.getTheme();
        if (AppSettings.THEME_LIGHT.equals(theme)) {
            checked = R.id.theme_light;
        } else if (AppSettings.THEME_DARK.equals(theme)) {
            checked = R.id.theme_dark;
        }
        ((RadioButton) findViewById(checked)).setChecked(true);
        group.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup rg, int checkedId) {
                settings.setTheme(themeKey(checkedId));
                recreate();
            }
        });
    }

    private static String themeKey(int checkedId) {
        if (checkedId == R.id.theme_light) {
            return AppSettings.THEME_LIGHT;
        }
        if (checkedId == R.id.theme_dark) {
            return AppSettings.THEME_DARK;
        }
        return AppSettings.THEME_SYSTEM;
    }

    private void checkForUpdates() {
        final Button button = findViewById(R.id.settings_check_updates);
        button.setEnabled(false);
        button.setText(R.string.update_checking);
        updateChecker.checkNow(new ResultCallback<UpdateInfo>() {
            @Override
            public void onResult(UpdateInfo info) {
                if (isFinishing() || isDestroyed()) {
                    return;
                }
                button.setEnabled(true);
                button.setText(R.string.settings_check_for_updates);
                if (info == null) {
                    Toast.makeText(SettingsActivity.this, R.string.update_check_failed,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                if (!info.available) {
                    Toast.makeText(SettingsActivity.this, R.string.update_up_to_date,
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                UpdatePrompts.showAvailable(SettingsActivity.this, info);
            }
        });
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "?";
        }
    }
}
