package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.Provides;
import lwt.sysu.babysleeptalkrecorder.sleeptalkrecording.AudioRecordMVPView;
import lwt.sysu.babysleeptalkrecorder.sleeptalkrecording.AudioRecordPresenter;
import lwt.sysu.babysleeptalkrecorder.sleeptalkrecording.AudioRecordPresenterImpl;
import lwt.sysu.babysleeptalkrecorder.di.scopes.FragmentScope;
import io.reactivex.disposables.CompositeDisposable;

@Module
class RecordFragmentModule {

    @Provides
    @FragmentScope
    AudioRecordPresenter<AudioRecordMVPView> provideAudioRecordPresenter(
            AudioRecordPresenterImpl<AudioRecordMVPView> audioRecordPresenter) {
        return audioRecordPresenter;
    }

    @Provides
    @FragmentScope
    CompositeDisposable provideCompositeDisposable() {
        return new CompositeDisposable();
    }
}
