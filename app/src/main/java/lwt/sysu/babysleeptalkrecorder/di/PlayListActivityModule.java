package lwt.sysu.babysleeptalkrecorder.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import lwt.sysu.babysleeptalkrecorder.activities.PlayListActivity;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ActivityContext;
import lwt.sysu.babysleeptalkrecorder.di.scopes.ActivityScope;

@Module
class PlayListActivityModule {
    @Provides
    @ActivityContext
    @ActivityScope
    Context provideActivityContext(PlayListActivity activity) {
        return activity;
    }
}
