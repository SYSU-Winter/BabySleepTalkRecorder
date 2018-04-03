package lwt.sysu.babysleeptalkrecorder.sleeptalkrecording;

import lwt.sysu.babysleeptalkrecorder.mvpbase.IMVPView;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecorder;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

public interface AudioRecordMVPView extends IMVPView {
  void updateChronometer(String text);

  void togglePauseStatus();

  void toggleRecordButton();

  void setPauseButtonVisible();

  void setPauseButtonInVisible();

  void setScreenOnFlag();

  void clearScreenOnFlag();

  void startServiceAndBind();

  void stopServiceAndUnBind();

  void bindToService();

  void unbindFromService();

  void pauseRecord();

  void resumeRecord();

  Disposable subscribeForTimer(Consumer<AudioRecorder.RecordTime> recordTimeConsumer);
}
