package lwt.sysu.babysleeptalkrecorder.di;

import android.content.Context;

import dagger.Module;
import dagger.Provides;
import lwt.sysu.babysleeptalkrecorder.activities.MainActivity;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ActivityContext;
import lwt.sysu.babysleeptalkrecorder.di.scopes.ActivityScope;

@Module
public class MainActivityModule {
    @Provides
    @ActivityContext
    @ActivityScope
    Context provideActivityContext(MainActivity activity) {
        return activity;
    }
}
