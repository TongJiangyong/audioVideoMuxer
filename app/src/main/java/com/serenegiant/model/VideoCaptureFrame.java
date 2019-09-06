package com.serenegiant.model;

import android.graphics.SurfaceTexture;
import android.opengl.Matrix;

import java.nio.ByteBuffer;
import java.util.Arrays;

public class VideoCaptureFrame {
    public static final int NO_TEXTURE = -1;
    public static final float[] DEFAULT_MATRIX = new float[16];
    public int mTextureId = NO_TEXTURE;
    public float[] mTexMatrix;
    public float[] mMvpMatrix;
    public int mRotation;
    public long mTimeStamp;
    public byte[] mImage;
    public byte[] rawData;
    public int videoWidth;
    public int videoHeight;
    public SurfaceTexture mSurfaceTexture;
    public boolean mMirror;
    public int mCount;
    public ByteBuffer mBuffer;  //audio
    public int mLength;          //audio

    public VideoCaptureFrame(ByteBuffer buffer, int length, long timeStamp) {
        this.mBuffer = buffer;
        this.mLength = length;
        this.mTimeStamp = timeStamp;
    }

    public VideoCaptureFrame(byte[] rawData, int videoWidth, int videoHeight) {
        this.rawData = rawData;
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
    }

    public VideoCaptureFrame(SurfaceTexture texture, int textureId, float[] mvpMatrix, int count) {
        mCount = count;
        mSurfaceTexture = texture;
        mMvpMatrix = mvpMatrix;
        mTextureId = textureId;
    }

    public VideoCaptureFrame(SurfaceTexture texture, int textureId, byte[] image, float[] matrix, long ts, int rotation, boolean mirror) {
        mTextureId = textureId;
        mImage = image;
        mTimeStamp = ts;
        mRotation = rotation;
        mSurfaceTexture = texture;
        mMirror = mirror;

        if (matrix != null && matrix.length == 16) {
            mTexMatrix = matrix;
        } else {
            mTexMatrix = DEFAULT_MATRIX;
            Matrix.setIdentityM(mTexMatrix, 0);
        }
    }

    @Override
    public String toString() {
        return "VideoCaptureFrame{" +
                ", videoWidth=" + videoWidth +
                ", videoHeight=" + videoHeight +
                ", mSurfaceTexture=" + mSurfaceTexture +
                ", mBuffer=" + mBuffer +
                ", mLength=" + mLength +
                '}';
    }
}