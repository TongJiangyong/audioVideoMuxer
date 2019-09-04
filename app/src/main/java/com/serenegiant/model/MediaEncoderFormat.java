package com.serenegiant.model;

import android.media.MediaFormat;

/**
 * Created by yong on 2019/8/30.
 */

public class MediaEncoderFormat {
    public enum CodecType{
        AUDIO,VIDEO
    }
    private MediaFormat mediaFormat;

    public MediaEncoderFormat(MediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
    }

    public MediaFormat getMediaFormat() {
        return mediaFormat;
    }

    public void setMediaFormat(MediaFormat mediaFormat) {
        this.mediaFormat = mediaFormat;
    }
}
