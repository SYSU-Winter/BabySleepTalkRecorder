package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecordService;

@Module
abstract public class ServiceBuilderModule {
    @ContributesAndroidInjector(modules = {ServiceModule.class})
    abstract AudioRecordService contributeAudioRecordService();
}
