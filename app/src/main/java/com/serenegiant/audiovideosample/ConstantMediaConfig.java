package com.serenegiant.audiovideosample;

import android.media.AudioFormat;
import android.media.MediaCodecInfo;
import android.opengl.EGLContext;

/**
 * Created by lixiaochen on 2019/9/1.
 */

public class ConstantMediaConfig {
    public final static String VIDEO_MIME_TYPE = "video/avc";


    //video encoder
    public final static int VIDEO_FRAME_RATE = 25;
    public final static float VIDEO_BPP = 0.25f;
    public final static float VIDEO_KEY_COLOR_FORMAT = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
    public final static int VIDEO_KEY_IFRAME = 1;
    public final static int VIDEO_ENCODE_WIDTH = 1280;
    public final static int VIDEO_ENCODE_HEIGHT = 720;

    //video capture
    public final static int VIDEO_CAPTURE_TYPE = 0; //0 texture ;1 byte array
    public final static int VIDEO_CAPTURE_FORMAT = 0; //0 texture oes ;1 texture2d; 2 yuv420p //3 nv21;
    public final static int VIDEO_CAPTURE_WIDHT = 1280;
    public final static int VIDEO_CAPTURE_HEIGHT= 720;
    public final static int VIDEO_CAPTURE_FPS = 15;
    public final static EGLContext VIDEO_CAPTURE_EGL_CONTEXT = null;

    /********************************************************************************/
    //audio
    public final static String AUDIO_MIME_TYPE = "audio/mp4a-latm";
    public final static int AUDIO_AAC_PROFILE = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public final static int AUDIO_KEY_CHANNEL_MASK = AudioFormat.CHANNEL_IN_MONO;
    public final static int AUDIO_KEY_CHANNEL_COUNT = 1;
    //audio encoder
    public final static int AUDIO_SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    public final static int AUDIO_BIT_RATE = 64000;
    public final static int AUDIO_PER_FRAME = 1024;    // AAC, bytes/frame/channel
    public final static int AUDIO_FRAME_BUFFER = 25;    // AAC, frame/buffer/sec

    //audio capture info
    public final static int AUDIO_CHANNEL_FORMAT = AudioFormat.CHANNEL_IN_MONO;    // 声道数 CHANNEL_IN_MONO 1;  CHANNEL_IN_STEREO 2
    public final static int AUDIO_PCM_BIT = AudioFormat.ENCODING_PCM_16BIT;

    //encoder config
    public static final int TIMEOUT_USEC = 10000;    // 10[msec]

}
