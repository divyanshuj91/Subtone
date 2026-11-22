#include <jni.h>
#include "AudioEngine.h"

static subtone::AudioEngine* g_audio_engine = nullptr;

extern "C" {

JNIEXPORT jboolean JNICALL
Java_dev_subtone_audio_AudioEngine_nativeInit(JNIEnv* env, jobject thiz) {
    if (g_audio_engine == nullptr) {
        g_audio_engine = new subtone::AudioEngine();
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL
Java_dev_subtone_audio_AudioEngine_nativeStart(JNIEnv* env, jobject thiz) {
    if (g_audio_engine == nullptr) {
        g_audio_engine = new subtone::AudioEngine();
    }
    return g_audio_engine->start() ? JNI_TRUE : JNI_FALSE;
}

JNIEXPORT void JNICALL
Java_dev_subtone_audio_AudioEngine_nativeStop(JNIEnv* env, jobject thiz) {
    if (g_audio_engine != nullptr) {
        g_audio_engine->stop();
    }
}

JNIEXPORT jint JNICALL
Java_dev_subtone_audio_AudioEngine_nativeReadInputSamples(
    JNIEnv* env,
    jobject thiz,
    jfloatArray buffer,
    jint offset,
    jint length) {

    if (g_audio_engine == nullptr || !g_audio_engine->isRunning()) {
        return 0;
    }

    jfloat* elements = env->GetFloatArrayElements(buffer, nullptr);
    if (elements == nullptr) return 0;

    size_t readCount = g_audio_engine->readInputSamples(elements + offset, static_cast<size_t>(length));

    env->ReleaseFloatArrayElements(buffer, elements, 0);
    return static_cast<jint>(readCount);
}

JNIEXPORT void JNICALL
Java_dev_subtone_audio_AudioEngine_nativeSetMetronome(
    JNIEnv* env,
    jobject thiz,
    jboolean enabled,
    jint bpm) {

    if (g_audio_engine != nullptr) {
        g_audio_engine->setMetronomeEnabled(enabled == JNI_TRUE);
        g_audio_engine->setMetronomeBpm(bpm);
    }
}

JNIEXPORT void JNICALL
Java_dev_subtone_audio_AudioEngine_nativeSetReferenceTone(
    JNIEnv* env,
    jobject thiz,
    jdouble freqHz,
    jfloat amplitude) {

    if (g_audio_engine != nullptr) {
        if (amplitude > 0.0001f) {
            g_audio_engine->setReferenceTone(freqHz, amplitude);
        } else {
            g_audio_engine->stopReferenceTone();
        }
    }
}

JNIEXPORT void JNICALL
Java_dev_subtone_audio_AudioEngine_nativeDestroy(JNIEnv* env, jobject thiz) {
    if (g_audio_engine != nullptr) {
        g_audio_engine->stop();
        delete g_audio_engine;
        g_audio_engine = nullptr;
    }
}

} // extern "C"
