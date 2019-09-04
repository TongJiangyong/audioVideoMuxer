package com.serenegiant.encoder;

import com.serenegiant.model.EncodedFrame;
import com.serenegiant.connector.SinkConnector;
import com.serenegiant.connector.SrcConnector;
import com.serenegiant.model.VideoCaptureFrame;

import java.io.IOException;

/**
 * Created by yong on 2019/8/30.
 */

public abstract class BaseEncoder implements SinkConnector<VideoCaptureFrame> {
    public IEncoderListener mEncoderListener;
    protected SrcConnector<EncodedFrame> mEncoderedDataConnector;

    public BaseEncoder(IEncoderListener iEncoderListener) {
        mEncoderedDataConnector = new SrcConnector<>();
        this.mEncoderListener = iEncoderListener;
    }

    public SrcConnector<EncodedFrame> getEncoderedDataConnector() {
        return mEncoderedDataConnector;
    }

    @Override
    public abstract int onDataAvailable(VideoCaptureFrame data);

    //prepare encoder configuration
    public abstract void prepare() throws IOException;

    //release encoder
    public abstract void release();

    //start encoder
    public abstract void startRecording();

    //stop encoder
    public abstract void stopRecording();

    //get an output buffer from encoder
    public abstract void drain();

    //notify encoder an new input buffer will come
    public abstract boolean frameAvailableSoon();


}
