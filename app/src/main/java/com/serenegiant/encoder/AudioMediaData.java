package com.serenegiant.encoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.serenegiant.audiovideosample.ConstantMediaConfig;

/**
 * Created by yong on 2019/8/31.
 */

public class AudioMediaData {

    public String audioMimeType = ConstantMediaConfig.AUDIO_MIME_TYPE;
    public int audioAacProfile = ConstantMediaConfig.AUDIO_AAC_PROFILE;
    public int audioKeyChannelMask = ConstantMediaConfig.AUDIO_KEY_CHANNEL_MASK;
    public int audioKeyChannelCount = ConstantMediaConfig.AUDIO_KEY_CHANNEL_COUNT;
    //encoderInfo
    public int audioSampleRate = ConstantMediaConfig.AUDIO_SAMPLE_RATE;	// 44.1[KHz] is only setting guaranteed to be available on all devices.
    public int audioBitRate = ConstantMediaConfig.AUDIO_BIT_RATE;
    public int audioPerFrame = ConstantMediaConfig.AUDIO_PER_FRAME;	// AAC, bytes/frame/channel
    public int audioFrameBuffer = ConstantMediaConfig.AUDIO_FRAME_BUFFER; 	// AAC, frame/buffer/sec

    //capture info
    public int audioChannelCount = ConstantMediaConfig.AUDIO_CHANNEL_COUNT; 	// 声道数 CHANNEL_IN_MONO 1;  CHANNEL_IN_STEREO 2
    public int audioPcmBit = ConstantMediaConfig.AUDIO_PCM_BIT;


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
}
