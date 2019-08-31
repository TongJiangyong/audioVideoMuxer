package com.serenegiant.encoder;

import com.serenegiant.muxer.EncodedFrame;
import com.serenegiant.connector.SinkConnector;
import com.serenegiant.connector.SrcConnector;

/**
 * Created by yong on 2019/8/30.
 */

public abstract class BaseEncoder implements SinkConnector<VideoCaptureFrame> {
    public IEncoderListener mEncoderListener;
    protected SrcConnector<EncodedFrame> mEncoderedDataConnector;
    public BaseEncoder(IEncoderListener iEncoderListener){
        mEncoderedDataConnector =  new SrcConnector<>();
        this.mEncoderListener =  iEncoderListener;
    }
    public SrcConnector<EncodedFrame> getEncoderedDataConnector() {
        return mEncoderedDataConnector;
    }

    @Override
    public abstract int onDataAvailable(VideoCaptureFrame data);

}
