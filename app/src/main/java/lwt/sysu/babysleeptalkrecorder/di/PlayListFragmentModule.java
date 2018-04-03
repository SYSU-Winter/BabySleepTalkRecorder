package lwt.sysu.babysleeptalkrecorder.di;

import dagger.Module;
import dagger.Provides;
import lwt.sysu.babysleeptalkrecorder.di.scopes.FragmentScope;
import lwt.sysu.babysleeptalkrecorder.playlist.PlayListMVPView;
import lwt.sysu.babysleeptalkrecorder.playlist.PlayListPresenter;
import lwt.sysu.babysleeptalkrecorder.playlist.PlayListPresenterImpl;
import io.reactivex.disposables.CompositeDisposable;

@Module
class PlayListFragmentModule {

    @Provides
    @FragmentScope
    PlayListPresenter<PlayListMVPView> providePlayListPresenter(PlayListPresenterImpl<PlayListMVPView> playListPresenter) {
        return playListPresenter;
    }

    @Provides
    @FragmentScope
    CompositeDisposable provideCompositeDisposable() {
        return new CompositeDisposable();
    }
}
