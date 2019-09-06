package com.serenegiant.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaCodecAudioEncoder.java
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

import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;


import com.serenegiant.model.AudioMediaData;
import com.serenegiant.model.MediaEncoderFormat;
import com.serenegiant.model.VideoCaptureFrame;
import com.serenegiant.utils.LogUtil;

public class MediaCodecAudioEncoder extends MediaCodecEncoder {
    private static final boolean DEBUG = false;    // TODO set false on release
    private static final String TAG = "MediaCodecAudioEncoder";

    private AudioMediaData mAudioMediaData;

    public MediaCodecAudioEncoder(final IEncoderListener listener, AudioMediaData audioMediaData) {
        super(listener, "MediaCodecAudioEncoder");
        mAudioMediaData = audioMediaData;
    }

    @Override
    public void prepare() throws IOException {
        LogUtil.v("prepareEncoders:");
        mTrackIndex = -1;
        codecType = MediaEncoderFormat.CodecType.AUDIO;
        mOutputBufferEnabled = mIsEOS = false;
        // prepareEncoders MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(mAudioMediaData.getAudioMimeType());
        if (audioCodecInfo == null) {
            LogUtil.e("Unable to find an appropriate codec for " + mAudioMediaData.getAudioMimeType());
            return;
        }
        LogUtil.i("selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(
                mAudioMediaData.getAudioMimeType(),
                mAudioMediaData.getAudioSampleRate(),
                mAudioMediaData.getAudioChannelCount());
        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, mAudioMediaData.getAudioAacProfile());
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, mAudioMediaData.getAudioChannelFormat());
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, mAudioMediaData.getAudioBitRate());
        audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
        LogUtil.i("format: " + audioFormat);
        mMediaCodec = MediaCodec.createEncoderByType(mAudioMediaData.getAudioMimeType());
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        LogUtil.i("prepareEncoders finishing");
        if (mListener != null) {
            try {
                mListener.onEncoderPrepared(this);
            } catch (final Exception e) {
                LogUtil.e("prepareEncoders:" + e);
            }
        }
    }

    @Override
    public void startRecording() {
        super.startRecording();
        // create and execute audio capturing thread using internal mic
    }

    @Override
    public void release() {
        super.release();
    }

    @Override
    protected void signalEndOfInputStream() {
        LogUtil.d("sending0 EOS to encoder codecType" + codecType);
        encode(null, 0, getPTSUs());
    }

    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        LogUtil.v("selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                LogUtil.i("supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }


    @Override
    public int onDataAvailable(VideoCaptureFrame data) {
        if (mRequestStop) {
            return 0;
        }
        if (data != null) {
            LogUtil.d("onDataAvailable encode");
            encode(data.mBuffer, data.mLength, data.mTimeStamp);
        }
        super.frameAvailableSoon();
        return 0;
    }

}
