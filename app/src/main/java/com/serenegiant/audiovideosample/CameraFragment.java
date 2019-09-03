package com.serenegiant.audiovideosample;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: CameraFragment.java
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

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.serenegiant.audio_capture.AudioCaptureThread;
import com.serenegiant.encoder.AudioMediaData;
import com.serenegiant.encoder.VideoEncoderDataPrepare;
import com.serenegiant.encoder.VideoMediaData;
import com.serenegiant.muxer.AndroidMediaMuxer;
import com.serenegiant.muxer.BaseMuxer;
import com.serenegiant.muxer.RtmpMuxer;
import com.serenegiant.muxer.StreamPublishParam;
import com.serenegiant.encoder.BaseEncoder;
import com.serenegiant.encoder.IEncoderListener;
import com.serenegiant.encoder.MediaCodecAudioEncoder;
import com.serenegiant.encoder.MediaCodecVideoEncoder;
import com.serenegiant.encoder.MediaEncoderFormat;

public class CameraFragment extends Fragment {
	private static final boolean DEBUG = true;	// TODO set false on release
	private static final String TAG = "CameraFragment";

	/**
	 * for camera preview display
	 */
	private CameraGLView mCameraView;
	/**
	 * for scale mode display
	 */
	private TextView mScaleModeView;
	/**
	 * button for start/stop recording
	 */
	private ImageButton mRecordButton;
	/**
	 * muxer for audio/video recording
	 */
	private BaseMuxer mMuxer;

	private BaseEncoder audioEncoder,videoEncoder;
	private VideoEncoderDataPrepare videoEncoderDataPrepare;
	private VideoMediaData videoMediaData;
	private AudioMediaData audioMediaData;
	private AudioCaptureThread audioCaptureThread;
	public CameraFragment() {
		// need default constructor
	}

	@Override
	public View onCreateView(final LayoutInflater inflater, final ViewGroup container, final Bundle savedInstanceState) {
		final View rootView = inflater.inflate(R.layout.fragment_main, container, false);
		mCameraView = (CameraGLView)rootView.findViewById(R.id.cameraView);
		mCameraView.setVideoSize(1280, 720);
		mCameraView.setOnClickListener(mOnClickListener);
		mScaleModeView = (TextView)rootView.findViewById(R.id.scalemode_textview);
		updateScaleModeText();
		mRecordButton = (ImageButton)rootView.findViewById(R.id.record_button);
		mRecordButton.setOnClickListener(mOnClickListener);
		return rootView;
	}

	@Override
	public void onResume() {
		super.onResume();
		if (DEBUG) Log.v(TAG, "onResume:");
		mCameraView.onResume();
	}

	@Override
	public void onPause() {
		if (DEBUG) Log.v(TAG, "onPause:");
		stopRecording();
		mCameraView.onPause();
		super.onPause();
	}

	/**
	 * method when touch record button
	 */
	private final OnClickListener mOnClickListener = new OnClickListener() {
		@Override
		public void onClick(final View view) {
			switch (view.getId()) {
			case R.id.cameraView:
				final int scale_mode = (mCameraView.getScaleMode() + 1) % 4;
				mCameraView.setScaleMode(scale_mode);
				updateScaleModeText();
				break;
			case R.id.record_button:
				if (mMuxer == null)
					startRecording();
				else
					stopRecording();
				break;
			}
		}
	};

	private void updateScaleModeText() {
		final int scale_mode = mCameraView.getScaleMode();
		mScaleModeView.setText(
			scale_mode == 0 ? "scale to fit"
			: (scale_mode == 1 ? "keep aspect(viewport)"
			: (scale_mode == 2 ? "keep aspect(matrix)"
			: (scale_mode == 3 ? "keep aspect(crop center)" : ""))));
	}

	/**
	 * start resorcing
	 * This is a sample project and call this on UI thread to avoid being complicated
	 * but basically this should be called on private thread because prepareing
	 * of encoder is heavy work
	 */
	private void startRecording() {
		if (DEBUG) Log.v(TAG, "startEncoders:");
		try {
			videoMediaData = new VideoMediaData();
			audioMediaData = new AudioMediaData();
			mRecordButton.setColorFilter(0xffff0000);	// turn red
			//mMuxer = new AndroidMediaMuxer("/sdcard/testAudioVideo.mp4");	// if you record audio only, ".m4a" is also OK.
			StreamPublishParam streamPublishParam = new StreamPublishParam();
			streamPublishParam.setRtmpUrl("rtmp://10.63.0.16:1935/live/room");
			streamPublishParam.setOutputFilePath("/sdcard/testAudioVideo.flv");
			streamPublishParam.setNeedLocalWrite(true);
			streamPublishParam.setVideoHeight(1280);
			streamPublishParam.setVideoWidth(720);
			mMuxer = new RtmpMuxer(streamPublishParam);
			if (true) {
				// for video capturing
				videoEncoder = new MediaCodecVideoEncoder(mMediaEncoderListener, mCameraView.getVideoWidth(), mCameraView.getVideoHeight());
			}
			if (true) {
				// for audio capturing
				audioEncoder = new MediaCodecAudioEncoder(mMediaEncoderListener);
			}
			mMuxer.addEncoder(videoEncoder);
			mMuxer.addEncoder(audioEncoder);
			videoEncoder.getEncoderedDataConnector().connect(mMuxer);
			audioEncoder.getEncoderedDataConnector().connect(mMuxer);
			mMuxer.prepareEncoders();
			mMuxer.startEncoders();
		} catch (final IOException e) {
			mRecordButton.setColorFilter(0);
			Log.e(TAG, "startCapture:", e);
		}
	}

	/**
	 * request stop recording
	 */
	private void stopRecording() {
		if (DEBUG) Log.v(TAG, "stopEncoders:mMuxer=" + mMuxer);
		mRecordButton.setColorFilter(0);	// return to default color

		if (mMuxer != null) {
			mMuxer.stopEncoders();
			// you should not wait here
		}
		if(audioCaptureThread !=null){
			audioCaptureThread.stopAudioCapture();
		}

		if(videoEncoderDataPrepare!=null){
			videoEncoderDataPrepare.stopEncoderDataPrepare();
		}

	}

	/**
	 * callback methods from encoder
	 */
	private final IEncoderListener mMediaEncoderListener = new IEncoderListener() {


		@Override
		public void onEncoderStopped(BaseEncoder mediaEncoder) {
			if (DEBUG) Log.v(TAG, "onStopped:encoder=" + mediaEncoder);
			if (mediaEncoder instanceof MediaCodecVideoEncoder)
				mCameraView.setVideoCodecContext(null);
		}

		@Override
		public void onEncoderPrepared(BaseEncoder mediaEncoder) {
			if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + mediaEncoder);
			//very important to put data when encoder all complete
			if (mediaEncoder instanceof MediaCodecVideoEncoder)
			{
				videoEncoderDataPrepare = new VideoEncoderDataPrepare();
				videoEncoderDataPrepare.startEncoderDataPrepare(((MediaCodecVideoEncoder)mediaEncoder).getEncoderInputSurface(),videoMediaData);
				videoEncoderDataPrepare.getRawDataConnector().connect(mediaEncoder);
				mCameraView.setVideoCodecContext(videoEncoderDataPrepare);
			}else if(mediaEncoder instanceof MediaCodecAudioEncoder){
				audioCaptureThread = new AudioCaptureThread();
				audioCaptureThread.getCaptureDataConnector().connect(mediaEncoder);
				audioCaptureThread.startAudioCapture();
			}

		}

		@Override
		public void onEncoderReleased() throws Exception {
			try {
				if (DEBUG) Log.v(TAG, "onEncoderReleased");
				mMuxer.stopMuxer();
			} catch (Exception e) {
				throw e;
			}
		}



		//only if outputBUffer ready ，muxer can write data
		@Override
		public int onEncoderOutPutBufferReady(MediaEncoderFormat mediaEncoderFormat) throws InterruptedException {
			int trackIndex = mMuxer.addTrackToMuxer(mediaEncoderFormat);
			if (!mMuxer.startMuxer()) {
               		// we should wait until muxer is ready
					if (DEBUG) Log.v(TAG, "onEncoderOutPutBufferReady");
               		synchronized (mMuxer) {
	               		while (!mMuxer.isMuxerStarted())
						try {
							mMuxer.wait(100);
						} catch (Exception e) {
							throw e;
						}
               		}
               	}
			return trackIndex;
		}
	};
}
