#include <jni.h>
#include <malloc.h>
#include <rtmp.h>

#include "../flvmuxer/xiecc_rtmp.h"

#define RTMP_JAVA_PREFIX                                 io_agora //net_butterflytv_rtmp_1client
#define CONCAT1(prefix, class, function)                CONCAT2(prefix, class, function)
#define CONCAT2(prefix, class, function)                Java_ ## prefix ## _ ## class ## _ ## function
#define RTMP_JAVA_INTERFACE(function)                    CONCAT1(RTMP_JAVA_PREFIX, RTMPMuxer, function)

/**
 * if it returns bigger than 0 it is successfull
 */
JNIEXPORT jint JNICALL
RTMP_JAVA_INTERFACE(open)(JNIEnv* env, jobject thiz, jstring url_,
                                                 jint video_width, jint video_height) {
    const char *url = (*env)->GetStringUTFChars(env, url_, NULL);

    int result = rtmp_open_for_write(url, video_width, video_height);

    (*env)->ReleaseStringUTFChars(env, url_, url);
    return result;
}

JNIEXPORT jint JNICALL
RTMP_JAVA_INTERFACE(writeAudio)(JNIEnv* env, jobject thiz, jbyteArray data_,
                                                       jint offset, jint length, jlong timestamp) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    jint result = rtmp_sender_write_audio_frame(&data[offset], length, timestamp, 0);

    (*env)->ReleaseByteArrayElements(env, data_, data, JNI_ABORT);
    return result;
}

JNIEXPORT jint JNICALL
RTMP_JAVA_INTERFACE(writeVideo)(JNIEnv* env, jobject thiz, jbyteArray data_,
                                                       jint offset, jint length, jlong timestamp) {
    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    jint result = rtmp_sender_write_video_frame(&data[offset], length, timestamp, 0, 0);

    (*env)->ReleaseByteArrayElements(env, data_, data, JNI_ABORT);

    return result;
}

JNIEXPORT jint JNICALL
RTMP_JAVA_INTERFACE(close)(JNIEnv* env, jobject thiz) {
    rtmp_close();

    return 0;

}

JNIEXPORT jboolean JNICALL
RTMP_JAVA_INTERFACE(isConnected)(JNIEnv* env, jobject thiz) {
    return rtmp_is_connected() ? true : false;
}

JNIEXPORT jint JNICALL
RTMP_JAVA_INTERFACE(read)(JNIEnv* env, jobject thiz, jbyteArray data_,
                                                 jint offset, jint size) {

    char* data = malloc(size);

    int readCount = rtmp_read_date(data, size);

    if (readCount > 0) {
        (*env)->SetByteArrayRegion(env, data_, offset, readCount, data);  // copy
    }
    free(data);

    return readCount;

}

JNIEXPORT void JNICALL
RTMP_JAVA_INTERFACE(write_1flv_1header)(JNIEnv* env, jobject thiz,
                                                               jboolean is_have_audio, jboolean is_have_video) {
    write_flv_header(is_have_audio, is_have_video);
}

JNIEXPORT void JNICALL
RTMP_JAVA_INTERFACE(file_1open)(JNIEnv* env, jobject thiz, jstring filename) {
    const char *cfilename = (*env)->GetStringUTFChars(env, filename, NULL);

    flv_file_open(cfilename);

    (*env)->ReleaseStringUTFChars(env, filename, cfilename);
}

JNIEXPORT void JNICALL
RTMP_JAVA_INTERFACE(file_1close)(JNIEnv* env, jobject thiz) {

    flv_file_close();

}