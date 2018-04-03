package lwt.sysu.babysleeptalkrecorder.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BaseActivity;
import lwt.sysu.babysleeptalkrecorder.settings.SettingsFragment;

public class SettingsActivity extends BaseActivity {
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.action_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowHomeEnabled(true);
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, new SettingsFragment())
                .commit();
    }
}
