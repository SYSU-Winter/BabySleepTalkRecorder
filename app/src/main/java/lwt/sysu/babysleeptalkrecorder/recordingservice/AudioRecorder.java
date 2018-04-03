package lwt.sysu.babysleeptalkrecorder.recordingservice;

import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.util.Log;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.processors.BehaviorProcessor;
import io.reactivex.processors.PublishProcessor;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;

public class AudioRecorder implements IAudioRecorder {

    // 录音机状态
    private static final int RECORDER_STATE_FAILURE = -1;
    private static final int RECORDER_STATE_IDLE = 0;
    private static final int RECORDER_STATE_STARTING = 1;
    private static final int RECORDER_STATE_STOPPING = 2;
    private static final int RECORDER_STATE_BUSY = 3;

    private volatile int recorderState;
    // 采样率
    private int mRecorderSampleRate = 8000;

    private AudioSaveHelper audioSaveHelper = null;

    private final Object recorderStateMonitor = new Object();

    private byte[] recordBuffer;

    private final CompositeDisposable compositeDisposable = new CompositeDisposable();

    // 以原子方式更新的Long值和Boolean值，避免在多线程下出现数据不一致
    private final AtomicLong mRecordTimeCounter = new AtomicLong(0);
    private final AtomicBoolean mIsPaused = new AtomicBoolean(false);
    private RecordTime currentRecordTime;

    private static final String TAG = "AudioRecorder";

    @Inject
    public AudioRecorder(AudioSaveHelper audioSaveHelper) {
        this.audioSaveHelper = audioSaveHelper;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void onRecordFailure() { // 录音失败结束录音
        recorderState = RECORDER_STATE_FAILURE;
        finishRecord();
    }

    @Override
    public void startRecord(int recorderSampleRate) {
        // 开始录音前录音机状态应处于空闲状态
        if (recorderState != RECORDER_STATE_IDLE) {
            return;
        }
        this.mRecorderSampleRate = recorderSampleRate;
        audioSaveHelper.setSampleRate(mRecorderSampleRate);
        startTimer(); // 启动计时器
        recorderState = RECORDER_STATE_STARTING; // 更新录音状态为正在启动
        startRecordThread(); // 启动录音线程
    }

    private final BehaviorProcessor<RecordTime> recordTimeProcessor = BehaviorProcessor.create();

    private void startTimer() {
        getTimerObservable().subscribeOn(Schedulers.newThread()).subscribe(recordTimeProcessor);
    }

    private Flowable<RecordTime> getTimerObservable() {
        return Flowable.interval(1000, TimeUnit.MILLISECONDS)
                // 出于暂停状态的声音跳过不计时
                .filter(new Predicate<Long>() {
                    @Override
                    public boolean test(Long aLong) throws Exception {
                        return !mIsPaused.get();
                    }
                })
                .map(new Function<Long, RecordTime>() {
                    @Override
                    public RecordTime apply(Long aLong) throws Exception {
                        long seconds = mRecordTimeCounter.incrementAndGet(); // 以原子方式将当前值加一
                        RecordTime recordTime = new RecordTime();
                        //recordTime.millis = seconds * 1000; // 总时间，毫秒算
                        //Log.d(TAG, "apply: 6666666666: " + recordTime.millis);
                        recordTime.hours = seconds / (60 * 60); // 小时
                        seconds = seconds % (60 * 60); // 秒数，对3600取模，也就是分钟数的秒数
                        recordTime.minutes = seconds / 60; // 分钟数
                        seconds = seconds % 60; // 显示的秒数
                        recordTime.seconds = seconds;
                        currentRecordTime = recordTime;
                        return recordTime;
                    }
                });
    }

    private final Flowable<byte[]> audioDataFlowable = Flowable.create(new FlowableOnSubscribe<byte[]>() {
        @Override
        public void subscribe(FlowableEmitter<byte[]> emitter) throws Exception {
            int bufferSize = 4 * 1024;

            AudioRecord recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, mRecorderSampleRate,
                    Constants.RECORDER_CHANNELS, Constants.RECORDER_AUDIO_ENCODING, bufferSize);
            audioSaveHelper.createNewFile();

            try {
                if (recorderState == RECORDER_STATE_STARTING) {
                    // 更新状态为忙，也就是正在录音咯
                    recorderState = RECORDER_STATE_BUSY;
                }
                // 开始录音
                recorder.startRecording();

                recordBuffer = new byte[bufferSize];
                do {
                    if (!mIsPaused.get()) {
                        int bytesRead = recorder.read(recordBuffer, 0, bufferSize);

                        long v = 0;
                        // 将 buffer 内容取出，进行平方和运算
                        for (byte aRecordBuffer : recordBuffer) {
                            v += aRecordBuffer * aRecordBuffer;
                        }
                        // 平方和除以数据总长度，得到音量大小。
                        double mean = v / (double) bytesRead;
                        mean = mean / 500;
                        double volume = 10 * Math.log10(mean);

                        Log.d(TAG, "分贝: " + volume);

                        if (volume > -2)
                            emitter.onNext(recordBuffer);

                        if (bytesRead == 0) {
                            Log.e(AudioRecorder.class.getSimpleName(), "error: " + bytesRead);
                            onRecordFailure();
                        }
                    }
                } while (recorderState == RECORDER_STATE_BUSY);
            } finally {
                recorder.release();
            }
            emitter.onComplete();
        }
    }, BackpressureStrategy.DROP); // 策略是直接扔掉存不下的事件


    private final PublishProcessor<byte[]> recordDataPublishProcessor = PublishProcessor.create();

    // 启动录音线程
    private void startRecordThread() {
        // 工作在io线程
        audioDataFlowable.subscribeOn(Schedulers.io()).subscribe(recordDataPublishProcessor);
        compositeDisposable.add(recordDataPublishProcessor.onBackpressureBuffer()
                .observeOn(Schedulers.io())
                .subscribeWith(new DisposableSubscriber<byte[]>() {
                    @Override
                    public void onNext(byte[] bytes) {
                        audioSaveHelper.onDataReady(recordBuffer);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {
                        audioSaveHelper.onRecordingStopped(currentRecordTime);
                        synchronized (recorderStateMonitor) {
                            // 置于空闲状态
                            recorderState = RECORDER_STATE_IDLE;
                            // 解除所有那些在此对象上调用wait方法的线程的阻塞状态
                            recorderStateMonitor.notifyAll();
                        }
                    }
                }));
    }

    @Override
    public void finishRecord() {
        int recorderStateLocal = recorderState;
        // 如果当前的录音状态不是出于空闲，那么就是需要结束了
        if (recorderStateLocal != RECORDER_STATE_IDLE) {
            synchronized (recorderStateMonitor) {
                recorderStateLocal = recorderState;
                if (recorderStateLocal == RECORDER_STATE_STARTING
                        || recorderStateLocal == RECORDER_STATE_BUSY) {

                    recorderStateLocal = recorderState = RECORDER_STATE_STOPPING;
                }

                do {
                    try {
                        if (recorderStateLocal != RECORDER_STATE_IDLE) {
                            // 使线程进入等待状态
                            recorderStateMonitor.wait();
                        }
                    } catch (InterruptedException ignore) {
                        /* Nothing to do */
                    }
                    recorderStateLocal = recorderState;
                } while (recorderStateLocal == RECORDER_STATE_STOPPING);
            }
        }
        compositeDisposable.dispose();
    }

    // 暂停录音
    @Override
    public void pauseRecord() {
        mIsPaused.set(true);
    }

    // 恢复录音
    @Override
    public void resumeRecord() {
        mIsPaused.set(false);
    }

    @Override
    public boolean isRecording() {
        return recorderState != RECORDER_STATE_IDLE;
    }

    public Flowable<byte[]> getAudioDataFlowable() {
        return recordDataPublishProcessor;
    }

    public Disposable subscribeTimer(Consumer<RecordTime> timerConsumer) {
        Disposable disposable =
                recordTimeProcessor.observeOn(AndroidSchedulers.mainThread()).subscribe(timerConsumer);
        compositeDisposable.add(disposable);
        return disposable;
    }

    // 获取暂停状态
    boolean isPaused() {
        return mIsPaused.get();
    }

    // 内部静态类，成员变量用于记录当前录音时间
    public static class RecordTime {
        public long seconds = 0;
        public long minutes = 0;
        public long hours = 0;
        public long millis = 0;
    }
}
