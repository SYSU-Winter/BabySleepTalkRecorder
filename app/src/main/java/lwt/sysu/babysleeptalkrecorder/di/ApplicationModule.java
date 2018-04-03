package lwt.sysu.babysleeptalkrecorder.di;

import android.app.Application;
import android.content.Context;

import dagger.Module;
import dagger.Provides;
import lwt.sysu.babysleeptalkrecorder.db.AppDataBase;
import lwt.sysu.babysleeptalkrecorder.db.RecordItemDataSource;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ApplicationContext;

import javax.inject.Singleton;

@Module
public class ApplicationModule {

    @Provides
    @ApplicationContext
    @Singleton
    Context provideApplicationContext(Application application) {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    RecordItemDataSource provideRecordItemDataSource(@ApplicationContext Context context) {
        return AppDataBase.getInstance(context).getRecordItemDataSource();
    }
}
