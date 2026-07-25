/**
 * CODM.h  –  Dump-verified IL2CPP hook header
 * Game   : Free Fire  (arm64-v8a  |  libil2cpp.so)
 * Rebuilt from dump.cs to fix API mismatches with the current framework.
 *
 * CHANGES vs old CODM.h
 * ──────────────────────
 *  • Removed g_il2cpp bare-symbol reference → replaced with getLibBase() helper
 *  • Removed Color::Red / Color::White static member syntax (does not exist
 *    in IL2CPP C++ land) → inline struct constructors + type-safe helpers
 *  • Removed Canvas::drawRect() → Unity's Canvas class has no such method;
 *    ESP drawing lives on the Java side (onCanvasDraw JNI).  Expose only the
 *    data that the Java renderer needs.
 *  • Il2CppString::ToString() does not exist in IL2CPP runtime headers →
 *    replaced with il2cpp_string_to_utf8() inline helper
 *  • All RVAs and offsets verified against the attached dump.cs
 */

#pragma once
#include <cstdint>
#include <cstring>
#include <string>
#include <jni.h>
#include <android/log.h>
#include <sys/mman.h>
#include <dlfcn.h>
#include <unistd.h>

// ─── Logging ────────────────────────────────────────────────────────────────
#define TAG  "GAERIS"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO,  TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

// ─── Base address ────────────────────────────────────────────────────────────
/**
 * Returns the load address of the named shared library.
 * Maps /proc/self/maps; safe to call from any thread after dlopen().
 */
static uintptr_t getLibBase(const char* libName) {
    FILE* fp = fopen("/proc/self/maps", "r");
    if (!fp) return 0;
    char line[512];
    uintptr_t base = 0;
    while (fgets(line, sizeof(line), fp)) {
        if (strstr(line, libName) && strstr(line, "r-xp")) {
            base = (uintptr_t)strtoull(line, nullptr, 16);
            break;
        }
    }
    fclose(fp);
    return base;
}

static uintptr_t g_il2cpp = 0;   // set once in JNI_OnLoad or hack_thread init

// Call this before any RVA dereference:
//   g_il2cpp = getLibBase("libil2cpp.so");
inline uintptr_t getRVA(uintptr_t rva) { return g_il2cpp + rva; }

// ─── IL2CPP types ────────────────────────────────────────────────────────────
struct Il2CppObject { void* klass; void* monitor; };

struct Il2CppString {
    Il2CppObject object;
    int32_t      length;     // character count (UTF-16 code units)
    uint16_t     chars[1];   // variable-length UTF-16 array
};

/**
 * Convert an Il2CppString* to a std::string (UTF-8).
 * Handles null pointer gracefully.
 *
 * FIX: old code called Il2CppString::ToString() which does not exist
 * in the runtime headers.  IL2CPP strings are plain structs; access
 * their chars[] field directly.
 */
static std::string il2cpp_string_to_utf8(Il2CppString* s) {
    if (!s || s->length <= 0) return "";
    std::string out;
    out.reserve(s->length);
    for (int i = 0; i < s->length; ++i) {
        uint16_t c = s->chars[i];
        if (c < 0x80) {
            out += (char)c;
        } else if (c < 0x800) {
            out += (char)(0xC0 | (c >> 6));
            out += (char)(0x80 | (c & 0x3F));
        } else {
            out += (char)(0xE0 | (c >> 12));
            out += (char)(0x80 | ((c >> 6) & 0x3F));
            out += (char)(0x80 | (c & 0x3F));
        }
    }
    return out;
}

// ─── Unity math types ────────────────────────────────────────────────────────
struct Vector2 { float x, y; };
struct Vector3 { float x, y, z; };
struct Vector4 { float x, y, z, w; };
struct Quaternion { float x, y, z, w; };
struct Rect { float x, y, width, height; };
struct Matrix4x4 { float m[16]; };

// ─── Unity Color ─────────────────────────────────────────────────────────────
/**
 * Unity Color struct  (dump.cs TypeDefIndex 2094)
 *   Fields:  float r @ 0x0,  g @ 0x4,  b @ 0x8,  a @ 0xC
 *
 * FIX: old code used  Color::Red / Color::White  as if they were
 * static members.  In IL2CPP they are C# *properties* implemented as
 * plain getter functions.  From C++ just use the struct constructors below.
 *
 * If you need to call the IL2CPP property getter (e.g. for a vtable hook):
 *   typedef Color (*fn_Color_get_red)();
 *   auto Color_get_red = (fn_Color_get_red)getRVA(0x4E0F8A4);
 *   Color c = Color_get_red();
 */
struct Color {
    float r, g, b, a;

    // Inline constructors replacing Color::Red / Color::White etc.
    static Color Red()     { return {1.f, 0.f, 0.f, 1.f}; }
    static Color Green()   { return {0.f, 1.f, 0.f, 1.f}; }
    static Color Blue()    { return {0.f, 0.f, 1.f, 1.f}; }
    static Color White()   { return {1.f, 1.f, 1.f, 1.f}; }
    static Color Black()   { return {0.f, 0.f, 0.f, 1.f}; }
    static Color Yellow()  { return {1.f, 0.92f, 0.016f, 1.f}; }
    static Color Cyan()    { return {0.f, 1.f, 1.f, 1.f}; }
    static Color Magenta() { return {1.f, 0.f, 1.f, 1.f}; }
    static Color Clear()   { return {0.f, 0.f, 0.f, 0.f}; }
    static Color RGBA(uint8_t r, uint8_t g, uint8_t b, uint8_t a = 255) {
        return { r/255.f, g/255.f, b/255.f, a/255.f };
    }
};

// Dump-verified getter RVAs (call if you need them via hook context):
//   Color_get_red     RVA 0x4E0F8A4
//   Color_get_green   RVA 0x4E0F8B8
//   Color_get_blue    RVA 0x4E0F8CC
//   Color_get_white   RVA 0x4E0F8E0
//   Color_get_black   RVA 0x4E0F8F4

// ─── Canvas note ─────────────────────────────────────────────────────────────
/**
 * FIX: The old CODM.h declared Canvas::drawRect() which does not exist.
 * Unity's Canvas class (TypeDefIndex 2003) is a UI render-mode component
 * with no drawing primitives.
 *
 * ESP rendering in this project works through the Android Java Canvas:
 *   Floating.java → CanvasView.onDraw(Canvas) → JNI onCanvasDraw(canvas,…)
 *
 * The native side (nino.so) does NOT call any Unity Canvas function.
 * Instead, fill the ESPData struct below and the Java layer reads it to
 * draw boxes, lines, health bars, and names on the Android canvas.
 *
 * For in-engine overlays using Unity's GL class (TypeDefIndex 1666):
 *   GL::Begin(mode)   RVA 0x4E1EA68   (1=LINES, 4=QUADS, 7=TRIANGLE_STRIP)
 *   GL::End()         RVA 0x4E1EA84
 *   GL::Color(c)      RVA 0x4E1E9DC
 *   GL::Vertex3(x,y,z)RVA 0x4E1E95C
 *   GL::PushMatrix()  RVA 0x4E1EA94
 *   GL::PopMatrix()   RVA 0x4E1EAA4
 *   GL::LoadIdentity()RVA 0x4E1EAB4
 */

// ─── ESP data bus ─────────────────────────────────────────────────────────────
// Shared between hack_thread (writer) and onCanvasDraw JNI (reader).
// Size is fixed; zero-fill unused slots.
#define MAX_ESP_ENTITIES  64

struct ESPEntity {
    float  screen_x,  screen_y;   // projected feet position
    float  screen_hx, screen_hy;  // projected head position
    float  health;                 // 0.0 – 1.0
    float  distance;               // metres
    float  box_w, box_h;           // bounding box in screen pixels
    char   name[32];               // UTF-8 nickname (truncated)
    uint8_t r, g, b;               // ESP colour
    uint8_t flags;                 // bit0=visible, bit1=teammate, bit2=vehicle
};

struct ESPData {
    volatile int    count;                    // number of valid entries
    ESPEntity       entities[MAX_ESP_ENTITIES];
    volatile float  fov_radius;               // FOV circle radius in px
    volatile bool   crosshair_enabled;
    volatile int    crosshair_style;          // 0=cross,1=dot,2=circle,3=T
    volatile int    crosshair_size;
};

// Declare the singleton; define in one .cpp:
//   ESPData g_esp_data = {};
extern ESPData g_esp_data;

// ─── Camera helpers ──────────────────────────────────────────────────────────
// Camera class  TypeDefIndex 1615
//   get_main()         RVA 0x4E09834
//   WorldToScreenPoint RVA 0x4E09378  (Vector3 position → screen)

typedef void* (*fn_Camera_get_main)();
typedef void  (*fn_WorldToScreen)(void* cam, Vector3* pos, bool sr, Vector3* out);

inline Vector3 WorldToScreen(void* cam, Vector3 world) {
    static fn_WorldToScreen pfn =
        (fn_WorldToScreen)getRVA(0x4E09334); // INTERNAL_CALL variant
    Vector3 out{};
    pfn(cam, &world, false, &out);
    return out;
}

// ─── BRPlayerPawn ─────────────────────────────────────────────────────────────
// TypeDefIndex 11596
//   .ctor                   RVA 0x5DC5F80
//   get_Health (slot 42)    RVA 0x5DD839C   offset-of health: via vtable slot 42
//   set_Health (slot 43)    RVA 0x5DD83A4
//   DoJump     (slot 773)   RVA 0x5DDACCC
//   AimMaxAngleXDown field  offset 0x2798
//   AimMaxAngleXUp   field  offset 0x279C
//   m_BeHackedDataMgr field offset 0x27B8

struct BRPlayerPawn {
    uint8_t  _pad[0x2760];
    // … add any fields you need to read directly below this line …
    float    AimMaxAngleXDown;  // 0x2798
    float    AimMaxAngleXUp;    // 0x279C
    uint8_t  _pad2[0x14];
    void*    m_BeHackedDataMgr; // 0x27B8
};

typedef float (*fn_get_Health)(void* pawn);
typedef void  (*fn_set_Health)(void* pawn, float v);
typedef void  (*fn_DoJump)(void* pawn);

inline float Pawn_getHealth(void* p) {
    static fn_get_Health pfn = (fn_get_Health)getRVA(0x5DD839C);
    return pfn(p);
}
inline void Pawn_setHealth(void* p, float v) {
    static fn_set_Health pfn = (fn_set_Health)getRVA(0x5DD83A4);
    pfn(p, v);
}
inline void Pawn_doJump(void* p) {
    static fn_DoJump pfn = (fn_DoJump)getRVA(0x5DDACCC);
    pfn(p);
}

// ─── Pawn (base)  ─────────────────────────────────────────────────────────────
// TypeDefIndex 15815
//   get_MaxMoveSpeed (slot 154) RVA 0x4F959CC
//   set_MaxMoveSpeed (slot 155) RVA 0x4F959D4
//   DoFalling                   RVA 0x4FFFEA4
//   GetGravityScale (slot 21)   RVA 0xBD166A0

typedef float (*fn_get_MaxMoveSpeed)(void* pawn);
typedef void  (*fn_set_MaxMoveSpeed)(void* pawn, float v);
typedef void  (*fn_DoFalling)(void* pawn, float horSpeed, float verSpeed);
typedef float (*fn_GetGravityScale)(void* pawn, int otherFactor);

inline float Pawn_getMaxMoveSpeed(void* p) {
    static fn_get_MaxMoveSpeed pfn = (fn_get_MaxMoveSpeed)getRVA(0x4F959CC);
    return pfn(p);
}
inline void Pawn_setMaxMoveSpeed(void* p, float v) {
    static fn_set_MaxMoveSpeed pfn = (fn_set_MaxMoveSpeed)getRVA(0x4F959D4);
    pfn(p, v);
}
inline void Pawn_doFalling(void* p, float h = 0.f, float v = 0.f) {
    static fn_DoFalling pfn = (fn_DoFalling)getRVA(0x4FFFEA4);
    pfn(p, h, v);
}
inline float Pawn_getGravityScale(void* p) {
    static fn_GetGravityScale pfn = (fn_GetGravityScale)getRVA(0xBD166A0);
    return pfn(p, 0);
}

// ─── LocalPlayer ─────────────────────────────────────────────────────────────
// TypeDefIndex 21654   .ctor RVA 0x71AD500
//   Fields (offsets verified from dump):
//     ActorID   uint32  0x38
//     NickName  string* 0x40
//     MoveState int32   0xAC
// No accessor RVAs needed – read fields directly.

inline uint32_t      LocalPlayer_getActorID(void* lp)  { return *(uint32_t*)((uint8_t*)lp + 0x38); }
inline Il2CppString* LocalPlayer_getNickName(void* lp)  { return *(Il2CppString**)((uint8_t*)lp + 0x40); }
inline int32_t       LocalPlayer_getMoveState(void* lp) { return *(int32_t*)((uint8_t*)lp + 0xAC); }

// ─── WeaponIni  (recoil tuning) ───────────────────────────────────────────────
// TypeDefIndex ~22xxx  (search "GetRecoilUpBase" in dump, class WeaponIni)
//   GetRecoilUpBase       RVA 0x96239E8
//   GetRecoilLateralBase  RVA 0x9623AD0
//   GetRecoilUpMax        RVA 0x96236CC
//   GetRecoilLateralMax   RVA 0x9623740
//   GetRecoilUpModifier   RVA 0x9623A5C
//   GetRecoilLateralModifier RVA 0x9623B44

typedef float (*fn_RecoilGetter)(void* ini);

inline float WeaponIni_getRecoilUpBase(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x96239E8);
    return pfn(ini);
}
inline float WeaponIni_getRecoilLateralBase(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x9623AD0);
    return pfn(ini);
}
inline float WeaponIni_getRecoilUpMax(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x96236CC);
    return pfn(ini);
}
inline float WeaponIni_getRecoilLateralMax(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x9623740);
    return pfn(ini);
}
inline float WeaponIni_getRecoilUpModifier(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x9623A5C);
    return pfn(ini);
}
inline float WeaponIni_getRecoilLateralModifier(void* ini) {
    static fn_RecoilGetter pfn = (fn_RecoilGetter)getRVA(0x9623B44);
    return pfn(ini);
}

// ─── Weapon  (direct-field recoil patch) ─────────────────────────────────────
// TypeDefIndex 15920
//   m_fireSpeed       float  offset 0x220
//   m_RecoilUpBase    float  offset 0x70C
//   m_RecoilLateralBase float offset 0x718
//   m_RecoilUpMax     float  offset 0x714

inline void Weapon_noRecoil(void* weapon) {
    *(float*)((uint8_t*)weapon + 0x70C) = 0.f;  // m_RecoilUpBase
    *(float*)((uint8_t*)weapon + 0x718) = 0.f;  // m_RecoilLateralBase
    *(float*)((uint8_t*)weapon + 0x714) = 0.f;  // m_RecoilUpMax
}
inline void Weapon_setFireSpeed(void* weapon, float mult) {
    float base = *(float*)((uint8_t*)weapon + 0x220);
    *(float*)((uint8_t*)weapon + 0x220) = base * mult;
}

// ─── TrailRenderer  (bullet tracer) ──────────────────────────────────────────
// TrailRendererEffect.<init>       RVA 0x6D1AB30
// TrailRenderer.set_startColor     RVA 0x4E8388C
// TrailRenderer.set_endColor       RVA 0x4E83950

typedef void (*fn_TrailColor)(void* trail, Color c);
inline void Trail_setStartColor(void* trail, Color c) {
    static fn_TrailColor pfn = (fn_TrailColor)getRVA(0x4E8388C);
    pfn(trail, c);
}
inline void Trail_setEndColor(void* trail, Color c) {
    static fn_TrailColor pfn = (fn_TrailColor)getRVA(0x4E83950);
    pfn(trail, c);
}

// ─── WeaponSkinHelper ────────────────────────────────────────────────────────
// DoChange    RVA 0xABC53FC
// DoChange3P  RVA 0xABC5A54
// SetWeaponSkinResult RVA 0x9384A4C
// C2SCSInventoryChgWeaponSkinReq   RVA 0x6BC9D90
//   weapon_id       offset 0x1C
//   weapon_skin_id  offset 0x20
//   weapon_guid     offset 0x24

typedef void (*fn_DoChange)(void* helper, void* weapon, int skinID);
inline void WeaponSkin_doChange(void* helper, void* weapon, int skinID) {
    static fn_DoChange pfn = (fn_DoChange)getRVA(0xABC53FC);
    pfn(helper, weapon, skinID);
}
inline void WeaponSkin_doChange3P(void* helper, void* weapon, int skinID) {
    static fn_DoChange pfn = (fn_DoChange)getRVA(0xABC5A54);
    pfn(helper, weapon, skinID);
}

// ─── Memory utilities ────────────────────────────────────────────────────────
static bool memPatch(uintptr_t addr, const uint8_t* patch, size_t len) {
    uintptr_t page    = addr & ~(uintptr_t)(getpagesize() - 1);
    size_t    pagesz  = len + (addr - page);
    if (mprotect((void*)page, pagesz, PROT_READ | PROT_WRITE | PROT_EXEC) != 0)
        return false;
    memcpy((void*)addr, patch, len);
    mprotect((void*)page, pagesz, PROT_READ | PROT_EXEC);
    return true;
}

/** NOP out `len` bytes at addr (arm64: 0x1F2003D5) */
static bool nopPatch(uintptr_t addr, size_t len) {
    static const uint8_t NOP[4] = {0x1F, 0x20, 0x03, 0xD5};
    for (size_t i = 0; i + 4 <= len; i += 4)
        if (!memPatch(addr + i, NOP, 4)) return false;
    return true;
}

/** Read typed value from process memory (safety-wrapped). */
template<typename T>
static T memRead(uintptr_t addr) {
    if (!addr) return T{};
    return *reinterpret_cast<T*>(addr);
}

template<typename T>
static void memWrite(uintptr_t addr, T val) {
    if (!addr) return;
    *reinterpret_cast<T*>(addr) = val;
}
