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
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Locale;

import android.media.MediaCodec;
import android.os.Handler;

import com.serenegiant.encoder.BaseEncoder;
import com.serenegiant.encoder.MediaCodecAudioEncoder;
import com.serenegiant.encoder.MediaCodecEncoder;
import com.serenegiant.encoder.MediaCodecVideoEncoder;
import com.serenegiant.model.AudioMediaData;
import com.serenegiant.model.MediaEncoderFormat;
import com.serenegiant.model.BufferInfoEx;
import com.serenegiant.model.EncodedFrame;
import com.serenegiant.model.VideoMediaData;
import com.serenegiant.utils.LogUtil;
import com.serenegiant.utils.OtherUtil;

import io.agora.RTMPMuxer;


public class RtmpMuxer extends BaseMuxer {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "RtmpMuxer";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
    private StreamPublishParam mStreamPublishParam;
    private String mOutputPath;
    private final RTMPMuxer mRtmpMuxer;    // API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    public boolean mIsConnected;
    private MediaCodecEncoder mVideoEncoder, mAudioEncoder;
    private FrameSender frameSender;
    protected TimeIndexCounter videoTimeIndexCounter = new TimeIndexCounter();
    protected TimeIndexCounter audioTimeIndexCounter = new TimeIndexCounter();

    private int keyFrameCount = 0;
    private int frameCount = 0;
    private long bitrateCount = 0;
    private boolean startCount = false;
    private Handler mFrameCountHandler;
    private Runnable mFrameCountProducer = new Runnable() {
        @Override
        public void run() {
            LogUtil.d("frame count fps:" + frameCount + " bitrate:" + (bitrateCount * 8 / 1000));
            frameCount = 0;
            bitrateCount = 0;
            mFrameCountHandler.postDelayed(mFrameCountProducer, 1000);
        }
    };


    public RtmpMuxer(StreamPublishParam publishParam, VideoMediaData videoMediaData, AudioMediaData audioMediaData) throws IOException {
        super(videoMediaData, audioMediaData);
        try {
            mStreamPublishParam = publishParam;
        } catch (final NullPointerException e) {
            throw new RuntimeException("This app has no permission of writing external storage");
        }
        mRtmpMuxer = new RTMPMuxer();
        mEncoderCount = mStatredCount = 0;
        mIsStarted = false;
        mIsConnected = false;
        initRtmpMuxer(mStreamPublishParam);
    }

    private void initRtmpMuxer(StreamPublishParam publishParam) {
        this.mStreamPublishParam = publishParam;
        int open = mRtmpMuxer.open(this.mStreamPublishParam.getRtmpUrl(),
                this.mStreamPublishParam.getVideoWidth(), this.mStreamPublishParam.getVideoHeight());
        LogUtil.d("initRtmpMuxer open:" + open);
        boolean connected = mRtmpMuxer.isConnected();
        //int connected = mRtmpMuxer.isConnected();
        //if(connected>0){
        if (connected) {
            mIsConnected = true;
        }
        LogUtil.d("initRtmpMuxer connected:" + connected + " mIsConnected:" + mIsConnected);
        if (mStreamPublishParam.isNeedLocalWrite()) {
            mRtmpMuxer.file_open(this.mStreamPublishParam.getOutputFilePath());
            mRtmpMuxer.write_flv_header(true, true);
        }
        videoTimeIndexCounter.reset();
        audioTimeIndexCounter.reset();
        mFrameCountHandler = new Handler();

        frameSender = new FrameSender(new FrameSender.FrameSenderCallback() {
            @Override
            public void onStart() {
            }

            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {
                if (mRtmpMuxer.writeVideo(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime()) >= 0) {
                    LogUtil.d("onSendVideo " + sendFrame.length + " pts:" + sendFrame.bufferInfo.getTotalTime());
                    if (!startCount) {
                        startCount = true;
                        mFrameCountHandler.postDelayed(mFrameCountProducer, 1000);
                    }
                    bitrateCount = bitrateCount + sendFrame.length;
                    frameCount++;
                } else {
                    LogUtil.d("onSendVideo error " + sendFrame.length);
                }

            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {
                LogUtil.d("onSendAudio " + sendFrame.length + " pts:" + sendFrame.bufferInfo.getTotalTime());
                mRtmpMuxer.writeAudio(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
            }

            @Override
            public void close() {

            }
        });
        frameSender.sendStartMessage();
    }

    public void prepareEncoders() throws IOException {
        if (mVideoEncoder != null)
            mVideoEncoder.prepare();
        if (mAudioEncoder != null)
            mAudioEncoder.prepare();
    }


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
        mFrameCountHandler.removeCallbacks(mFrameCountProducer);
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
        LogUtil.d("start");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mIsStarted = true;
            notifyAll();
            LogUtil.d("MediaMuxer started mIsStarted:" + mIsStarted + " mIsConnected:" + mIsConnected);
        }
        return mIsStarted && mIsConnected;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    @Override
    public synchronized void stopMuxer() {
        LogUtil.d("stop:mStatredCount=" + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            if (mStreamPublishParam.isNeedLocalWrite()) {
                mRtmpMuxer.file_close();
            }
            mRtmpMuxer.close();
            mIsStarted = false;
            LogUtil.d("MediaMuxer stopped:");
        }
    }

    @Override
    public synchronized int addTrackToMuxer(MediaEncoderFormat format) {
        return 0;
    }

    @Override
    public synchronized boolean isMuxerStarted() {
        return mIsStarted && mIsConnected;
    }


    /**
     * @param encodedFrame
     */
    @Override
    protected synchronized void writeEncodedData(EncodedFrame encodedFrame) {
        if (mStatredCount > 0) {
            LogUtil.i("addTrack:" + encodedFrame.getmTrackIndex() + ",EncodedFrame=" + encodedFrame);
            ByteBuffer mBuffer = encodedFrame.getEncodedByteBuffer();
            mBuffer.position(0);

            int offset = 0;

            int readLength = encodedFrame.getmBufferInfo().size - mBuffer.position();
            byte[] buffer = new byte[readLength];
            mBuffer.get(buffer, offset, readLength);
            if (encodedFrame.getCodecType() == MediaEncoderFormat.CodecType.AUDIO) {
                LogUtil.i("audio sendAddFrameMessage:" + encodedFrame.getCodecType() + " " + readLength);
                audioTimeIndexCounter.calcTotalTime(encodedFrame.getmBufferInfo().presentationTimeUs);
                frameSender.sendAddFrameMessage(buffer, offset, readLength, new BufferInfoEx(encodedFrame.getmBufferInfo(), audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);
            } else if (encodedFrame.getCodecType() == MediaEncoderFormat.CodecType.VIDEO) {
                boolean keyFrame = (encodedFrame.getmBufferInfo().flags & MediaCodec.BUFFER_FLAG_KEY_FRAME) != 0;
                LogUtil.i("video sendAddFrameMessage:" + encodedFrame.getCodecType() + " " + readLength + " keyFrame:" + keyFrame + " keyFrameCount" + keyFrameCount);
                if (keyFrame) {
                    keyFrameCount = 0;
                    LogUtil.i("frame count video keyframe size:" + readLength);
                } else {
                    keyFrameCount++;
                }
                videoTimeIndexCounter.calcTotalTime(encodedFrame.getmBufferInfo().presentationTimeUs);
                frameSender.sendAddFrameMessage(buffer, offset, readLength, new BufferInfoEx(encodedFrame.getmBufferInfo(), videoTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_VIDEO);
            }
        }


    }

    @Override
    public int onDataAvailable(EncodedFrame data) {
        this.writeEncodedData(data);
        return 0;
    }

}
