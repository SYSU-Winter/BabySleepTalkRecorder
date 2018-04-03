package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import lwt.sysu.babysleeptalkrecorder.activities.MainActivity;
import lwt.sysu.babysleeptalkrecorder.activities.PlayListActivity;
import lwt.sysu.babysleeptalkrecorder.activities.SettingsActivity;
import lwt.sysu.babysleeptalkrecorder.di.scopes.ActivityScope;

@Module
abstract class ActivityBuilderModule {
    @ActivityScope
    @ContributesAndroidInjector(modules = {MainActivityModule.class, RecordFragmentBuilderModule.class})
    abstract MainActivity contributeMainActivity();

    @ActivityScope
    @ContributesAndroidInjector(modules = {PlayListActivityModule.class, PlayListFragmentBuilderModule.class})
    abstract PlayListActivity contributePlayListActivity();

    @ContributesAndroidInjector()
    abstract SettingsActivity contributeSettingsActivity();
}
