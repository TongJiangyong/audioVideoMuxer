package com.serenegiant.encoder;

import android.opengl.EGLContext;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import android.util.Log;
import android.view.Surface;

import com.serenegiant.connector.SinkConnector;
import com.serenegiant.connector.SrcConnector;
import com.serenegiant.gles.ProgramTexture2d;
import com.serenegiant.gles.ProgramTextureOES;
import com.serenegiant.gles.core.EglCore;
import com.serenegiant.gles.core.Program;
import com.serenegiant.gles.core.WindowSurface;
import com.serenegiant.model.VideoCaptureFrame;
import com.serenegiant.model.VideoMediaData;
import com.serenegiant.utils.LogUtil;
import com.serenegiant.utils.OtherUtil;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

import static com.serenegiant.model.VideoMediaData.CaptureFormat.I420;
import static com.serenegiant.model.VideoMediaData.CaptureFormat.NV21;
import static com.serenegiant.model.VideoMediaData.CaptureFormat.TEXTURE_OES;
import static com.serenegiant.model.VideoMediaData.CaptureType.BYTE_ARRAY;
import static com.serenegiant.model.VideoMediaData.CaptureType.TEXTURE;

/**
 * Created by yong on 2019/8/31.
 */

//this class is used to divide vido encoder and render
//need to set to render thread after video encoder prepare
//just like a render ,use for mediacodec with texture only
//raw data use connecter ,texture direct to mediacodec input surface
//startEncoderDataPrepare-->initEncoderContext(codec ready and used in egl conetx)-->setTextureId/frameAvailable-->
//updateSharedContext(when not use)-->stopEncoderDataPrepare
public class VideoEncoderDataPrepare implements Runnable, SinkConnector<VideoCaptureFrame> {
    private static final String TAG = "VideoPrepare";
    private static final boolean DEBUG = true;

    private static final int MSG_RELEASE_EGL_CONTEXT = 1;
    private static final int MSG_FRAME_AVAILABLE = 2;
    private static final int MSG_SET_TEXTURE_ID = 3;
    private static final int MSG_UPDATE_SHARED_CONTEXT = 4;
    private static final int MSG_QUIT = 5;
    private static final int MSG_INIT_EGL_CONTEXT = 6;

    private WindowSurface mInputWindowSurface;
    private EglCore mEglCore;
    private Program programTexture;
    private int mTextureId;
    private int mFrameNum;
    private VideoMediaData videoMediaData;
    private boolean mMVPInit;
    protected SrcConnector<VideoCaptureFrame> rawDataConnector;
    // ----- accessed by multiple threads -----
    private volatile EncoderHandler mHandler;
    private Surface mInputSurface;
    private Object mReadyFence = new Object();      // guards ready/running
    private boolean mReady;
    private boolean mRunning;

    public VideoEncoderDataPrepare(VideoMediaData videoMediaData) {
        rawDataConnector = new SrcConnector<>();
        this.videoMediaData = videoMediaData;
    }

    public SrcConnector<VideoCaptureFrame> getRawDataConnector() {
        return rawDataConnector;
    }

    @Override
    public int onDataAvailable(VideoCaptureFrame videoCaptureFrame) {
        if (videoMediaData.getVideoCaptureType() == TEXTURE) {
            if (mHandler != null) {
                synchronized (mReadyFence) {
                    if (!mReady) {
                        return -1;
                    }
                }
                mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, videoCaptureFrame));
            }
        } else if (videoMediaData.getVideoCaptureType() == BYTE_ARRAY) {
            if (!mReady) {
                return -1;
            }
            byte[] changedRawData = new byte[videoCaptureFrame.rawData.length];
            if (videoMediaData.getVideoCaptureFormat() == NV21) {
                NV21ToNV12(videoCaptureFrame.rawData, changedRawData, videoCaptureFrame.videoWidth, videoCaptureFrame.videoHeight);
            } else if (videoMediaData.getVideoCaptureFormat() == I420) {

            }
            final ByteBuffer buf = ByteBuffer.allocateDirect(changedRawData.length);
            buf.clear();
            buf.put(changedRawData);
            buf.flip();
            currentPTSUs = getPTSUs();
            LogUtil.i("VideoEncoderDataPrepare video length:" + changedRawData.length + " width:" + videoCaptureFrame.videoWidth + " height:" + videoCaptureFrame.videoHeight);
            rawDataConnector.onDataAvailable(new VideoCaptureFrame(buf, changedRawData.length, currentPTSUs));
            prevOutputPTSUs = currentPTSUs;


        }
        return 0;
    }


    @Override
    public void run() {
        // Establish a Looper for this thread, and define a Handler for it.
        Looper.prepare();
        synchronized (mReadyFence) {
            mHandler = new EncoderHandler(this);
            mReady = true;
            mReadyFence.notify();
        }
        Looper.loop();

        LogUtil.d("VideoEncoderDataPrepare thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
        LogUtil.d("VideoEncoderDataPrepare thread exiting over:" + mReady);
    }

    public void startEncoderDataPrepare(Surface encoderInputSurface) {
        LogUtil.d("startEncoderDataPrepare");
        this.mInputSurface = encoderInputSurface;
        if (this.videoMediaData.getVideoCaptureType() == TEXTURE) {
            synchronized (mReadyFence) {
                if (mRunning) {
                    LogUtil.w("Encoder thread already running " + this.videoMediaData.getVideoCaptureType());
                    return;
                }
                mRunning = true;
                new Thread(this, "VideoEncoderDataPrepare").start();
                while (!mReady) {
                    try {
                        mReadyFence.wait();
                    } catch (InterruptedException ie) {
                        // ignore
                    }
                }
            }
        } else if (this.videoMediaData.getVideoCaptureType() == BYTE_ARRAY) {
            if (mRunning) {
                LogUtil.w("Encoder thread already running " + this.videoMediaData.getVideoCaptureType());
                return;
            }
            mRunning = true;
            mReady = true;
        }

    }

    public void stopEncoderDataPrepare() {
        //TODO check whay handler dead
        LogUtil.i("stopEncoderDataPrepare");
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_EGL_CONTEXT));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        } else {
            mReady = mRunning = false;
        }
        rawDataConnector.disconnect();
        prevOutputPTSUs = 0;
        currentPTSUs = 0;
        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    public void updateSharedContext(EGLContext sharedContext) {
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
        }
    }

    public void initEncoderContext(EGLContext eglContext) {
        LogUtil.d("initEncoderContext");
        if (mHandler != null) {
            mHandler.sendMessage(mHandler.obtainMessage(MSG_INIT_EGL_CONTEXT, eglContext));
        }
    }


    private void handleInitEncoderContext(EGLContext eglContext) {
        mEglCore = new EglCore(eglContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, this.mInputSurface, true);
        mInputWindowSurface.makeCurrent();
        if (this.videoMediaData.getVideoCaptureType() == TEXTURE && this.videoMediaData.getVideoCaptureFormat() == TEXTURE_OES) {
            LogUtil.d("ProgramTextureOES");
            programTexture = new ProgramTextureOES();
        } else if (this.videoMediaData.getVideoCaptureType() == TEXTURE && this.videoMediaData.getVideoCaptureFormat() == VideoMediaData.CaptureFormat.TEXTURE_2D) {
            programTexture = new ProgramTexture2d();
        }
    }

    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        LogUtil.d("handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        programTexture.release();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        if (this.videoMediaData.getVideoCaptureType() == TEXTURE && this.videoMediaData.getVideoCaptureFormat() == TEXTURE_OES) {
            programTexture = new ProgramTextureOES();
        } else if (this.videoMediaData.getVideoCaptureType() == TEXTURE && this.videoMediaData.getVideoCaptureFormat() == VideoMediaData.CaptureFormat.TEXTURE_2D) {
            programTexture = new ProgramTexture2d();
        }
    }

    private float[] mMTX = new float[16];
    private float[] mMVP = new float[16];

    private void handleFrameAvailable(VideoCaptureFrame videoCaptureFrame) {
        mTextureId = videoCaptureFrame.mTextureId;
        videoCaptureFrame.mSurfaceTexture.getTransformMatrix(mMTX);
        videoCaptureFrame.mTexMatrix = mMTX;
        if (DEBUG)
            LogUtil.d("handleFrameAvailable videoCaptureFrame" + videoCaptureFrame + " time:" + videoCaptureFrame.mSurfaceTexture.getTimestamp());
        rawDataConnector.onDataAvailable(null);
        programTexture.drawFrame(mTextureId, videoCaptureFrame.mTexMatrix, videoCaptureFrame.mMvpMatrix);
        mInputWindowSurface.swapBuffers();
    }

    private void handleRleaseEglContext() {
        LogUtil.i("handleRleaseEglContext");
        if (mInputWindowSurface != null) {
            mInputWindowSurface.release();
            mInputWindowSurface = null;
        }
        if (programTexture != null) {
            programTexture.release();
            programTexture = null;
        }
        if (mEglCore != null) {
            mEglCore.release();
        }
    }

    /**
     * Handles encoder state change requests.  The handler is created on the encoder thread.
     */
    private static class EncoderHandler extends Handler {
        private WeakReference<VideoEncoderDataPrepare> mWeakEncoder;

        public EncoderHandler(VideoEncoderDataPrepare encoder) {
            mWeakEncoder = new WeakReference<VideoEncoderDataPrepare>(encoder);
        }

        @Override  // runs on encoder thread
        public void handleMessage(Message inputMessage) {
            int what = inputMessage.what;
            Object obj = inputMessage.obj;

            VideoEncoderDataPrepare encoder = mWeakEncoder.get();
            if (encoder == null) {
                LogUtil.w("EncoderHandler.handleMessage: encoder is null");
                return;
            }

            switch (what) {
                case MSG_RELEASE_EGL_CONTEXT:
                    encoder.handleRleaseEglContext();
                    break;
                case MSG_INIT_EGL_CONTEXT:
                    encoder.handleInitEncoderContext((EGLContext) obj);
                    break;
                case MSG_FRAME_AVAILABLE:
//                    long timestamp = (((long) inputMessage.arg1) << 32) |
//                            (((long) inputMessage.arg2) & 0xffffffffL);
                    encoder.handleFrameAvailable((VideoCaptureFrame) obj);
                    break;
                case MSG_UPDATE_SHARED_CONTEXT:
                    encoder.handleUpdateSharedContext((EGLContext) inputMessage.obj);
                    break;
                case MSG_QUIT:
                    Looper.myLooper().quit();
                    break;
                default:
                    throw new RuntimeException("Unhandled msg what=" + what);
            }
        }
    }

    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
        if (nv21 == null || nv12 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        System.arraycopy(nv21, 0, nv12, 0, framesize);
        for (i = 0; i < framesize; i++) {
            nv12[i] = nv21[i];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j - 1] = nv21[j + framesize];
        }
        for (j = 0; j < framesize / 2; j += 2) {
            nv12[framesize + j] = nv21[j + framesize - 1];
        }
    }

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
