package com.serenegiant.Muxer;

import com.serenegiant.connector.SinkConnector;
import com.serenegiant.connector.SrcConnector;
import com.serenegiant.encoder.BaseEncoder;
import com.serenegiant.encoder.MediaEncoderFormat;
import com.serenegiant.encoder.VideoCaptureFrame;

import java.io.IOException;

/**
 * Created by yong on 2019/8/30.
 */

public abstract class BaseMuxer implements SinkConnector<EncodedFrame> {
    protected SrcConnector<EncodedFrame> mEncodedFrameConnector;
    protected BaseMuxer(){
        mEncodedFrameConnector = new SrcConnector<>();
    }

    public SrcConnector<EncodedFrame> getEncodedFrameConnector() {
        return mEncodedFrameConnector;
    }

    @Override
    public abstract int onDataAvailable(EncodedFrame data);



    public abstract  void prepareEncoders() throws IOException;
    public abstract void addEncoder(BaseEncoder mediaEncoder);
    public abstract boolean isMuxerStarted();
    public abstract boolean startMuxer();
    public abstract void stopMuxer();
    public abstract int addTrackToMuxer(MediaEncoderFormat format);
    public abstract void startEncoders();
    public abstract void stopEncoders();
    protected abstract void writeEncodedData(EncodedFrame encodedFrame);
}
