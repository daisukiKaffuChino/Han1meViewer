#ifndef CHINO_H
#define CHINO_H

#include <jni.h>
#include <cstdint>

#define MY_NR_OPENAT 56
#define MY_NR_READ   63
#define MY_NR_LSEEK  62
#define MY_NR_CLOSE  57

#define XOR_KEY 0x66

#define EXPECTED_SIG_HASH "f28a4e14d2a2c5014be546cb1aff7aaf0bdcf8ea8534a29f9c35c20a4aa8cfe7"

extern "C" {
JNIEXPORT jboolean JNICALL
Java_io_github_daisukikaffuchino_han1meviewer_ui_screen_video_VideoRouteHostScreenKt_svc(
        JNIEnv *env, jclass thiz);

JNIEXPORT jstring JNICALL
Java_io_github_daisukikaffuchino_han1meviewer_ui_screen_video_VideoRouteHostScreenKt_getString(
        JNIEnv *env,
        jclass thiz);
}

#endif // CHINO_H