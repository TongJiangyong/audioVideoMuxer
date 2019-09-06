package com.serenegiant.encoder;
/*
 * AudioVideoRecordingSample
 * Sample project to cature audio and video from internal mic/camera and save as MPEG4 file.
 *
 * Copyright (c) 2014-2015 saki t_saki@serenegiant.com
 *
 * File name: MediaCodecEncoder.java
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

import android.media.MediaCodec;
import android.media.MediaFormat;


import com.serenegiant.model.EncodedFrame;
import com.serenegiant.model.MediaEncoderFormat;
import com.serenegiant.utils.LogUtil;

import static com.serenegiant.audiovideosample.ConstantMediaConfig.TIMEOUT_USEC;

public abstract class MediaCodecEncoder extends BaseEncoder implements Runnable {
    private static final boolean DEBUG = true;    // TODO set false on release
    private static final String TAG = "MediaCodecEncoder";

    protected final Object mSync = new Object();
    /**
     * Flag that indicate this encoder is capturing now.
     */
    protected volatile boolean mIsCapturing;
    /**
     * Flag that indicate the frame data will be available soon.
     */
    private int mRequestDrain;
    /**
     * Flag to request stop capturing
     */
    protected volatile boolean mRequestStop;
    /**
     * Flag that indicate encoder received EOS(End Of Stream)
     */
    protected boolean mIsEOS;
    /**
     * Flag the indicate the muxer is running
     */
    protected boolean mOutputBufferEnabled;
    /**
     * Track Number
     */
    protected int mTrackIndex;
    /**
     * MediaCodec instance for encoding
     */
    protected MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    /**
     * Weak refarence of MediaMuxerWarapper instance
     */
//    protected final WeakReference<AndroidMediaMuxer> mWeakMuxer;
    /**
     * BufferInfo instance for dequeuing
     */
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)

    protected MediaEncoderFormat.CodecType codecType; //0为audio 1为video

    protected boolean enableCallback = true;

    public MediaCodecEncoder(final IEncoderListener listener, String codecThreadName) {
        super(listener);
        enableCallback = true;
        if (listener == null) throw new NullPointerException("MediaEncoderListener is null");
//    	if (muxer == null) throw new NullPointerException("AndroidMediaMuxer is null");
//		mWeakMuxer = new WeakReference<AndroidMediaMuxer>(muxer);
//		muxer.addEncoder(this);
        this.mEncoderListener = listener;
        synchronized (mSync) {
            // create BufferInfo here for effectiveness(to reduce GC)
            mBufferInfo = new MediaCodec.BufferInfo();
            // wait for starting thread
            //启动当前线程
            new Thread(this, codecThreadName).start();
            try {
                mSync.wait();
            } catch (final InterruptedException e) {
            }
        }
    }


    /**
     * the method to indicate frame data is soon available or already available
     *
     * @return return true if encoder is ready to encod.
     */

    public boolean frameAvailableSoon() {

        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return false;
            }
            //ZH 通过mRequestDrain的数量，来控制是否还继续下去？
            mRequestDrain++;
            mSync.notifyAll();
        }
        return true;
    }

    /**
     * encoding loop on private thread
     */
    @Override
    public void run() {
//		android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        synchronized (mSync) {
            mRequestStop = false;
            mRequestDrain = 0;
            mSync.notify();
        }
        final boolean isRunning = true;
        boolean localRequestStop;
        boolean localRequestDrain;
        while (isRunning) {
            LogUtil.i("MediaCodecEncoder run MediaCodecEncoder type：" + codecType + " mRequestStop:" + mRequestStop);
            synchronized (mSync) {
                localRequestStop = mRequestStop;
                localRequestDrain = (mRequestDrain > 0);
                if (localRequestDrain)
                    mRequestDrain--;
            }
            if (localRequestStop) {
                drain();
                // request stop recording
                LogUtil.i("signalEndOfInputStream codecType " + codecType);
                signalEndOfInputStream();
                // process output data again for EOS signale
                drain();
                // release all related objects
                release();
                break;
            }
            if (localRequestDrain) {
                if (codecType == MediaEncoderFormat.CodecType.VIDEO) {
                    LogUtil.i("video drain() mRequestDrain:" + mRequestDrain);
                }
                drain();
            } else {
                synchronized (mSync) {
                    try {
                        LogUtil.i("video drain() mSync.wait() mRequestDrain:" + mRequestDrain);
                        mSync.wait();
                    } catch (final InterruptedException e) {
                        break;
                    }
                }
            }
        } // end of while
        LogUtil.d("Encoder thread exiting");
        synchronized (mSync) {
            mRequestStop = true;
            mIsCapturing = false;
        }
    }

    /*
    * prepareing method for each sub class
    * this method should be implemented in sub class, so set this as abstract method
    * @throws IOException
    */
    @Override
    public abstract void prepare() throws IOException;

    @Override
    public void startRecording() {
        LogUtil.v("startEncoders");
        synchronized (mSync) {
            mIsCapturing = true;
            mRequestStop = false;
            mSync.notifyAll();
        }
    }

    /**
     * the method to request stop encoding
     */
    @Override
    public void stopRecording() {
        LogUtil.v("stopEncoders");
        synchronized (mSync) {
            if (!mIsCapturing || mRequestStop) {
                return;
            }
            LogUtil.v("try to stopEncoders codecType:" + codecType);
            mRequestStop = true;    // for rejecting newer frame
            mSync.notifyAll();
            // We can not know when the encoding and writing finish.
            // so we return immediately after request to avoid delay of caller thread
        }
    }

//********************************************************************************
//********************************************************************************

    /**
     * Release all releated objects
     */
    protected void release() {
        LogUtil.d("release");
        if (mEncoderListener != null&&enableCallback) {
            try {
                mEncoderListener.onEncoderStopped(this);
            } catch (final Exception e) {
                LogUtil.e("failed onStopped:" + e);
            }
        }
        mIsCapturing = false;
        if (mMediaCodec != null) {
            try {
                mMediaCodec.stop();
                mMediaCodec.release();
                mMediaCodec = null;
            } catch (final Exception e) {
                LogUtil.e("failed releasing MediaCodec:" + e);
            }
        }
        if (mEncoderListener != null&&enableCallback) {
            try {
                mEncoderListener.onEncoderReleased();
            } catch (final Exception e) {
                LogUtil.e("failed onEncoderReleased" + e);
            }
        }

        mBufferInfo = null;
    }

    protected abstract void signalEndOfInputStream();

    /**
     * Method to set byte array to the MediaCodec encoder
     *
     * @param buffer
     * @param length             　length of byte array, zero means EOS.
     * @param presentationTimeUs
     */
    protected void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (!mIsCapturing) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        while (mIsCapturing) {
            final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            if (inputBufferIndex >= 0) {
                final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                if (buffer != null) {
                    inputBuffer.put(buffer);
                }
//	             LogUtil.v("encode:queueInputBuffer");
                if (length <= 0) {
                    // send EOS
                    mIsEOS = true;
                    LogUtil.i("send BUFFER_FLAG_END_OF_STREAM");
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                            presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    break;
                } else {
                    if (codecType == MediaEncoderFormat.CodecType.VIDEO) {
                        LogUtil.i("queueInputBuffer:" + inputBufferIndex + " length:" + length);
                    }
                    mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                            presentationTimeUs, 0);
                }
                break;
            } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait for MediaCodec encoder is ready to encode
                // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
                // will wait for maximum TIMEOUT_USEC(10msec) on each call
            }
        }
    }

    /**
     * drain encoded data and write them to muxer
     */
    @Override
    public void drain() {
        if (mMediaCodec == null) return;
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus, count = 0;
//        final AndroidMediaMuxer muxer = mWeakMuxer.get();
//        if (muxer == null) {
////        	throw new NullPointerException("muxer is unexpectedly null");
//        	LogUtil.w("muxer is unexpectedly null");
//        	return;
//        }
        LOOP:
        while (mIsCapturing) {
            // get encoded data with maximum timeout duration of TIMEOUT_USEC(=10[msec])
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (codecType == MediaEncoderFormat.CodecType.VIDEO) {
                LogUtil.i(codecType + "video drain encoderStatus" + encoderStatus);
            }
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // wait 5 counts(=TIMEOUT_USEC x 5 = 50msec) until data/EOS come
                if (!mIsEOS) {
                    if (++count > 5)
                        break LOOP;        // out of while
                }
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                LogUtil.v("INFO_OUTPUT_BUFFERS_CHANGED");
                // this shoud not come when encoding
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                LogUtil.v("INFO_OUTPUT_FORMAT_CHANGED");
                // this status indicate the output format of codec is changed
                // this should come only once before actual encoded data
                // but this status never come on Android4.3 or less
                // and in that case, you should treat when MediaCodec.BUFFER_FLAG_CODEC_CONFIG come.
                if (mOutputBufferEnabled) {    // second time request is error
                    throw new RuntimeException("format changed twice");
                }
                // get output format from codec and pass them to muxer
                // getOutputFormat should be called after INFO_OUTPUT_FORMAT_CHANGED otherwise crash.
                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                //mTrackIndex = muxer.addTrack(format);
                mOutputBufferEnabled = true;
                if (mEncoderListener != null&&enableCallback) {
                    try {
                        mTrackIndex = mEncoderListener.onEncoderOutPutBufferReady(new MediaEncoderFormat(format));
                    } catch (final Exception e) {
                        break LOOP;
                    }
                }
                enableCallback = true;
            } else if (encoderStatus < 0) {
                // unexpected status
                LogUtil.w("drain:unexpected result from encoder#dequeueOutputBuffer: " + encoderStatus);
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if (encodedData == null) {
                    // this never should come...may be a MediaCodec internal error
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepareEncoders output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    LogUtil.d("drain:BUFFER_FLAG_CODEC_CONFIG");
                    //remove this,may cause trouble in local record
                    //mBufferInfo.size = 0;
                }
                if (mBufferInfo.size != 0) {
                    // encoded data is ready, clear waiting counter
                    count = 0;
                    if (!mOutputBufferEnabled) {
                        // muxer is not ready...this will prrograming failure.
                        throw new RuntimeException("drain:muxer hasn't started");
                    }
                    // write encoded data to muxer(need to adjust presentationTimeUs.
                    mBufferInfo.presentationTimeUs = getPTSUs();
                    //muxer.writeSampleData(mTrackIndex, encodedData, mBufferInfo);
                    mEncoderedDataConnector.onDataAvailable(new EncodedFrame(codecType, mTrackIndex, encodedData, mBufferInfo));
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    // when EOS come.
                    mIsCapturing = false;
                    break;      // out of while
                }
            }
        }
    }


    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    protected long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }


}
