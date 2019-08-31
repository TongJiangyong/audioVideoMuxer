package com.serenegiant.encoder;

import android.media.MediaCodecInfo;

/**
 * Created by yong on 2019/8/31.
 */

public class VideoMediaData {
    public static String videoMimeType = "video/avc";


    //encoder
    public static int videoFrameRate = 25;
    public static float videoBpp = 0.25f;
    public static float videoKeyColorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    public static float videoKeyIframe = 10;
    public static float videoWidth = 1280;
    public static float videoHeight = 720;

    //capture
    public static int captureType = 0; //0 texture ;1 byte array
    public static int captureFormat = 0; //0 texture oes ;1 texture2d; 2 yuv420p //3 nv21;
    public static int captureWidth = 1280;
    public static int captureHeight = 720;
    public static int captureFps = 15;
}
