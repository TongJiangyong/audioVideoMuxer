package com.serenegiant.Muxer;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by yong on 2019/8/30.
 */

public class EncodedFrame {
    private int mTrackIndex;
    private ByteBuffer encodedData;
    private MediaCodec.BufferInfo mBufferInfo;

    public EncodedFrame(int mTrackIndex, ByteBuffer encodedData, MediaCodec.BufferInfo mBufferInfo) {
        this.mTrackIndex = mTrackIndex;
        this.encodedData = encodedData;
        this.mBufferInfo = mBufferInfo;
    }

    public int getmTrackIndex() {
        return mTrackIndex;
    }

    public void setmTrackIndex(int mTrackIndex) {
        this.mTrackIndex = mTrackIndex;
    }

    public ByteBuffer getEncodedData() {
        return encodedData;
    }

    public void setEncodedData(ByteBuffer encodedData) {
        this.encodedData = encodedData;
    }

    public MediaCodec.BufferInfo getmBufferInfo() {
        return mBufferInfo;
    }

    public void setmBufferInfo(MediaCodec.BufferInfo mBufferInfo) {
        this.mBufferInfo = mBufferInfo;
    }
}
