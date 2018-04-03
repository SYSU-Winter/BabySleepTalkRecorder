package lwt.sysu.babysleeptalkrecorder.db;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

import java.util.List;
import java.util.concurrent.Callable;

public class RecordItemDataSource {
    private RecordItemDao recordItemDao;

    public RecordItemDataSource(RecordItemDao recordItemDao) {
        this.recordItemDao = recordItemDao;
    }

    // 获取数据库中所有录音文件
    public Single<List<RecordingItem>> getAllRecordings() {
        return Single.fromCallable(new Callable<List<RecordingItem>>() {
            @Override
            public List<RecordingItem> call() throws Exception {
                return recordItemDao.getAllRecordings();
            }
        }).subscribeOn(Schedulers.io());
    }

    public long insertNewRecordItem(RecordingItem recordingItem) {
        return recordItemDao.insertNewRecordItem(recordingItem);
    }

    public int deleteRecordItem(RecordingItem recordingItem) {
        return recordItemDao.deleteRecordItem(recordingItem);
    }

    public int updateRecordItem(RecordingItem recordingItem) {
        return recordItemDao.updateRecordItem(recordingItem);
    }

    public int getRecordingsCount() {
        return recordItemDao.getCount();
    }

}
