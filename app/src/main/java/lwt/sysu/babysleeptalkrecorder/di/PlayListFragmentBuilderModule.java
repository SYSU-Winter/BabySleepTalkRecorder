package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import lwt.sysu.babysleeptalkrecorder.di.scopes.FragmentScope;
import lwt.sysu.babysleeptalkrecorder.playlist.PlayListFragment;

@Module
abstract class PlayListFragmentBuilderModule {
    @FragmentScope
    @ContributesAndroidInjector(modules = {PlayListFragmentModule.class})
    abstract PlayListFragment contributePlayListFragment();
}
