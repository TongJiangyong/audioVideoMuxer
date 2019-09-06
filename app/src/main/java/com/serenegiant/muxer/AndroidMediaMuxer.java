package com.serenegiant.muxer;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: AndroidMediaMuxer.java
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 * All files in the folder are under this Apache License, Version 2.0.
*/

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.media.MediaMuxer;


import com.serenegiant.encoder.BaseEncoder;
import com.serenegiant.encoder.MediaCodecAudioEncoder;
import com.serenegiant.encoder.MediaCodecEncoder;
import com.serenegiant.encoder.MediaCodecVideoEncoder;
import com.serenegiant.model.AudioMediaData;
import com.serenegiant.model.MediaEncoderFormat;
import com.serenegiant.model.EncodedFrame;
import com.serenegiant.model.VideoMediaData;
import com.serenegiant.utils.LogUtil;

public class AndroidMediaMuxer extends BaseMuxer {
    private static final boolean DEBUG = false;    // TODO set false on release
    private static final String TAG = "AndroidMediaMuxer";

    private static final String DIR_NAME = "AVRecSample";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);

    private String mOutputPath;
    private final MediaMuxer mMediaMuxer;    // API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    private MediaCodecEncoder mVideoEncoder, mAudioEncoder;

    /**
     * Constructor
     *
     * @param path extension of output file
     * @throws IOException
     */
    public AndroidMediaMuxer(String path, VideoMediaData videoMediaData, AudioMediaData audioMediaData) throws IOException {
        super(videoMediaData, audioMediaData);
        try {
            mOutputPath = path;
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        mMediaMuxer = new MediaMuxer(mOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
    }

    @Override
    public void prepareEncoders() throws IOException {
        if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
    }


    @Override
    public void startEncoders() {
        if (mVideoEncoder != null)
            mVideoEncoder.startRecording();
        if (mAudioEncoder != null)
            mAudioEncoder.startRecording();
    }


    @Override
    public void stopEncoders() {
        if (mVideoEncoder != null)
            mVideoEncoder.stopRecording();
        mVideoEncoder = null;
        if (mAudioEncoder != null)
            mAudioEncoder.stopRecording();
        mAudioEncoder = null;
    }

    @Override
    public synchronized boolean isMuxerStarted() {
        return mIsStarted;
    }

//**********************************************************************
//**********************************************************************

    /**
     * assign encoder to this calss. this is called from encoder.
     *
     * @param encoder instance of MediaCodecVideoEncoder or MediaCodecAudioEncoder
     */
    @Override
    public void addEncoder(final BaseEncoder encoder) {
        if (encoder instanceof MediaCodecVideoEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = (MediaCodecEncoder) encoder;
        } else if (encoder instanceof MediaCodecAudioEncoder) {
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = (MediaCodecEncoder) encoder;
        } else
            throw new IllegalArgumentException("unsupported encoder");
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     *
     * @return true when muxer is ready to write
     */
    @Override
    public synchronized boolean startMuxer() {
        LogUtil.v("start:");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mMediaMuxer.start();
            mIsStarted = true;
            notifyAll();
            LogUtil.v("MediaMuxer started");
        }
        return mIsStarted;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    @Override
    public synchronized void stopMuxer() {
        LogUtil.v("stop:mStatredCount=" + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mMediaMuxer.stop();
            mMediaMuxer.release();
            mIsStarted = false;
            LogUtil.v("MediaMuxer stopped");
        }
    }

    /**
     * assign encoder to muxer
     *
     * @param format
     * @return minus value indicate error
     */
    @Override
    public synchronized int addTrackToMuxer(MediaEncoderFormat format) {
        if (mIsStarted)
            throw new IllegalStateException("muxer already started");
        final int trackIx = mMediaMuxer.addTrack(format.getMediaFormat());
        LogUtil.i("addTrack:" + mEncoderCount + ",trackIx=" + trackIx + ",format=" + format.getMediaFormat());
        return trackIx;
    }

    /**
     * @param encodedFrame
     */
    @Override
    protected synchronized void writeEncodedData(EncodedFrame encodedFrame) {
        if (mStatredCount > 0) {
            LogUtil.i("addTrack:" + encodedFrame.getmTrackIndex() + ",BufferInfo=" + encodedFrame.getmBufferInfo());
            mMediaMuxer.writeSampleData(encodedFrame.getmTrackIndex(), encodedFrame.getEncodedByteBuffer(), encodedFrame.getmBufferInfo());
        }


    }

    @Override
    public int onDataAvailable(EncodedFrame data) {
        this.writeEncodedData(data);
        return 0;
    }

}
