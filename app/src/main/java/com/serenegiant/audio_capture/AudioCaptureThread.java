package com.serenegiant.audio_capture;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import com.serenegiant.model.AudioMediaData;
import com.serenegiant.model.VideoCaptureFrame;
import com.serenegiant.utils.LogUtil;

import java.nio.ByteBuffer;

/**
 * Created by yong on 2019/8/31.
 */

//audio collect thread
public class AudioCaptureThread extends BaseAudioCapture {
    private final String TAG = "AudioCaptureThread";
    private static final boolean DEBUG = true;    // TODO set false on release
    private AudioRecordThread mAudioRecordThread = null;
    private boolean mIsCapturing = false;
    private AudioMediaData mAudioMediaData;

    public AudioCaptureThread(AudioMediaData audioMediaData) {
        super();
        this.mAudioMediaData = audioMediaData;
    }

    @Override
    public void startAudioCapture() {
        if (mAudioRecordThread == null) {
            mIsCapturing = true;
            mAudioRecordThread = new AudioRecordThread();
            mAudioRecordThread.setName("AudioCaptureThread");
            mAudioRecordThread.start();
        }
    }

    @Override
    public void stopAudioCapture() {
        mIsCapturing = false;
        prevOutputPTSUs = 0;
        currentPTSUs = 0;
    }

    /**
     * Thread to capture audio data from internal mic as uncompressed 16bit PCM data
     * and write them to the MediaCodec encoder
     */
    private class AudioRecordThread extends Thread {
        @Override
        public void run() {
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
            try {
                final int min_buffer_size = AudioRecord.getMinBufferSize(
                        mAudioMediaData.getAudioSampleRate(), mAudioMediaData.getAudioChannelFormat(),
                        mAudioMediaData.getAudioPcmBit());
                int buffer_size = mAudioMediaData.getAudioPerFrame() * mAudioMediaData.getAudioFrameBuffer();
                if (buffer_size < min_buffer_size)
                    buffer_size = ((min_buffer_size / mAudioMediaData.getAudioPerFrame()) + 1) * mAudioMediaData.getAudioPerFrame() * 2;

                AudioRecord audioRecord = null;
                for (final int source : AUDIO_SOURCES) {
                    try {
                        audioRecord = new AudioRecord(
                                source, mAudioMediaData.getAudioSampleRate(),
                                mAudioMediaData.getAudioChannelFormat(), mAudioMediaData.getAudioPcmBit(), buffer_size);
                        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                            audioRecord = null;
                    } catch (final Exception e) {
                        audioRecord = null;
                    }
                    if (audioRecord != null) break;
                }
                if (audioRecord != null) {
                    try {
                        if (mIsCapturing) {
                            LogUtil.d("AudioRecordThread:start audio recording");
                            final ByteBuffer buf = ByteBuffer.allocateDirect(mAudioMediaData.getAudioPerFrame());
                            int readBytes;
                            audioRecord.startRecording();
                            try {
                                for (; mIsCapturing; ) {
                                    // read audio data from internal mic
                                    buf.clear();
                                    readBytes = audioRecord.read(buf, mAudioMediaData.getAudioPerFrame());
                                    if (readBytes > 0) {
                                        // set audio data to encoder
                                        buf.position(readBytes);
                                        buf.flip();
                                        currentPTSUs = getPTSUs();
                                        mCaptureDataConnector.onDataAvailable(new VideoCaptureFrame(buf, readBytes, currentPTSUs));
                                        prevOutputPTSUs = currentPTSUs;
                                    }
                                }
                                //TODO deal with this
                                mCaptureDataConnector.onDataAvailable(null);
                            } finally {
                                audioRecord.stop();
                            }
                        }
                    } finally {
                        audioRecord.release();
                    }
                } else {
                    LogUtil.e("failed to initialize AudioRecord");
                }
            } catch (final Exception e) {
                LogUtil.e("AudioRecordThread#run" + e);
            }
            LogUtil.v("AudioRecordThread:finished");
        }
    }

    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.MIC,
            MediaRecorder.AudioSource.DEFAULT,
            MediaRecorder.AudioSource.CAMCORDER,
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };

    private long prevOutputPTSUs = 0;
    private long currentPTSUs = 0;

    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }
}
