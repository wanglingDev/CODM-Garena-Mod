#include "Includes.h"
#include "Il2Cpp.h"
#include "CODM.h"

// ─── Globals ──────────────────────────────────────────────────────────────────

bool titleValid = false;

// Fix #1 + #2: Config must be a string-keyed map, not a bare identifier.
// native_onSendConfig was indexing with a const char* which is never a valid
// array subscript; std::map<std::string, unsigned long> solves both issues at once.
// unsigned long used instead of u_long — strtoul returns unsigned long and
// u_long is not guaranteed available in Android Bionic without sys/types.h.
static std::map<std::string, unsigned long> Config;

// Fix #7: ESPData is declared extern in CODM.h — define the singleton here.
ESPData g_esp_data = {};

// ─── JNI exports (non-native-method style) ───────────────────────────────────

extern "C" {
JNIEXPORT jstring JNICALL
Java_com_gotoubun_Floating_Title(JNIEnv *env, jobject thiz) {
    titleValid = true;
    return env->NewStringUTF("LOL DONT USE THIS PROJECT NOOB");
}
} // extern "C"

// ─── Native method implementations ───────────────────────────────────────────

// Receives a key/value string pair from Java and stores it in Config.
void native_onSendConfig(JNIEnv *env, jobject thiz, jstring s, jstring v) {
    const char *config = env->GetStringUTFChars(s, 0);
    const char *value  = env->GetStringUTFChars(v, 0);

    // Fix #1/#2: use std::string key so map lookup compiles and works correctly.
    Config[std::string(config)] = (unsigned long) strtoul(value, 0, 0);

    env->ReleaseStringUTFChars(s, config);
    env->ReleaseStringUTFChars(v, value);
}

// Fix #3: native_onCanvasDraw was referenced in Register1's method table but
// never defined.  JNI descriptor: (Landroid/graphics/Canvas;IIF)V
// → canvas=jobject, entityCount=jint, flags=jint, scale=jfloat
void native_onCanvasDraw(JNIEnv *env, jobject thiz,
                         jobject canvas,
                         jint    entityCount,
                         jint    flags,
                         jfloat  scale) {
    // Guard: nothing to draw if the hack thread hasn't started yet.
    if (!titleValid) return;

    // The Java CanvasView reads g_esp_data directly via JNI field mirrors;
    // this callback exists so the native side can do any per-frame prep
    // (e.g. locking, clearing stale slots) before the Java renderer walks
    // the entity list.
    //
    // Clamp entity count to what g_esp_data actually holds.
    if (entityCount > MAX_ESP_ENTITIES)
        entityCount = MAX_ESP_ENTITIES;

    g_esp_data.count = entityCount;

    // Additional per-frame work (FOV circle, crosshair sync, etc.) goes here.
    // The Java layer polls g_esp_data fields; no explicit return value needed.
    (void)canvas; (void)flags; (void)scale;
}

// Fix #4: native_Init was referenced in Register2's method table but never
// defined.  JNI descriptor: (Landroid/content/Context;)V
void native_Init(JNIEnv *env, jobject thiz, jobject context) {
    // Resolve libil2cpp.so base address once — all getRVA() calls depend on it.
    g_il2cpp = getLibBase("libil2cpp.so");
    if (!g_il2cpp) {
        LOGE("native_Init: could not locate libil2cpp.so");
        return;
    }

    // Attach the Il2Cpp namespace resolver (walks the export table).
    Il2Cpp::Attach("libil2cpp.so");

    LOGI("native_Init: il2cpp base = 0x%" PRIxPTR, g_il2cpp);

    // Place feature hooks / hack_thread spawn here.
    // Tools::GetPackageName(env, context) is available if you need the
    // package string for any signature check.
}

// ─── JNI method registration ─────────────────────────────────────────────────

// Fix #5 (cascade): sizeof(methods) was "incomplete type" because
// native_onCanvasDraw was undeclared; now that it's defined above, the
// initialiser list is complete and sizeof resolves correctly.
int Register1(JNIEnv *env) {
    JNINativeMethod methods[] = {
        {"onSendConfig", "(Ljava/lang/String;Ljava/lang/String;)V", (void *) native_onSendConfig},
        {"onCanvasDraw", "(Landroid/graphics/Canvas;IIF)V",         (void *) native_onCanvasDraw}
    };

    jclass clazz = env->FindClass("com/gotoubun/Floating");
    if (!clazz) return -1;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return -1;
    return 0;
}

// Fix #6 (cascade): same sizeof issue for native_Init — resolved above.
int Register2(JNIEnv *env) {
    JNINativeMethod methods[] = {
        {"Init", "(Landroid/content/Context;)V", (void *) native_Init}
    };

    jclass clazz = env->FindClass("com/gotoubun/Launcher");
    if (!clazz) return -1;
    if (env->RegisterNatives(clazz, methods, sizeof(methods) / sizeof(methods[0])) != 0)
        return -1;
    return 0;
}

// ─── Library lifecycle ────────────────────────────────────────────────────────

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    JNIEnv *env;
    vm->GetEnv((void **) &env, JNI_VERSION_1_6);

    if (Register1(env) != 0) return -1;
    if (Register2(env) != 0) return -1;

    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved) {
    // Cleanup if needed.
}
