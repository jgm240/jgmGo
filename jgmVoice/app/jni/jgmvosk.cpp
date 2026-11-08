// Bridge between Java and the Vosk speech library (libvosk.so is loaded at run time, so nothing is linked at build time).
#include <jni.h>
#include <dlfcn.h>
#include <string>

typedef void* (*model_new_t)(const char*);
typedef void (*model_free_t)(void*);
typedef void* (*rec_new_t)(void*, float);
typedef void (*rec_free_t)(void*);
typedef int (*accept_t)(void*, const char*, int);
typedef const char* (*text_t)(void*);
typedef void (*loglevel_t)(int);

static void* lib = nullptr;
static model_new_t p_model_new; static model_free_t p_model_free; static rec_new_t p_rec_new; static rec_free_t p_rec_free;
static accept_t p_accept; static text_t p_result, p_final, p_partial;

static bool ensure() {
    if (lib) return true;
    lib = dlopen("libvosk.so", RTLD_NOW);
    if (!lib) lib = dlopen("/system/lib/libvosk.so", RTLD_NOW);
    if (!lib) return false;
    p_model_new = (model_new_t) dlsym(lib, "vosk_model_new"); p_model_free = (model_free_t) dlsym(lib, "vosk_model_free");
    p_rec_new = (rec_new_t) dlsym(lib, "vosk_recognizer_new"); p_rec_free = (rec_free_t) dlsym(lib, "vosk_recognizer_free");
    p_accept = (accept_t) dlsym(lib, "vosk_recognizer_accept_waveform");
    p_result = (text_t) dlsym(lib, "vosk_recognizer_result"); p_final = (text_t) dlsym(lib, "vosk_recognizer_final_result"); p_partial = (text_t) dlsym(lib, "vosk_recognizer_partial_result");
    loglevel_t ll = (loglevel_t) dlsym(lib, "vosk_set_log_level"); if (ll) ll(-1);
    return p_model_new && p_rec_new && p_accept && p_result && p_final && p_partial;
}

extern "C" {
JNIEXPORT jlong JNICALL Java_org_jgmgo_voice_Vosk_nativeLoad(JNIEnv* e, jclass, jstring path) {
    if (!ensure()) return 0;
    const char* p = e->GetStringUTFChars(path, nullptr);
    void* m = p_model_new(p);
    e->ReleaseStringUTFChars(path, p);
    return (jlong) m;
}
JNIEXPORT jlong JNICALL Java_org_jgmgo_voice_Vosk_nativeRecognizer(JNIEnv*, jclass, jlong model, jfloat rate) { return (jlong) p_rec_new((void*) model, rate); }
JNIEXPORT jint JNICALL Java_org_jgmgo_voice_Vosk_nativeAccept(JNIEnv* e, jclass, jlong rec, jbyteArray data, jint len) {
    jbyte* b = e->GetByteArrayElements(data, nullptr);
    int r = p_accept((void*) rec, (const char*) b, len);
    e->ReleaseByteArrayElements(data, b, JNI_ABORT);
    return r;
}
JNIEXPORT jstring JNICALL Java_org_jgmgo_voice_Vosk_nativeResult(JNIEnv* e, jclass, jlong rec) { return e->NewStringUTF(p_result((void*) rec)); }
JNIEXPORT jstring JNICALL Java_org_jgmgo_voice_Vosk_nativeFinal(JNIEnv* e, jclass, jlong rec) { return e->NewStringUTF(p_final((void*) rec)); }
JNIEXPORT jstring JNICALL Java_org_jgmgo_voice_Vosk_nativePartial(JNIEnv* e, jclass, jlong rec) { return e->NewStringUTF(p_partial((void*) rec)); }
JNIEXPORT void JNICALL Java_org_jgmgo_voice_Vosk_nativeFree(JNIEnv*, jclass, jlong rec, jlong model) { if (rec) p_rec_free((void*) rec); if (model) p_model_free((void*) model); }
}
