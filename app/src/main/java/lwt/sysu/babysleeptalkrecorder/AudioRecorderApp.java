package lwt.sysu.babysleeptalkrecorder;

import android.app.Activity;
import android.app.Application;
import android.app.Service;
import com.orhanobut.hawk.Hawk;
import dagger.android.AndroidInjector;
import dagger.android.DispatchingAndroidInjector;
import dagger.android.HasActivityInjector;
import dagger.android.HasServiceInjector;
import lwt.sysu.babysleeptalkrecorder.di.ApplicationComponent;
import lwt.sysu.babysleeptalkrecorder.di.DaggerApplicationComponent;
import javax.inject.Inject;

public class AudioRecorderApp extends Application implements HasActivityInjector, HasServiceInjector{
    private ApplicationComponent applicationComponent;

    @Inject DispatchingAndroidInjector<Activity> dispatchingAndroidActivityInjector;
    @Inject DispatchingAndroidInjector<Service> dispatchingAndroidServiceInjector;

    @Override public void onCreate() {
        super.onCreate();

        Hawk.init(getApplicationContext()).build();
        DaggerApplicationComponent.builder().application(this).build().inject(this);
    }

    @Override public AndroidInjector<Activity> activityInjector() {
        return dispatchingAndroidActivityInjector;
    }

    @Override public AndroidInjector<Service> serviceInjector() {
        return dispatchingAndroidServiceInjector;
    }
}