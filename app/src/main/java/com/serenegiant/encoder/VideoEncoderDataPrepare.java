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

import java.lang.ref.WeakReference;

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

    public VideoEncoderDataPrepare() {
        rawDataConnector = new SrcConnector<>();
    }

    public SrcConnector<VideoCaptureFrame> getRawDataConnector() {
        return rawDataConnector;
    }

    @Override
    public int onDataAvailable(VideoCaptureFrame data) {
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

        Log.d(TAG, "VideoEncoderDataPrepare thread exiting");
        synchronized (mReadyFence) {
            mReady = mRunning = false;
            mHandler = null;
        }
        Log.d(TAG, "VideoEncoderDataPrepare thread exiting over:"+mReady);
    }

    public void startEncoderDataPrepare(Surface encoderInputSurface, VideoMediaData videoMediaData) {
        Log.d(TAG, "startEncoderDataPrepare");
        this.mInputSurface = encoderInputSurface;
        this.videoMediaData = videoMediaData;
        synchronized (mReadyFence) {
            if (mRunning) {
                Log.w(TAG, "Encoder thread already running");
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
    }

    public void stopEncoderDataPrepare() {
        //TODO check whay handler dead
        Log.i(TAG,"stopEncoderDataPrepare");
        if(mHandler!=null){
            mHandler.sendMessage(mHandler.obtainMessage(MSG_RELEASE_EGL_CONTEXT));
            mHandler.sendMessage(mHandler.obtainMessage(MSG_QUIT));
        }

        // We don't know when these will actually finish (or even start).  We don't want to
        // delay the UI thread though, so we return immediately.
    }

    public void frameAvailable(VideoCaptureFrame videoCaptureFrame) {
        synchronized (mReadyFence) {
            if (!mReady) {
                return;
            }
        }
        Log.i("TJY","try to frameAvailable:"+videoCaptureFrame+" mReady:"+mReady);
        mHandler.sendMessage(mHandler.obtainMessage(MSG_FRAME_AVAILABLE, videoCaptureFrame));
    }

    public void updateSharedContext(EGLContext sharedContext) {
        mHandler.sendMessage(mHandler.obtainMessage(MSG_UPDATE_SHARED_CONTEXT, sharedContext));
    }

    public void initEncoderContext(EGLContext eglContext) {
        Log.d(TAG, "initEncoderContext");
        mHandler.sendMessage(mHandler.obtainMessage(MSG_INIT_EGL_CONTEXT, eglContext));
    }


    private void handleInitEncoderContext(EGLContext eglContext) {
        mEglCore = new EglCore(eglContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface = new WindowSurface(mEglCore, this.mInputSurface, true);
        mInputWindowSurface.makeCurrent();
        if (this.videoMediaData.videoCaptureType == 0 && this.videoMediaData.videoCaptureFormat == 0) {
            Log.d(TAG, "ProgramTextureOES");
            programTexture = new ProgramTextureOES();
        } else if (this.videoMediaData.videoCaptureType == 0 && this.videoMediaData.videoCaptureFormat == 1) {
            programTexture = new ProgramTexture2d();
        }
    }

    private void handleUpdateSharedContext(EGLContext newSharedContext) {
        Log.d(TAG, "handleUpdatedSharedContext " + newSharedContext);

        // Release the EGLSurface and EGLContext.
        mInputWindowSurface.releaseEglSurface();
        programTexture.release();
        mEglCore.release();

        // Create a new EGLContext and recreate the window surface.
        mEglCore = new EglCore(newSharedContext, EglCore.FLAG_RECORDABLE);
        mInputWindowSurface.recreate(mEglCore);
        mInputWindowSurface.makeCurrent();

        // Create new programs and such for the new context.
        if (this.videoMediaData.videoCaptureType == 0 && this.videoMediaData.videoCaptureFormat == 0) {
            programTexture = new ProgramTextureOES();
        } else if (this.videoMediaData.videoCaptureType == 0 && this.videoMediaData.videoCaptureFormat == 1) {
            programTexture = new ProgramTexture2d();
        }
    }

    private float[] mMTX = new float[16];
    private float[] mMVP = new float[16];
    private void handleFrameAvailable(VideoCaptureFrame videoCaptureFrame) {
        mTextureId = videoCaptureFrame.mTextureId;
        videoCaptureFrame.mSurfaceTexture.getTransformMatrix(mMTX);
        videoCaptureFrame.mTexMatrix = mMTX;
        if (DEBUG) Log.d(TAG, "handleFrameAvailable videoCaptureFrame" + videoCaptureFrame+ " time:"+videoCaptureFrame.mSurfaceTexture.getTimestamp());
        rawDataConnector.onDataAvailable(null);
        programTexture.drawFrame(mTextureId, videoCaptureFrame.mTexMatrix, videoCaptureFrame.mMvpMatrix);
        mInputWindowSurface.swapBuffers();
    }

    private void handleRleaseEglContext() {
        Log.i(TAG,"handleRleaseEglContext");
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
                Log.w(TAG, "EncoderHandler.handleMessage: encoder is null");
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

}
