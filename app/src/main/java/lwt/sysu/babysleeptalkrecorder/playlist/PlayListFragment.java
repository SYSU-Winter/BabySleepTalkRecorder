package lwt.sysu.babysleeptalkrecorder.playlist;

import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.db.RecordingItem;
import lwt.sysu.babysleeptalkrecorder.mvpbase.BaseFragment;
import lwt.sysu.babysleeptalkrecorder.recordingservice.AudioRecorder;
import lwt.sysu.babysleeptalkrecorder.recordingservice.Constants;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.inject.Inject;

public class PlayListFragment extends BaseFragment implements PlayListMVPView {
    private static final String TAG = "PlayListFragment";

    @Inject
    public PlayListAdapter mPlayListAdapter;

    @Inject
    public PlayListPresenter<PlayListMVPView> playListPresenter;

    private RecyclerView mRecordingsListView;
    private TextView emptyListLabel;
    private MediaPlayer mMediaPlayer;

    public static PlayListFragment newInstance() {
        return new PlayListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        playListPresenter.onAttach(this);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 加载布局
        View v = inflater.inflate(R.layout.fragment_file_viewer, container, false);
        initViews(v);
        mMediaPlayer = new MediaPlayer();
        return v;
    }

    private void initViews(View v) {
        emptyListLabel = v.findViewById(R.id.empty_list_label);
        mRecordingsListView = v.findViewById(R.id.recyclerView);
        mRecordingsListView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getActivity());
        llm.setOrientation(LinearLayoutManager.VERTICAL);

        // 倒序显示，就是最新的录音在最上面
        llm.setReverseLayout(true);
        llm.setStackFromEnd(true);

        mRecordingsListView.setLayoutManager(llm);
        mRecordingsListView.setAdapter(mPlayListAdapter);
        playListPresenter.onViewInitialised();
    }


    @Override
    public void onDestroy() {
        playListPresenter.onDetach();
        super.onDestroy();
    }

    @Override
    public void notifyListAdapter() {
        mPlayListAdapter.notifyDataSetChanged();
    }

    @Override
    public void setRecordingListVisible() {
        mRecordingsListView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setRecordingListInVisible() {
        mRecordingsListView.setVisibility(View.GONE);
    }

    @Override
    public void setEmptyLabelVisible() {
        emptyListLabel.setVisibility(View.VISIBLE);
    }

    @Override
    public void setEmptyLabelInVisible() {
        emptyListLabel.setVisibility(View.GONE);
    }

    private int positionOfCurrentViewHolder = -1;
    private PlayListAdapter.RecordingsViewHolder recordingsViewHolder;

    Handler uiThreadHandler = new Handler();

    @Override
    public void updateProgressInListItem(final Integer position) {
        if (position != positionOfCurrentViewHolder || recordingsViewHolder == null) {
            positionOfCurrentViewHolder = position;
            recordingsViewHolder =
                    (PlayListAdapter.RecordingsViewHolder)
                            mRecordingsListView.findViewHolderForAdapterPosition(position);
        }
        if (recordingsViewHolder != null && recordingsViewHolder.getAdapterPosition() == position) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    // 更新播放项的进度条
                    recordingsViewHolder.updateProgressInSeekBar(position);
                }
            });
        } else {
            positionOfCurrentViewHolder = -1;
            recordingsViewHolder = null;
        }
    }

    @Override
    public void updateTimerInListItem(final int position) {
        // 更新播放项的计时器
        if (recordingsViewHolder != null) {
            uiThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    recordingsViewHolder.updatePlayTimer(position);
                }
            });
        }
    }

    @Override
    public void notifyListItemChange(Integer position) {
        mPlayListAdapter.notifyItemChanged(position);
    }

    @Override
    public void showError(String message) {
        Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void notifyListItemRemove(Integer position) {
        mPlayListAdapter.notifyItemRemoved(position);
    }

    @Override
    public void showFileOptionDialog(final int position, RecordingItem recordingItem) {
        // 这里是长按会显示的3个选项
        // 分别是分享、重命名和删除
        ArrayList<String> fileOptions = new ArrayList<>();
        fileOptions.add(getString(R.string.dialog_file_share));
        fileOptions.add(getString(R.string.dialog_file_rename));
        fileOptions.add(getString(R.string.dialog_file_delete));

        final CharSequence[] items = fileOptions.toArray(new CharSequence[fileOptions.size()]);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_title_options));
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int listItem) {
                switch (listItem) {
                    case 0:
                        playListPresenter.shareFileClicked(position);
                        break;
                    case 1:
                        playListPresenter.renameFileClicked(position);
                        break;
                    case 2:
                        playListPresenter.deleteFileClicked(position);
                        break;
                }
            }
        });
        builder.setCancelable(true);
        builder.setNegativeButton(getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        AlertDialog alert = builder.create();
        alert.show();
    }

    @Override
    public void shareFileDialog(String filePath) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM,
                Uri.fromFile(new File(filePath)));
        shareIntent.setType("audio/mp4");
        getActivity().startActivity(Intent.createChooser(shareIntent, getText(R.string.send_to)));
    }

    @Override
    public void showRenameFileDialog(final int position) {
        AlertDialog.Builder renameFileBuilder = new AlertDialog.Builder(getActivity());
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_rename_file, null);
        final EditText input = view.findViewById(R.id.new_name);
        renameFileBuilder.setTitle(getString(R.string.dialog_title_rename));
        renameFileBuilder.setCancelable(true);
        renameFileBuilder.setPositiveButton(getString(R.string.dialog_action_ok),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String value = input.getText().toString().trim()
                                + Constants.AUDIO_RECORDER_FILE_EXT_WAV;
                        playListPresenter.renameFile(position, value);
                        dialog.cancel();
                    }
                });
        renameFileBuilder.setNegativeButton(getActivity().getString(R.string.dialog_action_cancel),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        renameFileBuilder.setView(view);
        AlertDialog alert = renameFileBuilder.create();
        alert.show();
    }

    @Override
    public void showDeleteFileDialog(final int position) {
        AlertDialog.Builder confirmDelete = new AlertDialog.Builder(getActivity());
        confirmDelete.setTitle(getString(R.string.dialog_title_delete));
        confirmDelete.setMessage(getString(R.string.dialog_text_delete));
        confirmDelete.setCancelable(true);
        confirmDelete.setPositiveButton(getString(R.string.dialog_action_yes),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        playListPresenter.deleteFile(position);
                        dialog.cancel();
                    }
                });
        confirmDelete.setNegativeButton(getString(R.string.dialog_action_no),
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
        AlertDialog alert = confirmDelete.create();
        alert.show();
    }

    @Override
    public void pauseMediaPlayer(int position) {
        mMediaPlayer.pause();
    }

    @Override
    public void resumeMediaPlayer(int position) {
        mMediaPlayer.start();
    }

    @Override
    public void stopMediaPlayer(int currentPlayingItem) {
        if (mMediaPlayer != null) {
            Log.i("Debug ", "Stopping");
            mMediaPlayer.stop();
            mMediaPlayer.reset();
            mMediaPlayer.release();
            mMediaPlayer = null;
        }
    }

    @Override
    public void startMediaPlayer(int position, RecordingItem recordingItem)
            throws IOException {
        mMediaPlayer = new MediaPlayer();
        mMediaPlayer.setDataSource(recordingItem.getFilePath());
        mMediaPlayer.prepare();
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                playListPresenter.mediaPlayerStopped();
            }
        });
        Log.i("Debug ", "Started");
    }
}
