package lwt.sysu.babysleeptalkrecorder.playlist;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import lwt.sysu.babysleeptalkrecorder.R;
import lwt.sysu.babysleeptalkrecorder.db.RecordingItem;
import lwt.sysu.babysleeptalkrecorder.di.qualifiers.ActivityContext;
import lwt.sysu.babysleeptalkrecorder.libs.FillSeekBar;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

public class PlayListAdapter extends RecyclerView.Adapter<PlayListAdapter.RecordingsViewHolder> {

    private static final String LOG_TAG = "PlayListAdapter";

    private final LayoutInflater inflater;

    private final Context mContext;
    private final PlayListPresenter<PlayListMVPView> playListPresenter;

    @Inject
    public PlayListAdapter(@ActivityContext Context context,
                           PlayListPresenter<PlayListMVPView> playListPresenter) {
        mContext = context;
        this.playListPresenter = playListPresenter;
        inflater = LayoutInflater.from(mContext);
    }

    @Override
    public void onBindViewHolder(final RecordingsViewHolder holder, int position) {

        RecordingItem currentRecording = playListPresenter.getListItemAt(position);
        long itemDuration = currentRecording.getLength();
        long minutes = TimeUnit.MILLISECONDS.toMinutes(itemDuration);
        // 计算应该显示的秒数，总秒数减去分钟的秒数
        long seconds =
                TimeUnit.MILLISECONDS.toSeconds(itemDuration) - TimeUnit.MINUTES.toSeconds(minutes);
        holder.fillSeekBar.setMaxVal(itemDuration);
        holder.vName.setText(currentRecording.getName());
        holder.vLength.setText(
                String.format(mContext.getString(R.string.play_time_format), minutes, seconds));
        holder.vDateAdded.setText(DateUtils.formatDateTime(mContext, currentRecording.getTime(),
                DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE
                        | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));
        holder.fillSeekBar.setProgress(currentRecording.playProgress);
        holder.playStateImage.setImageResource(
                currentRecording.isPlaying ? R.drawable.ic_pause_grey : R.drawable.ic_play_arrow_grey);
    }

    @Override
    public RecordingsViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new RecordingsViewHolder(inflater.
                inflate(R.layout.record_list_item, parent, false), playListPresenter);
    }

    static class RecordingsViewHolder extends RecyclerView.ViewHolder {
        final ImageView playStateImage;
        final TextView vName;
        final TextView playProgress;
        final TextView vLength;
        final TextView vDateAdded;
        final View cardView;
        final FillSeekBar fillSeekBar;
        private final PlayListPresenter playListPresenter;

        RecordingsViewHolder(View v, PlayListPresenter playListPresenter) {
            super(v);
            this.playListPresenter = playListPresenter;
            playStateImage = v.findViewById(R.id.record_list_image);
            vName = v.findViewById(R.id.file_name_text);
            playProgress = v.findViewById(R.id.play_progress_text);
            vLength = v.findViewById(R.id.file_length_text);
            vDateAdded = v.findViewById(R.id.file_date_added_text);
            cardView = v.findViewById(R.id.record_item_root_view);
            fillSeekBar = v.findViewById(R.id.attached_seek_bar);
            bindEvents();
        }

        private void bindEvents() {
            cardView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    playListPresenter.onListItemClick(getAdapterPosition());
                }
            });

            cardView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    playListPresenter.onListItemLongClick(getAdapterPosition());
                    return false;
                }
            });
        }

        public void updateProgressInSeekBar(Integer position) {
            RecordingItem currentItem = playListPresenter.getListItemAt(position);
            fillSeekBar.setProgress(currentItem.playProgress);
        }

        // 更新播放列表右上角的正在播放时间
        public void updatePlayTimer(int position) {
            // 定位到列表中正在播放的项目
            RecordingItem currentItem = playListPresenter.getListItemAt(position);
            if (currentItem.playProgress > 0) {
                playProgress.setVisibility(View.VISIBLE);
                long minutes = TimeUnit.MILLISECONDS.toMinutes(currentItem.playProgress);
                long seconds =
                        TimeUnit.MILLISECONDS.toSeconds(currentItem.playProgress) - TimeUnit.MINUTES.toSeconds(
                                minutes);
                playProgress.setText(String.format("%02d:%02d", minutes, seconds));
            } else {
                playProgress.setVisibility(View.GONE);
            }
        }
    }

    @Override
    public int getItemCount() {
        return playListPresenter.getListItemCount();
    }
}
