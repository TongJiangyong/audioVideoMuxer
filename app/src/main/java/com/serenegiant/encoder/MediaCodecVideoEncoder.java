package com.serenegiant.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaCodecVideoEncoder.java
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

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;

import android.view.Surface;

import com.serenegiant.model.MediaEncoderFormat;
import com.serenegiant.model.VideoCaptureFrame;
import com.serenegiant.model.VideoMediaData;
import com.serenegiant.utils.LogUtil;


public class MediaCodecVideoEncoder extends MediaCodecEncoder {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "MediaCodecVideoEncoder";


    private final VideoMediaData mVideoMediaData;
    private Surface mSurface;

    public MediaCodecVideoEncoder(final IEncoderListener listener, VideoMediaData videoMediaData) {
        super(listener, "MediaCodecAudioEncoder");
        LogUtil.i("MediaCodecVideoEncoder: ");
        mVideoMediaData = videoMediaData;
    }

    public Surface getEncoderInputSurface() {
        return mSurface;
    }


    @Override
    public void prepare() throws IOException {
        LogUtil.i("prepareEncoders: ");
        mTrackIndex = -1;
        codecType = MediaEncoderFormat.CodecType.VIDEO;
        mOutputBufferEnabled = mIsEOS = false;

        final MediaCodecInfo videoCodecInfo = selectVideoCodec(mVideoMediaData.getVideoMimeType());
        if (videoCodecInfo == null) {
            LogUtil.e("Unable to find an appropriate codec for " + mVideoMediaData.getVideoMimeType());
            return;
        }
        LogUtil.i("selected codec: " + videoCodecInfo.getName());

        final MediaFormat format = MediaFormat.createVideoFormat(
                mVideoMediaData.getVideoMimeType(),
                mVideoMediaData.getVideoEncodeWidth(),
                mVideoMediaData.getVideoEncodeHeight());
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);    // API >= 18
        format.setInteger(MediaFormat.KEY_BIT_RATE, mVideoMediaData.getVideoEncodeBitrate());
        format.setInteger(MediaFormat.KEY_FRAME_RATE, mVideoMediaData.getVideoFrameRate());
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mVideoMediaData.getVideoKeyIframe());
        format.setInteger(MediaFormat.KEY_BITRATE_MODE,
                MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
        LogUtil.i("format: " + format);

        mMediaCodec = MediaCodec.createEncoderByType(mVideoMediaData.getVideoMimeType());
        mMediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        // get Surface for encoder input
        // this method only can call between #configure and #start
        mSurface = mMediaCodec.createInputSurface();    // API >= 18
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
    public void release() {
        LogUtil.i("release:");
        if (mSurface != null) {
            mSurface.release();
            mSurface = null;
        }
        super.release();
    }


    /**
     * select the first codec that match a specific MIME type
     *
     * @param mimeType
     * @return null if no codec matched
     */
    protected static final MediaCodecInfo selectVideoCodec(final String mimeType) {
        LogUtil.v("selectVideoCodec:");

        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);

            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            // select first codec that match a specific MIME type and color format
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mimeType)) {
                    LogUtil.i("codec:" + codecInfo.getName() + ",MIME=" + types[j]);
                    final int format = selectColorFormat(codecInfo, mimeType);
                    if (format > 0) {
                        return codecInfo;
                    }
                }
            }
        }
        return null;
    }

    /**
     * select color format available on specific codec and we can use.
     *
     * @return 0 if no colorFormat is matched
     */
    protected static final int selectColorFormat(final MediaCodecInfo codecInfo, final String mimeType) {
        LogUtil.i("selectColorFormat: ");
        int result = 0;
        final MediaCodecInfo.CodecCapabilities caps;
        try {
            Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            caps = codecInfo.getCapabilitiesForType(mimeType);
        } finally {
            Thread.currentThread().setPriority(Thread.NORM_PRIORITY);
        }
        int colorFormat;
        for (int i = 0; i < caps.colorFormats.length; i++) {
            colorFormat = caps.colorFormats[i];
            if (isRecognizedViewoFormat(colorFormat)) {
                if (result == 0)
                    result = colorFormat;
                break;
            }
        }
        if (result == 0)
            LogUtil.e("couldn't find a good color format for " + codecInfo.getName() + " / " + mimeType);
        return result;
    }

    /**
     * color formats that we can use in this class
     */
    protected static int[] recognizedFormats;

    static {
        recognizedFormats = new int[]{
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
//        	MediaCodecInfo.CodecCapabilities.COLOR_QCOM_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface,
        };
    }

    private static final boolean isRecognizedViewoFormat(final int colorFormat) {
        LogUtil.i("isRecognizedViewoFormat:colorFormat=" + colorFormat);
        final int n = recognizedFormats != null ? recognizedFormats.length : 0;
        for (int i = 0; i < n; i++) {
            if (recognizedFormats[i] == colorFormat) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void signalEndOfInputStream() {
        LogUtil.d("sending EOS to encoder codecType" + codecType);
        mMediaCodec.signalEndOfInputStream();    // API >= 18
        mIsEOS = true;
    }

    @Override
    public int onDataAvailable(VideoCaptureFrame data) {
        if (mRequestStop) {
            return 0;
        }
        super.frameAvailableSoon();
        return 0;
    }
}
