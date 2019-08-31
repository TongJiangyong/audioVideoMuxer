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

import android.util.Log;

import com.serenegiant.encoder.BaseEncoder;
import com.serenegiant.encoder.MediaCodecAudioEncoder;
import com.serenegiant.encoder.MediaCodecEncoder;
import com.serenegiant.encoder.MediaCodecVideoEncoder;
import com.serenegiant.encoder.MediaEncoderFormat;

import net.butterflytv.rtmp_client.RTMPMuxer;

public class RtmpMuxer extends BaseMuxer{
    private static final boolean DEBUG = true;	// TODO set false on release
    private static final String TAG = "RtmpMuxer";
    private static final SimpleDateFormat mDateTimeFormat = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss", Locale.US);
    private StreamPublishParam mStreamPublishParam;
    private String mOutputPath;
    private final RTMPMuxer mRtmpMuxer;	// API >= 18
    private int mEncoderCount, mStatredCount;
    private boolean mIsStarted;
    public boolean mIsConnected;
    private MediaCodecEncoder mVideoEncoder, mAudioEncoder;
    private FrameSender frameSender;
    protected TimeIndexCounter videoTimeIndexCounter = new TimeIndexCounter();
    protected TimeIndexCounter audioTimeIndexCounter = new TimeIndexCounter();

    public RtmpMuxer(StreamPublishParam publishParam) throws IOException {
        super();
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

    private void initRtmpMuxer(StreamPublishParam publishParam){
        int open = mRtmpMuxer.open(publishParam.getRtmpUrl(),
                publishParam.getVideoWidth(), publishParam.getVideoHeight());
        if (DEBUG) Log.v(TAG,  "initRtmpMuxer open:"+open);
        int connected = mRtmpMuxer.isConnected();
        if(connected>0){
            mIsConnected = true;
        }
        if (DEBUG) Log.v(TAG,  "initRtmpMuxer connected:"+connected+ " mIsConnected:"+mIsConnected);
        mRtmpMuxer.file_open(publishParam.getOutputFilePath());
        mRtmpMuxer.write_flv_header(true, true);
        videoTimeIndexCounter.reset();
        audioTimeIndexCounter.reset();
        frameSender = new FrameSender(new FrameSender.FrameSenderCallback() {
            @Override
            public void onStart() {}

            @Override
            public void onSendVideo(FramePool.Frame sendFrame) {
                if (DEBUG) Log.v(TAG,  "onSendVideo "+sendFrame.length);
                mRtmpMuxer.writeVideo(sendFrame.data, 0, sendFrame.length, sendFrame.bufferInfo.getTotalTime());
            }

            @Override
            public void onSendAudio(FramePool.Frame sendFrame) {
                if (DEBUG) Log.v(TAG,  "onSendAudio "+sendFrame.length);
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
    }


//**********************************************************************
//**********************************************************************
    /**
     * assign encoder to this calss. this is called from encoder.
     * @param encoder instance of MediaCodecVideoEncoder or MediaCodecAudioEncoder
     */
    @Override
	/*package*/ public void addEncoder(final BaseEncoder encoder) {
        if (encoder instanceof MediaCodecVideoEncoder) {
            if (mVideoEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mVideoEncoder = (MediaCodecEncoder)encoder;
        } else if (encoder instanceof MediaCodecAudioEncoder) {
            if (mAudioEncoder != null)
                throw new IllegalArgumentException("Video encoder already added.");
            mAudioEncoder = (MediaCodecEncoder)encoder;
        } else
            throw new IllegalArgumentException("unsupported encoder");
        mEncoderCount = (mVideoEncoder != null ? 1 : 0) + (mAudioEncoder != null ? 1 : 0);
    }

    /**
     * request start recording from encoder
     * @return true when muxer is ready to write
     */
    @Override
	/*package*/ public synchronized boolean startMuxer() {
        if (DEBUG) Log.v(TAG,  "start");
        mStatredCount++;
        if ((mEncoderCount > 0) && (mStatredCount == mEncoderCount)) {
            mIsStarted = true;
            notifyAll();
            if (DEBUG) Log.v(TAG,  "MediaMuxer started mIsStarted:"+mIsStarted+ " mIsConnected:"+mIsConnected);
        }
        return mIsStarted&&mIsConnected;
    }

    /**
     * request stop recording from encoder when encoder received EOS
     */
    @Override
	/*package*/ public synchronized void stopMuxer() {
        if (DEBUG) Log.v(TAG,  "stop:mStatredCount=" + mStatredCount);
        mStatredCount--;
        if ((mEncoderCount > 0) && (mStatredCount <= 0)) {
            mRtmpMuxer.file_close();
            mRtmpMuxer.close();
            mIsStarted = false;
            if (DEBUG) Log.v(TAG,  "MediaMuxer stopped:");
        }
    }

    @Override
	public synchronized int addTrackToMuxer( MediaEncoderFormat format) {
        return 0;
    }

    @Override
    public synchronized boolean isMuxerStarted() {
        return mIsStarted&&mIsConnected;
    }

    /**
     *
     * @param encodedFrame
     */
    @Override
	/*package*/ protected synchronized void writeEncodedData(EncodedFrame encodedFrame) {
        if (mStatredCount > 0){
            if (DEBUG) Log.i(TAG, "addTrack:" + encodedFrame.getmTrackIndex() + ",EncodedFrame=" + encodedFrame);
            ByteBuffer mBuffer =  encodedFrame.getEncodedByteBuffer();
            mBuffer.position(0);

            int offset = 0;

            int readLength = encodedFrame.getmBufferInfo().size - mBuffer.position();
            byte[] buffer = new byte[readLength];
            mBuffer.get(buffer, offset, readLength);
            if(encodedFrame.getCodecType()==0){
                if (DEBUG) Log.i(TAG, "audio sendAddFrameMessage:"+encodedFrame.getCodecType()+" "+readLength);
                audioTimeIndexCounter.calcTotalTime(encodedFrame.getmBufferInfo().presentationTimeUs);
                frameSender.sendAddFrameMessage(buffer, offset, readLength, new BufferInfoEx(encodedFrame.getmBufferInfo(), audioTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_AUDIO);
            }else if(encodedFrame.getCodecType()==1){
                if (DEBUG) Log.i(TAG, "vido sendAddFrameMessage:"+encodedFrame.getCodecType()+" "+readLength);
                videoTimeIndexCounter.calcTotalTime(encodedFrame.getmBufferInfo().presentationTimeUs);
                frameSender.sendAddFrameMessage(buffer, offset, readLength, new BufferInfoEx(encodedFrame.getmBufferInfo(), videoTimeIndexCounter.getTimeIndex()), FramePool.Frame.TYPE_VIDEO);
            }
        }


    }

    @Override
    public int onDataAvailable(EncodedFrame data){
        this.writeEncodedData(data);
        return 0;
    }

}
