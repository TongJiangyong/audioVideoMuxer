package com.serenegiant.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

/**
 * Created by yong on 2019/8/31.
 */

public class AudioMediaData {

    public  static String audioMimeType = "audio/mp4a-latm";
    public  static int audioAacProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
    public  static int audioKeyChannelMask = AudioFormat.CHANNEL_IN_MONO;
    public  static int audioKeyChannelCount = 1;
    //encoderInfo
    public static int audioSampleRate = 44100;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    public static int audioBitRate = 64000;
    public static int audioPerFrame = 1024;	// AAC, bytes/frame/channel
    public static int audioFrameBuffer = 25; 	// AAC, frame/buffer/sec

    //capture info
    public static int audioChannelCount = AudioFormat.CHANNEL_IN_MONO; 	// 声道数 CHANNEL_IN_MONO 1;  CHANNEL_IN_STEREO 2
    public static int audioPcmBit = AudioFormat.ENCODING_PCM_16BIT;



}
