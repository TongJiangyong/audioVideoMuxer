package com.serenegiant.encoder;

import android.opengl.EGLContext;

import com.serenegiant.audiovideosample.ConstantMediaConfig;


/**
 * Created by yong on 2019/8/31.
 */

public class VideoMediaData {
    public String videoMimeType = ConstantMediaConfig.VIDEO_MIME_TYPE;


    //encoder
    public int videoFrameRate = ConstantMediaConfig.VIDEO_FRAME_RATE;
    public float videoBpp = ConstantMediaConfig.VIDEO_BPP;
    public float videoKeyColorFormat = ConstantMediaConfig.VIDEO_KEY_COLOR_FORMAT;
    public float videoKeyIframe = ConstantMediaConfig.VIDEO_KEY_IFRAME;
    public float videoEncodeWidth = ConstantMediaConfig.VIDEO_ENCODE_WIDTH;
    public float videoEncodeHeight = ConstantMediaConfig.VIDEO_ENCODE_HEIGHT;

    //capture
    public int videoCaptureType = ConstantMediaConfig.VIDEO_CAPTURE_TYPE; //0 texture ;1 byte array
    public int videoCaptureFormat = ConstantMediaConfig.VIDEO_CAPTURE_FORMAT; //0 texture oes ;1 texture2d; 2 yuv420p //3 nv21;
    public int videoCaptureWidth = ConstantMediaConfig.VIDEO_CAPTURE_WIDHT;
    public int videoCaptureHeight = ConstantMediaConfig.VIDEO_CAPTURE_HEIGHT;
    public int videoCaptureFps = ConstantMediaConfig.VIDEO_CAPTURE_FPS;

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

    public float getVideoKeyIframe() {
        return videoKeyIframe;
    }

    public void setVideoKeyIframe(float videoKeyIframe) {
        this.videoKeyIframe = videoKeyIframe;
    }

    public float getVideoEncodeWidth() {
        return videoEncodeWidth;
    }

    public void setVideoEncodeWidth(float videoEncodeWidth) {
        this.videoEncodeWidth = videoEncodeWidth;
    }

    public float getVideoEncodeHeight() {
        return videoEncodeHeight;
    }

    public void setVideoEncodeHeight(float videoEncodeHeight) {
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

}
