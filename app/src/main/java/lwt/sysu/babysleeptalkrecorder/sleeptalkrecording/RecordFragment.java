package lwt.sysu.babysleeptalkrecorder.sleeptalkrecording;

import android.animation.FloatEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;

import com.jakewharton.rxbinding2.view.RxView;

import jaygoo.widget.wlv.WaveLineView;
import lwt.sysu.babysleeptalkrecorder.AppConstants;
import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.activities.PlayListActivity;
import lwt.sysu.babysleeptalkrecorder.activities.SettingsActivity;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ActivityContext;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BaseFragment;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecordService;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecorder;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import javax.inject.Inject;

public class RecordFragment extends BaseFragment implements AudioRecordMVPView {
    private static final String LOG_TAG = RecordFragment.class.getSimpleName();
    private FloatingActionButton mRecordButton = null;
    private FloatingActionButton mPauseButton = null;

    private TextView chronometer;
    private boolean mIsServiceBound = false;
    private AudioRecordService mAudioRecordService;
    private ObjectAnimator alphaAnimator;
    private FloatingActionButton mSettingsButton;
    private FloatingActionButton mPlayListBtn;

    private WaveLineView waveLineView;

    @Inject
    @ActivityContext
    public Context mContext;

    @Inject
    public AudioRecordPresenter<AudioRecordMVPView> audioRecordPresenter;

    public static RecordFragment newInstance() {
        return new RecordFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        audioRecordPresenter.onAttach(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View recordView = inflater.inflate(R.layout.fragment_record, container, false);
        initViews(recordView);
        bindEvents();
        return recordView;
    }

    // 绑定主页面4个按键的实现功能
    private void bindEvents() {
        RxView.clicks(mRecordButton).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                audioRecordPresenter.onToggleRecodingStatus();
            }
        });

        RxView.clicks(mSettingsButton).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                startActivity(new Intent(mContext, SettingsActivity.class));
            }
        });

        RxView.clicks(mPlayListBtn).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                startActivity(new Intent(mContext, PlayListActivity.class));
            }
        });

        RxView.clicks(mPauseButton).subscribe(new Consumer<Object>() {
            @Override
            public void accept(Object o) throws Exception {
                audioRecordPresenter.onTogglePauseStatus();
            }
        });
    }

    private void initViews(View recordView) {
        chronometer = recordView.findViewById(R.id.chronometer);

        mSettingsButton = recordView.findViewById(R.id.settings_btn);
        mPlayListBtn = recordView.findViewById(R.id.play_list_btn);
        mRecordButton = recordView.findViewById(R.id.btnRecord);
        mPauseButton = recordView.findViewById(R.id.btnPause);
        mPauseButton.setVisibility(View.GONE); //hide pause button before recording starts

        waveLineView = recordView.findViewById(R.id.waveLineView);

        // 上方计时器的动画效果（属性动画）
        alphaAnimator =
                ObjectAnimator.ofObject(chronometer, "alpha", new FloatEvaluator(), 0.2f);
        alphaAnimator.setRepeatMode(ValueAnimator.REVERSE);
        alphaAnimator.setRepeatCount(ValueAnimator.INFINITE);
        audioRecordPresenter.onViewInitialised();
    }

    // 正在录音的状态，设置图标，取消闪烁动画
    private void setAsPauseBtn() {
        alphaAnimator.cancel();
        chronometer.setAlpha(1.0f);
        mPauseButton.setImageResource(R.drawable.ic_media_pause);
        waveLineView.onResume();
    }

    // 现在暂停的状态，闪烁计时器
    private void setAsResumeBtn() {
        alphaAnimator.start();
        mPauseButton.setImageResource(R.drawable.ic_media_record);
        waveLineView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        audioRecordPresenter.onDetach();
        waveLineView.release();
    }


    // 更新计时器
    @Override
    public void updateChronometer(String text) {
        chronometer.setText(text);
    }

    // 暂停状态监测
    @Override
    public void togglePauseStatus() {
        if (audioRecordPresenter.isPaused()) {
            setAsResumeBtn();
        } else {
            setAsPauseBtn();
        }
    }

    @Override
    public void pauseRecord() {
        mAudioRecordService.pauseRecord();
        togglePauseStatus();
    }

    @Override
    public void resumeRecord() {
        mAudioRecordService.resumeRecord();
        togglePauseStatus();
    }

    @Override
    public void toggleRecordButton() {
        if (audioRecordPresenter.isRecording()) {
            mRecordButton.setImageResource(R.drawable.ic_media_stop);
            waveLineView.startAnim();
        }
        else {
            mRecordButton.setImageResource(R.drawable.ic_media_record);
            waveLineView.stopAnim();
        }
    }

    @Override
    public void setPauseButtonVisible() {
        mPauseButton.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPauseButtonInVisible() {
        mPauseButton.setVisibility(View.GONE);
    }

    @Override
    public void setScreenOnFlag() {
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void clearScreenOnFlag() {
        getActivity().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    public void startServiceAndBind() {
        Intent intent = new Intent(mContext, AudioRecordService.class);
        mContext.startService(intent);
        bindToService();
    }

    @Override
    public void bindToService() {
        Intent intent = new Intent(mContext, AudioRecordService.class);
        mContext.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        registerLocalBroadCastReceiver();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mIsServiceBound = true;
            mAudioRecordService =
                    ((AudioRecordService.ServiceBinder) iBinder).getService();
            Log.i("Tesing", " " + mAudioRecordService.isRecording() + " recording");
            audioRecordPresenter.onServiceStatusAvailable(mAudioRecordService.isRecording(),
                    mAudioRecordService.isPaused());
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
        }
    };

    @Override
    public void unbindFromService() {
        unRegisterLocalBroadCastReceiver();
        if (mIsServiceBound) {
            mIsServiceBound = false;
            mContext.unbindService(serviceConnection);
        }
    }

    @Override
    public Disposable subscribeForTimer(Consumer<AudioRecorder.RecordTime> recordTimeConsumer) {
        return mAudioRecordService.subscribeForTimer(recordTimeConsumer);
    }

    private void unRegisterLocalBroadCastReceiver() {
        LocalBroadcastManager.getInstance(mContext).unregisterReceiver(serviceUpdateReceiver);
    }

    private void registerLocalBroadCastReceiver() {
        LocalBroadcastManager.getInstance(mContext)
                .registerReceiver(serviceUpdateReceiver, new IntentFilter(AppConstants.ACTION_IN_SERVICE));
    }

    @Override
    public void stopServiceAndUnBind() {
        Intent intent = new Intent(mContext, AudioRecordService.class);
        mContext.stopService(intent);
        unbindFromService();
    }

    private final BroadcastReceiver serviceUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!intent.hasExtra(AppConstants.ACTION_IN_SERVICE)) return;
            String actionExtra = intent.getStringExtra(AppConstants.ACTION_IN_SERVICE);
            audioRecordPresenter.onServiceUpdateReceived(actionExtra);
        }
    };
}