package com.serenegiant.audio_capture;

import com.serenegiant.connector.SrcConnector;
import com.serenegiant.model.VideoCaptureFrame;

/**
 * Created by lixiaochen on 2019/9/1.
 */

public abstract class BaseAudioCapture {
    protected SrcConnector<VideoCaptureFrame> mCaptureDataConnector;

    public BaseAudioCapture() {
        mCaptureDataConnector = new SrcConnector<>();
    }

    public SrcConnector<VideoCaptureFrame> getCaptureDataConnector() {
        return mCaptureDataConnector;
    }

    //start audio record
    public abstract void startAudioCapture();

    //stop audio record
    public abstract void stopAudioCapture();
}
