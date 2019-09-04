package com.serenegiant.encoder;

import com.serenegiant.model.MediaEncoderFormat;

/**
 * Created by yong on 2019/8/30.
 */

public interface IEncoderListener {
    public void onEncoderStopped(BaseEncoder mediaEncoder);

    public void onEncoderPrepared(BaseEncoder mediaEncoder);

    public void onEncoderReleased() throws Exception;

    //should return the index of encoded data
    public int onEncoderOutPutBufferReady(MediaEncoderFormat mediaEncoderFormat) throws Exception;

}
