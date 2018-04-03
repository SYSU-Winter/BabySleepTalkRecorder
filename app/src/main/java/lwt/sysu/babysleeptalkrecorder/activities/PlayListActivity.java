package lwt.sysu.babysleeptalkrecorder.activities;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;

import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.support.HasSupportFragmentInjector;
import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BaseActivity;
import lwt.sysu.babysleeptalkrecorder.playlist.PlayListFragment;

import javax.inject.Inject;

public class PlayListActivity extends BaseActivity implements HasSupportFragmentInjector {

    @Inject
    DispatchingAndroidInjector<Fragment> dispatchingAndroidInjector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_record_list);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.tab_title_saved_recordings);
            // 给左上角图标的左边加一个返回图标
            actionBar.setDisplayHomeAsUpEnabled(true);
            // 使左上角图标显示
            actionBar.setDisplayShowHomeEnabled(true);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.record_list_container, PlayListFragment.newInstance())
                    .commit();
        }
    }

    @Override
    public AndroidInjector<Fragment> supportFragmentInjector() {
        return dispatchingAndroidInjector;
    }
}
