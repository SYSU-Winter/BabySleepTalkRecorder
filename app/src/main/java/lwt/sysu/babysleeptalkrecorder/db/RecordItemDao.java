package lwt.sysu.babysleeptalkrecorder.db;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

@Dao
public interface RecordItemDao {
    @Query("Select * from recordings")
    List<RecordingItem> getAllRecordings();

    @Insert
    long insertNewRecordItem(RecordingItem recordingItem);

    @Update
    int updateRecordItem(RecordingItem recordingItem);

    @Delete
    int deleteRecordItem(RecordingItem recordingItem);

    // 返回数据库中文件的个数
    @Query("Select count() from recordings")
    int getCount();
}
