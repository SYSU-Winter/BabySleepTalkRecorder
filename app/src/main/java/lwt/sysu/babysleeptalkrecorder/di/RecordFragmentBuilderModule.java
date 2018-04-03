package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import lwt.sysu.babysleeptalkrecorder.sleeptalkrecording.RecordFragment;
import lwt.sysu.babysleeptalkrecorder.di.scopes.FragmentScope;

@Module
abstract class RecordFragmentBuilderModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = {RecordFragmentModule.class})
    abstract RecordFragment contributeRecordFragment();
}
