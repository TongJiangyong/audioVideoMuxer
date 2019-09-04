package com.serenegiant.model;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.serenegiant.audiovideosample.ConstantMediaConfig;

import java.security.PrivateKey;

import static android.media.AudioFormat.CHANNEL_IN_MONO;
import static android.media.AudioFormat.CHANNEL_IN_STEREO;

/**
 * Created by yong on 2019/8/31.
 */

public class AudioMediaData {
    private String audioMimeType;
    private int audioAacProfile;
    private int audioKeyChannelMask;
    private int audioKeyChannelCount;
    //encoderInfo
    private int audioSampleRate;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private int audioBitRate;
    private int audioPerFrame;    // AAC, bytes/frame/channel
    private int audioFrameBuffer;    // AAC, frame/buffer/sec

    //capture info
    private int audioChannelCount;    // 声道数 CHANNEL_IN_MONO 1;  CHANNEL_IN_STEREO 2



    private int audioChannelFormat;    // 声道数 CHANNEL_IN_MONO 1;  CHANNEL_IN_STEREO 2
    private int audioPcmBit;

    public AudioMediaData() {
        audioMimeType = ConstantMediaConfig.AUDIO_MIME_TYPE;
        audioAacProfile = ConstantMediaConfig.AUDIO_AAC_PROFILE;
        audioKeyChannelMask = ConstantMediaConfig.AUDIO_KEY_CHANNEL_MASK;
        audioKeyChannelCount = ConstantMediaConfig.AUDIO_KEY_CHANNEL_COUNT;
        audioSampleRate = ConstantMediaConfig.AUDIO_SAMPLE_RATE;
        audioBitRate = ConstantMediaConfig.AUDIO_BIT_RATE;
        audioPerFrame = ConstantMediaConfig.AUDIO_PER_FRAME;
        audioFrameBuffer = ConstantMediaConfig.AUDIO_FRAME_BUFFER;
        audioChannelFormat = ConstantMediaConfig.AUDIO_CHANNEL_FORMAT;
        audioChannelCount = countAudioChannelCount();
        audioPcmBit = ConstantMediaConfig.AUDIO_PCM_BIT;

    }


    public String getAudioMimeType() {
        return audioMimeType;
    }

    public void setAudioMimeType(String audioMimeType) {
        this.audioMimeType = audioMimeType;
    }

    public int getAudioAacProfile() {
        return audioAacProfile;
    }

    public void setAudioAacProfile(int audioAacProfile) {
        this.audioAacProfile = audioAacProfile;
    }

    public int getAudioKeyChannelMask() {
        return audioKeyChannelMask;
    }

    public void setAudioKeyChannelMask(int audioKeyChannelMask) {
        this.audioKeyChannelMask = audioKeyChannelMask;
    }

    public int getAudioKeyChannelCount() {
        return audioKeyChannelCount;
    }

    public void setAudioKeyChannelCount(int audioKeyChannelCount) {
        this.audioKeyChannelCount = audioKeyChannelCount;
    }

    public int getAudioSampleRate() {
        return audioSampleRate;
    }

    public void setAudioSampleRate(int audioSampleRate) {
        this.audioSampleRate = audioSampleRate;
    }

    public int getAudioBitRate() {
        return audioBitRate;
    }

    public void setAudioBitRate(int audioBitRate) {
        this.audioBitRate = audioBitRate;
    }

    public int getAudioPerFrame() {
        return audioPerFrame;
    }

    public void setAudioPerFrame(int audioPerFrame) {
        this.audioPerFrame = audioPerFrame;
    }

    public int getAudioFrameBuffer() {
        return audioFrameBuffer;
    }

    public void setAudioFrameBuffer(int audioFrameBuffer) {
        this.audioFrameBuffer = audioFrameBuffer;
    }

    public int getAudioChannelCount() {
        return audioChannelCount;
    }

    public void setAudioChannelCount(int audioChannelCount) {
        this.audioChannelCount = audioChannelCount;
    }

    public int getAudioPcmBit() {
        return audioPcmBit;
    }

    public void setAudioPcmBit(int audioPcmBit) {
        this.audioPcmBit = audioPcmBit;
    }

    public int getAudioChannelFormat() {
        return audioChannelFormat;
    }
    private int countAudioChannelCount() {
        switch (audioChannelFormat){
            case  CHANNEL_IN_MONO :
                return 1;
            case CHANNEL_IN_STEREO:
                return 2;
            default:
                return 1;
        }
    }

}
