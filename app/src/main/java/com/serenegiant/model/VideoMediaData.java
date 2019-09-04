package com.serenegiant.model;

import android.opengl.EGLContext;

import com.serenegiant.audiovideosample.ConstantMediaConfig;
import com.serenegiant.utils.LogUtil;


/**
 * Created by yong on 2019/8/31.
 */

public class VideoMediaData {
    public String videoMimeType;


    //encoder
    private int videoFrameRate;
    private float videoBpp;
    private float videoKeyColorFormat;
    private int videoKeyIframe;
    private int videoEncodeWidth;
    private int videoEncodeHeight;
    private int videoEncodeFps;
    private int videoEncodeBitrate;

    //capture
    private int videoCaptureType; //0 texture ;1 byte array
    private int videoCaptureFormat; //0 texture oes ;1 texture2d; 2 yuv420p //3 nv21;
    private int videoCaptureWidth;
    private int videoCaptureHeight;
    private int videoCaptureFps;

    public VideoMediaData() {
        videoMimeType = ConstantMediaConfig.VIDEO_MIME_TYPE;


        //encoder
        videoFrameRate = ConstantMediaConfig.VIDEO_FRAME_RATE;
        videoBpp = ConstantMediaConfig.VIDEO_BPP;
        videoKeyColorFormat = ConstantMediaConfig.VIDEO_KEY_COLOR_FORMAT;
        videoKeyIframe = ConstantMediaConfig.VIDEO_KEY_IFRAME;
        videoEncodeWidth = ConstantMediaConfig.VIDEO_ENCODE_WIDTH;
        videoEncodeHeight = ConstantMediaConfig.VIDEO_ENCODE_HEIGHT;
        videoEncodeBitrate = calcBitRate();

        //capture
        videoCaptureType = ConstantMediaConfig.VIDEO_CAPTURE_TYPE;
        videoCaptureFormat = ConstantMediaConfig.VIDEO_CAPTURE_FORMAT;
        videoCaptureWidth = ConstantMediaConfig.VIDEO_CAPTURE_WIDHT;
        videoCaptureHeight = ConstantMediaConfig.VIDEO_CAPTURE_HEIGHT;
        videoCaptureFps = ConstantMediaConfig.VIDEO_CAPTURE_FPS;
    }


    public String getVideoMimeType() {
        return videoMimeType;
    }

    public void setVideoMimeType(String videoMimeType) {
        this.videoMimeType = videoMimeType;
    }

    public int getVideoFrameRate() {
        return videoFrameRate;
    }

    public void setVideoFrameRate(int videoFrameRate) {
        this.videoFrameRate = videoFrameRate;
    }

    public float getVideoBpp() {
        return videoBpp;
    }

    public void setVideoBpp(float videoBpp) {
        this.videoBpp = videoBpp;
    }

    public float getVideoKeyColorFormat() {
        return videoKeyColorFormat;
    }

    public void setVideoKeyColorFormat(float videoKeyColorFormat) {
        this.videoKeyColorFormat = videoKeyColorFormat;
    }

    public int getVideoKeyIframe() {
        return videoKeyIframe;
    }

    public void setVideoKeyIframe(int videoKeyIframe) {
        this.videoKeyIframe = videoKeyIframe;
    }

    public int getVideoEncodeWidth() {
        return videoEncodeWidth;
    }

    public void setVideoEncodeWidth(int videoEncodeWidth) {
        this.videoEncodeWidth = videoEncodeWidth;
    }

    public int getVideoEncodeHeight() {
        return videoEncodeHeight;
    }

    public void setVideoEncodeHeight(int videoEncodeHeight) {
        this.videoEncodeHeight = videoEncodeHeight;
    }

    public int getVideoCaptureType() {
        return videoCaptureType;
    }

    public void setVideoCaptureType(int videoCaptureType) {
        this.videoCaptureType = videoCaptureType;
    }

    public int getVideoCaptureFormat() {
        return videoCaptureFormat;
    }

    public void setVideoCaptureFormat(int videoCaptureFormat) {
        this.videoCaptureFormat = videoCaptureFormat;
    }

    public int getVideoCaptureWidth() {
        return videoCaptureWidth;
    }

    public void setVideoCaptureWidth(int videoCaptureWidth) {
        this.videoCaptureWidth = videoCaptureWidth;
    }

    public int getVideoCaptureHeight() {
        return videoCaptureHeight;
    }

    public void setVideoCaptureHeight(int videoCaptureHeight) {
        this.videoCaptureHeight = videoCaptureHeight;
    }

    public int getVideoCaptureFps() {
        return videoCaptureFps;
    }

    public void setVideoCaptureFps(int videoCaptureFps) {
        this.videoCaptureFps = videoCaptureFps;
    }

    public int getVideoEncodeFps() {
        return videoEncodeFps;
    }

    public void setVideoEncodeFps(int videoEncodeFps) {
        this.videoEncodeFps = videoEncodeFps;
    }

    private int calcBitRate() {
        final int bitrate = (int) (this.videoBpp * this.videoFrameRate * this.videoEncodeWidth * this.videoEncodeHeight)/2;
        LogUtil.i(String.format("bitrate=%5.2f[Mbps]", bitrate / 1024f / 1024f / 2));
        return bitrate;
    }

    public int getVideoEncodeBitrate() {
        return videoEncodeBitrate;
    }
}
