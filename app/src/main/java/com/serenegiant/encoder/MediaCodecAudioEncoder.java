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
import java.nio.ByteBuffer;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

public class MediaCodecAudioEncoder extends MediaCodecEncoder {
	private static final boolean DEBUG = false;	// TODO set false on release
	private static final String TAG = "MediaCodecAudioEncoder";

	private static final String MIME_TYPE = "audio/mp4a-latm";
	private static final int BIT_RATE = 64000;
	private static final int SAMPLE_RATE = 44100;

	public MediaCodecAudioEncoder(final IEncoderListener listener) {
		super(listener);
	}

	@Override
	public void prepare() throws IOException {
		if (DEBUG) Log.v(TAG, "prepareEncoders:");
        mTrackIndex = -1;
		codecType = 0;
        mOutputBufferEnabled = mIsEOS = false;
        // prepareEncoders MediaCodec for AAC encoding of audio data from inernal mic.
        final MediaCodecInfo audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
		if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

        final MediaFormat audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
		audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);
		audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
		audioFormat.setInteger(MediaFormat.KEY_BITRATE_MODE,
				MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
		if (DEBUG) Log.i(TAG, "format: " + audioFormat);
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepareEncoders finishing");
        if (mListener != null) {
        	try {
        		mListener.onEncoderPrepared(this);
        	} catch (final Exception e) {
        		Log.e(TAG, "prepareEncoders:", e);
        	}
        }
	}

    @Override
	public void startRecording() {
		super.startRecording();
		// create and execute audio capturing thread using internal mic
	}

	@Override
    protected void release() {
		super.release();
    }






    /**
     * select the first codec that match a specific MIME type
     * @param mimeType
     * @return
     */
    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
    	if (DEBUG) Log.v(TAG, "selectAudioCodec:");

    	MediaCodecInfo result = null;
    	// get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
LOOP:	for (int i = 0; i < numCodecs; i++) {
        	final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {	// skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
            	if (DEBUG) Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
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
		if(mRequestStop){
			return 0;
		}
		if(data!=null){
			if (DEBUG) Log.d(TAG, "onDataAvailable encode");
			encode(data.mBuffer,data.mLength,data.mTimeStamp);
		}
		super.frameAvailableSoon();
		return 0;
	}

}
