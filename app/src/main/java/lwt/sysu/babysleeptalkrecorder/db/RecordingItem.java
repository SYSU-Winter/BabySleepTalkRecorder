package lwt.sysu.babysleeptalkrecorder.db;

import android.arch.persistence.room.Entity;
import android.arch.persistence.room.Ignore;
import android.arch.persistence.room.PrimaryKey;
import android.os.Parcel;
import android.os.Parcelable;

@Entity(tableName = "recordings")
public class RecordingItem implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    private int id;
    private String mName; // 文件名
    private String mFilePath; // 文件路径
    private long mLength; // 录音文件长度，以秒计算
    private long mTime; // 录音的时间（日期）

    @Ignore
    public boolean isPlaying = false;

    @Ignore
    public boolean isPaused;
    @Ignore
    public long playProgress;

    public RecordingItem() {
    }

    private RecordingItem(Parcel in) {
        mName = in.readString();
        mFilePath = in.readString();
        mLength = in.readLong();
        mTime = in.readLong();
    }

    public String getFilePath() {
        return mFilePath;
    }

    public void setFilePath(String filePath) {
        mFilePath = filePath;
    }

    public long getLength() {
        return mLength;
    }

    public void setLength(long length) {
        mLength = length;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public long getTime() {
        return mTime;
    }

    public void setTime(long time) {
        mTime = time;
    }

    public static final Parcelable.Creator<RecordingItem> CREATOR =
            new Parcelable.Creator<RecordingItem>() {
                public RecordingItem createFromParcel(Parcel in) {
                    return new RecordingItem(in);
                }

                public RecordingItem[] newArray(int size) {
                    return new RecordingItem[size];
                }
            };

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mLength);
        dest.writeLong(mTime);
        dest.writeString(mFilePath);
        dest.writeString(mName);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}