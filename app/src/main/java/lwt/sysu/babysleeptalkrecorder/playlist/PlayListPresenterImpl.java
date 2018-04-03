package lwt.sysu.babysleeptalkrecorder.playlist;

import android.os.Environment;

import lwt.sysu.babysleeptalkrecorder.db.RecordItemDataSource;
import lwt.sysu.babysleeptalkrecorder.db.RecordingItem;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BasePresenter;
import io.reactivex.Flowable;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class PlayListPresenterImpl<V extends PlayListMVPView> extends BasePresenter<V>
        implements PlayListPresenter<V> {
    private static final int INVALID_ITEM = -1;
    private static final int PROGRESS_OFFSET = 20;
    @Inject
    public RecordItemDataSource recordItemDataSource;

    private int currentPlayingItem;
    private boolean isAudioPlaying = false;
    private boolean isAudioPaused = false;
    private List<RecordingItem> recordingItems = new ArrayList<>();

    @Inject
    public PlayListPresenterImpl(CompositeDisposable compositeDisposable) {
        super(compositeDisposable);
    }

    @Override
    public void onViewInitialised() {
        fillAdapter();
    }

    @Override
    public void renameFile(int adapterPosition, String value) {
        rename(recordingItems.get(adapterPosition), adapterPosition, value).subscribe(
                new SingleObserver<Integer>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(Integer position) {
                        // 重命名成功之后刷新显示列表
                        getAttachedView().notifyListItemChange(position);
                    }

                    @Override
                    public void onError(Throwable e) {
                        // 重命名失败则弹出错误信息(toast提示)
                        getAttachedView().showError(e.getMessage());
                    }
                });
    }

    private Single<Integer> rename(final RecordingItem recordingItem, final int adapterPosition, final String name) {
        return Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                File newFile = new File(
                        Environment.getExternalStorageDirectory().getAbsolutePath() + "/SoundRecorder/" + name);
                // 如果新名字已存在或者是重命名之后的path不是一个目录则报一个错误信息
                if (newFile.exists() && !newFile.isDirectory()) {
                    e.onError(new Exception("File with same name already exists"));
                } else {
                    File oldFilePath = new File(recordingItem.getFilePath());
                    if (oldFilePath.renameTo(newFile)) {
                        // 如果重命名成功，则需要重新设置录音文件项的名字为新名字
                        recordingItem.setName(name);
                        // 重新设置文件目录
                        recordingItem.setFilePath(newFile.getPath());
                        // 更新数据库
                        recordItemDataSource.updateRecordItem(recordingItem);
                        // 回调成功接口
                        e.onSuccess(adapterPosition);
                    } else {
                        e.onError(new Throwable("Cannot Rename file. Please try again"));
                    }
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void deleteFile(int position) {
        removeFile(recordingItems.get(position), position).subscribe(new SingleObserver<Integer>() {
            @Override
            public void onSubscribe(Disposable d) {

            }

            @Override
            public void onSuccess(Integer position) {
                // 删除成功之后更新列表
                getAttachedView().notifyListItemRemove(position);
            }

            @Override
            public void onError(Throwable e) {
                getAttachedView().showError(e.getMessage());
            }
        });
    }

    @Override
    public RecordingItem getListItemAt(int position) {
        // 返回录音文件在列表中的位置
        return recordingItems.get(position);
    }

    // 当前录音播放状态为停止状态
    @Override
    public void mediaPlayerStopped() {
        updateStateToStop();
    }

    private void updateStateToStop() {
        if (playProgressDisposable != null) {
            playProgressDisposable.dispose();
        }
        isAudioPlaying = false;
        isAudioPaused = false;
        currentProgress = 0;
        RecordingItem currentItem = recordingItems.get(currentPlayingItem);
        currentItem.isPlaying = false;
        currentItem.playProgress = 0;
        getAttachedView().notifyListItemChange(currentPlayingItem);
        currentPlayingItem = INVALID_ITEM;
    }

    @Override
    public void onListItemClick(int position) {
        try {
            if (isAudioPlaying) { // 播放的状态
                if (currentPlayingItem == position) {
                    if (isAudioPaused) { // 如果现在是处于暂停状态则恢复播放
                        isAudioPaused = false;
                        getAttachedView().resumeMediaPlayer(position);
                        recordingItems.get(position).isPlaying = true;
                        updateProgress(position);
                    } else {
                        // 如果当前处于正在播放的状态则转成暂停
                        isAudioPaused = true;
                        getAttachedView().pauseMediaPlayer(position);
                        recordingItems.get(position).isPlaying = false;
                        playProgressDisposable.dispose();
                    }
                } else {
                    // 如果当前播放的录音项不是点击项，则停止当前播放的录音项
                    getAttachedView().stopMediaPlayer(currentPlayingItem);
                    updateStateToStop();
                    startPlayer(position);
                }
            } else {
                startPlayer(position);
            }
            getAttachedView().notifyListItemChange(position);
        } catch (IOException e) {
            getAttachedView().showError("Failed to start media Player");
        }
    }

    private long currentProgress = 0;

    // 开始播放
    private void startPlayer(int position) throws IOException {
        isAudioPlaying = true;
        currentProgress = 0;
        recordingItems.get(position).isPlaying = true;
        getAttachedView().startMediaPlayer(position, recordingItems.get(position));
        currentPlayingItem = position;
        updateProgress(position);
    }

    @Override
    public void onListItemLongClick(int position) {
        getAttachedView().showFileOptionDialog(position, recordingItems.get(position));
    }

    @Override
    public int getListItemCount() {
        return recordingItems.size();
    }

    @Override
    public void shareFileClicked(int position) {
        getAttachedView().shareFileDialog(recordingItems.get(position).getFilePath());
    }

    @Override
    public void renameFileClicked(int position) {
        getAttachedView().showRenameFileDialog(position);
    }

    @Override
    public void deleteFileClicked(int position) {
        getAttachedView().showDeleteFileDialog(position);
    }

    private DisposableSubscriber<Long> playProgressDisposable;

    // 更新进度
    private void updateProgress(final int position) {
        playProgressDisposable = Flowable.interval(PROGRESS_OFFSET, TimeUnit.MILLISECONDS)
                .onBackpressureDrop()
                .map(new Function<Long, Long>() {
                    @Override
                    public Long apply(Long aLong) throws Exception {
                        currentProgress += PROGRESS_OFFSET;
                        recordingItems.get(position).playProgress = currentProgress;
                        getAttachedView().updateProgressInListItem(position);
                        return currentProgress / 1000;
                    }
                })
                .distinctUntilChanged()
                .subscribeOn(Schedulers.computation())
                .subscribeWith(new DisposableSubscriber<Long>() {
                    @Override
                    public void onNext(Long aLong) {
                        getAttachedView().updateTimerInListItem(position);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
        getCompositeDisposable().add(playProgressDisposable);
    }

    // 文件的删除操作
    private Single<Integer> removeFile(final RecordingItem recordingItem, final int position) {
        return Single.create(new SingleOnSubscribe<Integer>() {
            @Override
            public void subscribe(SingleEmitter<Integer> e) throws Exception {
                // 获取需要删除的文件
                File file = new File(recordingItem.getFilePath());
                if (file.delete()) {
                    // 如果文件删除成功，需要对数据库以及容器列表进行更新
                    recordItemDataSource.deleteRecordItem(recordingItem);
                    recordingItems.remove(position);
                    e.onSuccess(position);
                } else {
                    e.onError(new Exception("File deletion failed"));
                }
            }
        }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public void onDetach() {
        getAttachedView().stopMediaPlayer(currentPlayingItem);
        super.onDetach();
    }

    // 将数据库中存储的所有录音添加到容器中
    private void fillAdapter() {
        getCompositeDisposable().add(recordItemDataSource.getAllRecordings()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<RecordingItem>>() {
                    @Override
                    public void accept(List<RecordingItem> items) throws Exception {
                        if (items.size() > 0) {
                            recordingItems.addAll(items);
                            getAttachedView().notifyListAdapter();
                        } else { /// 如果当前列表为空那么把显示列表视图隐藏，显示空列表视图
                            getAttachedView().setRecordingListInVisible();
                            getAttachedView().setEmptyLabelVisible();
                        }
                    }
                }));
    }
}
