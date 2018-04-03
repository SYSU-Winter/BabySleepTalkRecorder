package lwt.sysu.babysleeptalkrecorder.sleeptalkrecording;

import android.content.Context;

import lwt.sysu.babysleeptalkrecorder.AppConstants;
import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ActivityContext;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BasePresenter;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecorder;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Consumer;

import java.util.Locale;

import javax.inject.Inject;

public class AudioRecordPresenterImpl<V extends AudioRecordMVPView> extends BasePresenter<V>
        implements AudioRecordPresenter<V> {

    @Inject
    @ActivityContext
    public Context mContext;
    private boolean mIsRecording = false;
    private boolean mIsRecordingPaused = false;

    @Inject
    public AudioRecordPresenterImpl(CompositeDisposable compositeDisposable) {
        super(compositeDisposable);
    }

    @Override
    public void onToggleRecodingStatus() {
        if (!mIsRecording) {
            mIsRecording = true;
            getAttachedView().startServiceAndBind();
            getAttachedView().toggleRecordButton();
            getAttachedView().setPauseButtonVisible();
            getAttachedView().togglePauseStatus();
            getAttachedView().setScreenOnFlag();
        } else {
            stopRecording();
        }
    }

    @Override
    public void onTogglePauseStatus() {
        getAttachedView().setPauseButtonVisible();
        mIsRecordingPaused = !mIsRecordingPaused;
        if (mIsRecordingPaused) {
            getAttachedView().pauseRecord();
        } else {
            getAttachedView().resumeRecord();
        }
    }

    @Override
    public boolean isRecording() {
        return mIsRecording;
    }

    @Override
    public boolean isPaused() {
        return mIsRecordingPaused;
    }

    @Override
    public void onAttach(V view) {
        super.onAttach(view);
        getAttachedView().bindToService();
    }

    @Override
    public void onViewInitialised() {
        getAttachedView().updateChronometer(getChronometerText(new AudioRecorder.RecordTime()));
        getAttachedView().toggleRecordButton();
    }

    @Override
    public void onDetach() {
        getAttachedView().unbindFromService();
        super.onDetach();
    }

    private void stopRecording() {
        mIsRecording = false;
        mIsRecordingPaused = false;
        getAttachedView().stopServiceAndUnBind();
        getAttachedView().toggleRecordButton();
        getAttachedView().clearScreenOnFlag();
        getAttachedView().updateChronometer(getChronometerText(new AudioRecorder.RecordTime()));
        getAttachedView().setPauseButtonInVisible();
        getAttachedView().togglePauseStatus();
    }

    private final Consumer<AudioRecorder.RecordTime> recordTimeConsumer = new Consumer<AudioRecorder.RecordTime>() {
        @Override
        public void accept(AudioRecorder.RecordTime recordTime) throws Exception {
            getAttachedView().updateChronometer(getChronometerText(recordTime));
        }
    };

    private String getChronometerText(AudioRecorder.RecordTime recordTime) {
        return String.format(Locale.getDefault(), mContext.getString(R.string.record_time_format),
                recordTime.hours,
                recordTime.minutes,
                recordTime.seconds);
    }

    @Override
    public void onServiceStatusAvailable(boolean isRecoding, boolean isRecordingPaused) {
        mIsRecording = isRecoding;
        mIsRecordingPaused = isRecordingPaused;
        if (mIsRecording) {
            getAttachedView().setPauseButtonVisible();
            getAttachedView().togglePauseStatus();
            getAttachedView().toggleRecordButton();
            getCompositeDisposable().add(getAttachedView().subscribeForTimer(recordTimeConsumer));
        } else {
            getAttachedView().unbindFromService();
        }
    }

    @Override
    public void onServiceUpdateReceived(String actionExtra) {
        switch (actionExtra) {
            case AppConstants.ACTION_PAUSE:
                mIsRecordingPaused = true;
                getAttachedView().togglePauseStatus();
                break;
            case AppConstants.ACTION_RESUME:
                mIsRecordingPaused = false;
                getAttachedView().togglePauseStatus();
                break;
            case AppConstants.ACTION_STOP:
                stopRecording();
                break;
        }
    }
}
