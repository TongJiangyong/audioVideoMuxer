package com.serenegiant.Muxer;

import android.media.MediaCodec;

import java.nio.ByteBuffer;

/**
 * Created by yong on 2019/8/30.
 */

public class EncodedFrame {
    private int mTrackIndex;
    //0为音频 1为视频
    private int codecType;
    private ByteBuffer encodedByteBuffer;
    private MediaCodec.BufferInfo mBufferInfo;

    public EncodedFrame(int codecType, int mTrackIndex, ByteBuffer encodedByteBuffer, MediaCodec.BufferInfo mBufferInfo) {
        this.mTrackIndex = mTrackIndex;
        this.encodedByteBuffer = encodedByteBuffer;
        this.mBufferInfo = mBufferInfo;
        this.codecType = codecType;
    }

    public int getmTrackIndex() {
        return mTrackIndex;
    }

    public void setmTrackIndex(int mTrackIndex) {
        this.mTrackIndex = mTrackIndex;
    }

    public ByteBuffer getEncodedByteBuffer() {
        return encodedByteBuffer;
    }

    public void setEncodedByteBuffer(ByteBuffer encodedByteBuffer) {
        this.encodedByteBuffer = encodedByteBuffer;
    }

    public MediaCodec.BufferInfo getmBufferInfo() {
        return mBufferInfo;
    }

    public void setmBufferInfo(MediaCodec.BufferInfo mBufferInfo) {
        this.mBufferInfo = mBufferInfo;
    }

    public int getCodecType() {
        return codecType;
    }

    public void setCodecType(int codecType) {
        this.codecType = codecType;
    }

    @Override
    public String toString() {
        return "EncodedFrame{" +
                "mTrackIndex=" + mTrackIndex +
                ", codecType=" + codecType +
                ", encodedByteBuffer=" + encodedByteBuffer +
                ", mBufferInfo=" + mBufferInfo +
                '}';
    }
}
