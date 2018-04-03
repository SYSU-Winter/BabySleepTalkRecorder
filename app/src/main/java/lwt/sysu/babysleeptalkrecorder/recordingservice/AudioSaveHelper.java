package lwt.sysu.babysleeptalkrecorder.recordingservice;

import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.os.Environment;
import android.util.Log;

import lwt.sysu.babysleeptalkrecorder.db.RecordItemDataSource;
import lwt.sysu.babysleeptalkrecorder.db.RecordingItem;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.inject.Inject;

class AudioSaveHelper {

    private final RecordItemDataSource recordItemDataSource;
    private FileOutputStream os;
    private File mFile;
    private int mRecordSampleRate;

    private static final String TAG = "AudioHelper";

    @Inject
    public AudioSaveHelper(RecordItemDataSource recordItemDataSource) {
        this.recordItemDataSource = recordItemDataSource;
    }

    // 创建文件
    public void createNewFile() {
        Log.i("Testing", "creating file");
        // 获取存储路径
        String storeLocation = Environment.getExternalStorageDirectory().getAbsolutePath();
        // 命名一个存储文件夹
        File folder = new File(storeLocation + "/SoundRecorder");
        // 如果文件夹不存在那么新建一个
        if (!folder.exists()) {
            folder.mkdir();
        }
        int count = 0;
        // 存储的录音文件命名，尾部依次加一
        String fileName;
        // 如果文件名已存在，那么通过一个循环来增加文件名后面的序号
        do {
            count++;
            fileName = "AudioRecord_"
                    + (recordItemDataSource.getRecordingsCount() + count)
                    + Constants.AUDIO_RECORDER_FILE_EXT_WAV;

            Log.d(TAG, "createNewFile: " + recordItemDataSource.getRecordingsCount());

            // 文件的完整路径
            String mFilePath = storeLocation + "/SoundRecorder/" + fileName;
            Log.d(TAG, "文件路径: " + mFilePath);
            // 新建这个文件
            mFile = new File(mFilePath);
        } while (mFile.exists() && !mFile.isDirectory());

        try {
            os = new FileOutputStream(mFile);
            writeWavHeader(os, Constants.RECORDER_CHANNELS, mRecordSampleRate,
                    Constants.RECORDER_AUDIO_ENCODING);
        } catch (IOException e) {
            // TODO: SIX_SIX_SIX
            e.printStackTrace();
        }
    }

    public void onDataReady(byte[] data) {
        try {
            os.write(data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 停止录音
    public void onRecordingStopped(AudioRecorder.RecordTime currentRecordTime) {
        try {
            os.close();
            updateWavHeader(mFile);
            saveFileDetails(currentRecordTime);
            System.out.println("Record Complete. Saving and closing");
        } catch (IOException e) {
            mFile.deleteOnExit();
            e.printStackTrace();
        }
    }

    // 录下来的文件的一些详细信息
    // 包括名字的设置，文件路径，时长，录制的结束时间
    private void saveFileDetails(AudioRecorder.RecordTime currentRecordTime) {
        RecordingItem recordingItem = new RecordingItem();
        recordingItem.setName(mFile.getName());
        recordingItem.setFilePath(mFile.getPath());

        // 这里获取实际保存的录音文件的长度，也就是有声音的部分
        // 如果是获取到完整的录音时长，也就是开始到结束的时间则是如下：
        // recordingItem.setLength(mediaPlayer.getDuration());
        MediaPlayer mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(mFile.getPath());
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }
        recordingItem.setLength(mediaPlayer.getDuration());
        mediaPlayer.release();

        recordingItem.setTime(System.currentTimeMillis());
        recordItemDataSource.insertNewRecordItem(recordingItem);
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out         The stream to write the header to
     * @param channelMask An AudioFormat.CHANNEL_* mask
     * @param sampleRate  The sample rate in hertz
     * @param encoding    An AudioFormat.ENCODING_PCM_* value
     * @throws IOException
     */
    private void writeWavHeader(OutputStream out, int channelMask, int sampleRate, int encoding)
            throws IOException {
        short channels;
        switch (channelMask) {
            case AudioFormat.CHANNEL_IN_MONO: // 单声道
                channels = 1;
                break;
            case AudioFormat.CHANNEL_IN_STEREO: // 双声道
                channels = 2;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable channel mask");
        }

        short bitDepth;
        switch (encoding) {
            case AudioFormat.ENCODING_PCM_8BIT:
                bitDepth = 8;
                break;
            case AudioFormat.ENCODING_PCM_16BIT:
                bitDepth = 16;
                break;
            case AudioFormat.ENCODING_PCM_FLOAT:
                bitDepth = 32;
                break;
            default:
                throw new IllegalArgumentException("Unacceptable encoding");
        }

        writeWavHeader(out, channels, sampleRate, bitDepth);
    }

    /**
     * Writes the proper 44-byte RIFF/WAVE header to/for the given stream
     * Two size fields are left empty/null since we do not yet know the final stream size
     *
     * @param out        The stream to write the header to
     * @param channels   The number of channels
     * @param sampleRate The sample rate in hertz
     * @param bitDepth   The bit depth
     * @throws IOException
     */

    // 上面搞了一大堆然后音质选择并没有实现的很多好吧，其实就是单声道，16bits, 采样率可选8000和44100
    private void writeWavHeader(OutputStream out, short channels, int sampleRate, short bitDepth)
            throws IOException {
        // Convert the multi-byte integers to raw bytes in little endian format as required by the spec
        byte[] littleBytes = ByteBuffer.allocate(14)
                .order(ByteOrder.LITTLE_ENDIAN)
                .putShort(channels)
                .putInt(sampleRate)
                .putInt(sampleRate * channels * (bitDepth / 8))
                .putShort((short) (channels * (bitDepth / 8)))
                .putShort(bitDepth)
                .array();

        // Not necessarily the best, but it's very easy to visualize this way
        out.write(new byte[]{
                // RIFF header
                'R', 'I', 'F', 'F', // ChunkID
                0, 0, 0, 0, // ChunkSize (must be updated later)
                'W', 'A', 'V', 'E', // Format
                // fmt subchunk
                'f', 'm', 't', ' ', // Subchunk1ID
                16, 0, 0, 0, // Subchunk1Size
                1, 0, // AudioFormat
                littleBytes[0], littleBytes[1], // NumChannels
                littleBytes[2], littleBytes[3], littleBytes[4], littleBytes[5], // SampleRate
                littleBytes[6], littleBytes[7], littleBytes[8], littleBytes[9], // ByteRate
                littleBytes[10], littleBytes[11], // BlockAlign
                littleBytes[12], littleBytes[13], // BitsPerSample
                // data subchunk
                'd', 'a', 't', 'a', // Subchunk2ID
                0, 0, 0, 0, // Subchunk2Size (must be updated later)
        });
    }

    /**
     * Updates the given wav file's header to include the final chunk sizes
     *
     * @param wav The wav file to update
     * @throws IOException
     */
    private void updateWavHeader(File wav) throws IOException {
        byte[] sizes = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
                .putInt((int) (wav.length() - 8)) // ChunkSize
                .putInt((int) (wav.length() - 44)) // Subchunk2Size
                .array();

        RandomAccessFile accessWave = null;
        //noinspection CaughtExceptionImmediatelyRethrown
        try {
            accessWave = new RandomAccessFile(wav, "rw");
            // ChunkSize
            accessWave.seek(4);
            accessWave.write(sizes, 0, 4);

            // Subchunk2Size
            accessWave.seek(40);
            accessWave.write(sizes, 4, 4);
        } catch (IOException ex) {
            // Rethrow but we still close accessWave in our finally
            throw ex;
        } finally {
            if (accessWave != null) {
                try {
                    accessWave.close();
                } catch (IOException ex) {
                    //
                }
            }
        }
    }

    // 好吧，可选的音质只有采样率的高低可选
    public void setSampleRate(int sampleRate) {
        this.mRecordSampleRate = sampleRate;
    }
}
