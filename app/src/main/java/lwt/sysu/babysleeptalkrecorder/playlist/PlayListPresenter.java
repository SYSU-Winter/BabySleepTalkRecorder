package lwt.sysu.babysleeptalkrecorder.playlist;

import lwt.sysu.babysleeptalkrecorder.db.RecordingItem;
import lwt.sysu.babysleeptalkrecorder.mvpbase.IMVPPresenter;

public interface PlayListPresenter<V extends PlayListMVPView> extends IMVPPresenter<V> {
    // 初始化工作
    void onViewInitialised();

    // 对保存的录音文件重命名
    void renameFile(int position, String value);

    // 删除文件
    void deleteFile(int position);

    // 获取列表序号
    RecordingItem getListItemAt(int position);

    // 列表子项的点击事件
    void onListItemClick(int position);

    // 列表子项的长按事件
    void onListItemLongClick(int position);

    // 获取列表中子项的数目
    int getListItemCount();

    // 这里是长按之后会弹出的3个选项
    // 分享按钮点击事件
    void shareFileClicked(int position);

    // 重命名按钮点击事件
    void renameFileClicked(int position);

    // 删除文件按钮点击事件
    void deleteFileClicked(int position);

    // TODO
    void mediaPlayerStopped();
}
