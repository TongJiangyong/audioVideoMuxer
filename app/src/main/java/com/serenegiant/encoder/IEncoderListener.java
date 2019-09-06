package com.serenegiant.encoder;

import com.serenegiant.model.MediaEncoderFormat;

/**
 * Created by yong on 2019/8/30.
 */

public interface IEncoderListener {
    //encoder is init ready and you should pass egl context when use surface input
    public void onEncoderPrepared(BaseEncoder mediaEncoder);

    //encoder is ready to accept input buffer ,and should return the index of encoded data
    //the index is necessary in AndroidMediaMuxer for the MediaMuxer,but useless in RTMPMuxer
    public int onEncoderOutPutBufferReady(MediaEncoderFormat mediaEncoderFormat) throws Exception;

    //encoder is stopped
    public void onEncoderStopped(BaseEncoder mediaEncoder);

    //encoder is released
    public void onEncoderReleased() throws Exception;


}
