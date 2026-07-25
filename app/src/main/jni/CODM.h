#pragma once
#include "Includes.h"
#include "Tools.h"
#include "Engine/Canvas.h"
#include "Il2CppType.h"
#include "Substrate/CydiaSubstrate.h"
#include "Vector3.hpp"
#include "Vector2.hpp"
#include "Quaternion.hpp"

// ================================================================
// CODM Garena Mod Menu
// Fresh Dump: 25 July 2026
// Method: Direct RVA (no runtime reflection - libunity.so is stripped)
// ================================================================

// ================================================================
// DIRECT RVA OFFSETS FROM DUMP.CS (25/07/2026)
// All offsets relative to libunity.so base address
// ================================================================
// -- Methods --
#define RVA_Camera_get_main           0x4E09834
#define RVA_Camera_WorldToScreen      0x4E09378
#define RVA_Transform_get_position    0x4E63820
#define RVA_BRGamePlay_get_LocalPawn  0x5B671AC
#define RVA_GamePlay_get_Game         0xC3FE434
#define RVA_BaseGame_GetEnemyPawns    0x73B8D9C
#define RVA_BaseGame_GetAllPawns      0x73B2960
#define RVA_Pawn_SetAimRotation       0x4FAE5DC

// -- Field Offsets --
#define OFFSET_Pawn_m_HeadBone                    0x308
#define OFFSET_Pawn_m_IsAlive                     0x548
#define OFFSET_Pawn_m_PlayerInfo                  0x5C0
#define OFFSET_Pawn_m_Mesh                        0x628
#define OFFSET_AttackableTarget_m_AttackableInfo  0x18
#define OFFSET_AttackableTargetInfo_m_Health      0x34
#define OFFSET_AttackableTargetInfo_m_MaxHealth   0x38
#define OFFSET_PlayerInfo_m_NickName              0x158

// ================================================================
// TYPES
// ================================================================
template<typename T>
using List = Il2CppList<T>;

typedef Il2CppString String;

struct Transform {
    Vector3 get_position() {
        auto fn = (Vector3(*)(Transform*))(g_il2cpp + RVA_Transform_get_position);
        return fn(this);
    }
};

struct Camera {
    static Camera* get_main() {
        auto fn = (Camera*(*)())(g_il2cpp + RVA_Camera_get_main);
        return fn();
    }
    Vector3 WorldToScreenPoint(Vector3 pos) {
        auto fn = (Vector3(*)(Camera*, Vector3))(g_il2cpp + RVA_Camera_WorldToScreen);
        return fn(this, pos);
    }
};

// ================================================================
// GLOBALS
// ================================================================
uintptr_t g_il2cpp = 0;
int g_screenWidth = 0;
int g_screenHeight = 0;
bool bInitDone = false;
std::map<std::string, uintptr_t> Config;

// ================================================================
// HELPER: GET METHOD PTR FROM RVA
// ================================================================
#define METHOD(rva) (g_il2cpp + (rva))

// ================================================================
// WORLDTOSCREEN
// ================================================================
Vector3 WorldToScreen(Vector3 pos) {
    auto cam = Camera::get_main();
    if (cam) {
        return cam->WorldToScreenPoint(pos);
    }
    return {0, 0, 0};
}

// ================================================================
// GET LOCAL PAWN
// ================================================================
uintptr_t GetLocalPawn() {
    auto fn = (uintptr_t(*)())(METHOD(RVA_BRGamePlay_get_LocalPawn));
    return fn();
}

// ================================================================
// GET BASE GAME
// ================================================================
uintptr_t GetBaseGame() {
    auto fn = (uintptr_t(*)())(METHOD(RVA_GamePlay_get_Game));
    return fn();
}

// ================================================================
// GET ENEMY PAWNS
// ================================================================
List<uintptr_t>* GetEnemyPawns() {
    auto baseGame = GetBaseGame();
    if (!baseGame) return nullptr;
    auto fn = (List<uintptr_t>*(*)(uintptr_t))(METHOD(RVA_BaseGame_GetEnemyPawns));
    return fn(baseGame);
}

// ================================================================
// GET PAWN INFO
// ================================================================
bool IsPawnAlive(uintptr_t pawn) {
    if (!pawn) return false;
    return *(bool*)(pawn + OFFSET_Pawn_m_IsAlive);
}

Transform* GetPawnMesh(uintptr_t pawn) {
    if (!pawn) return nullptr;
    return *(Transform**)(pawn + OFFSET_Pawn_m_Mesh);
}

Transform* GetPawnHeadBone(uintptr_t pawn) {
    if (!pawn) return nullptr;
    return *(Transform**)(pawn + OFFSET_Pawn_m_HeadBone);
}

float GetPawnHealth(uintptr_t pawn) {
    if (!pawn) return 0;
    auto attackableTarget = *(uintptr_t*)(pawn + OFFSET_AttackableTarget_m_AttackableInfo);
    if (!attackableTarget) return 0;
    return *(float*)(attackableTarget + OFFSET_AttackableTargetInfo_m_Health);
}

float GetPawnMaxHealth(uintptr_t pawn) {
    if (!pawn) return 100;
    auto attackableTarget = *(uintptr_t*)(pawn + OFFSET_AttackableTarget_m_AttackableInfo);
    if (!attackableTarget) return 100;
    return *(float*)(attackableTarget + OFFSET_AttackableTargetInfo_m_MaxHealth);
}

std::string GetPawnName(uintptr_t pawn) {
    if (!pawn) return "Unknown";
    auto playerInfo = *(uintptr_t*)(pawn + OFFSET_Pawn_m_PlayerInfo);
    if (!playerInfo) return "Unknown";
    auto nameStr = *(String**)(playerInfo + OFFSET_PlayerInfo_m_NickName);
    if (!nameStr) return "Unknown";
    return nameStr->ToString();
}

// ================================================================
// AIMBOT
// ================================================================
bool isInsideFOV(int x, int y) {
    if (!Config["AIM::SIZE"]) return true;
    int cx = g_screenWidth / 2, cy = g_screenHeight / 2;
    int r = Config["AIM::SIZE"];
    return (x - cx) * (x - cx) + (y - cy) * (y - cy) <= r * r;
}

uintptr_t GetClosestTarget() {
    uintptr_t result = 0;
    float maxDist = std::numeric_limits<float>::infinity();
    auto localPawn = GetLocalPawn();
    if (!localPawn) return 0;
    auto myMesh = GetPawnMesh(localPawn);
    if (!myMesh) return 0;
    Vector3 myPos = myMesh->get_position();
    auto enemies = GetEnemyPawns();
    if (!enemies) return 0;
    auto items = enemies->getItems();
    if (!items) return 0;
    for (int i = 0; i < enemies->getSize(); i++) {
        auto pawn = items[i];
        if (!pawn || !IsPawnAlive(pawn) || pawn == localPawn) continue;
        auto mesh = GetPawnMesh(pawn);
        if (!mesh) continue;
        float dist = Vector3::Distance(myPos, mesh->get_position());
        if (dist < maxDist) { result = pawn; maxDist = dist; }
    }
    return result;
}

uintptr_t GetFOVTarget() {
    uintptr_t result = 0;
    float maxDist = std::numeric_limits<float>::infinity();
    auto localPawn = GetLocalPawn();
    if (!localPawn) return 0;
    auto enemies = GetEnemyPawns();
    if (!enemies) return 0;
    auto items = enemies->getItems();
    if (!items) return 0;
    Vector2 center((float)(g_screenWidth / 2), (float)(g_screenHeight / 2));
    for (int i = 0; i < enemies->getSize(); i++) {
        auto pawn = items[i];
        if (!pawn || !IsPawnAlive(pawn) || pawn == localPawn) continue;
        auto head = GetPawnHeadBone(pawn);
        if (!head) continue;
        auto sc = WorldToScreen(head->get_position());
        if (sc.Z <= 0) continue;
        if (!isInsideFOV((int)sc.X, (int)sc.Y)) continue;
        float dist = Vector2::Distance(center, Vector2(sc.X, sc.Y));
        if (dist < maxDist) { result = pawn; maxDist = dist; }
    }
    return result;
}

void DoAimbot(uintptr_t target) {
    if (!target) return;
    uintptr_t bonePtr = 0;
    if (Config["AIM::HEAD"]) bonePtr = (uintptr_t)GetPawnHeadBone(target);
    else bonePtr = (uintptr_t)GetPawnMesh(target);
    if (!bonePtr) return;
    Vector3 targetPos = ((Transform*)bonePtr)->get_position();
    auto localPawn = GetLocalPawn();
    if (!localPawn) return;
    Quaternion rot = Quaternion::LookRotation(targetPos);
    auto fn = (void(*)(uintptr_t, Quaternion))(METHOD(RVA_Pawn_SetAimRotation));
    fn(localPawn, rot);
}

// ================================================================
// ESP DRAW
// ================================================================
void DrawESP(Canvas* canvas) {
    if (!canvas) return;
    auto localPawn = GetLocalPawn();
    if (!localPawn) return;
    auto enemies = GetEnemyPawns();
    if (!enemies) return;
    auto items = enemies->getItems();
    if (!items) return;

    for (int i = 0; i < enemies->getSize(); i++) {
        auto pawn = items[i];
        if (!pawn || !IsPawnAlive(pawn) || pawn == localPawn) continue;

        auto head = GetPawnHeadBone(pawn);
        auto mesh = GetPawnMesh(pawn);
        if (!head || !mesh) continue;

        auto headSc = WorldToScreen(head->get_position());
        auto rootSc = WorldToScreen(mesh->get_position());

        if (headSc.Z <= 0 || rootSc.Z <= 0) continue;

        float height = std::abs(rootSc.Y - headSc.Y);
        float width = height * 0.4f;
        float health = GetPawnHealth(pawn);
        float maxHealth = GetPawnMaxHealth(pawn);
        std::string name = GetPawnName(pawn);
        float dist = headSc.Z;

        // Box ESP
        if (Config["ESP::BOX"]) {
            canvas->drawRect(
                headSc.X - width / 2,
                headSc.Y,
                headSc.X + width / 2,
                rootSc.Y,
                Color::Red, 1.5f
            );
        }

        // Line ESP
        if (Config["ESP::LINE"]) {
            canvas->drawLine(
                g_screenWidth / 2, g_screenHeight,
                headSc.X, headSc.Y,
                Color::Red, 1.5f
            );
        }

        // Health Bar
        if (Config["ESP::HEALTH"]) {
            float ratio = health / (maxHealth > 0 ? maxHealth : 100);
            Color hpColor = ratio > 0.5f ? Color::Green : ratio > 0.25f ? Color::Yellow : Color::Red;
            canvas->drawRect(
                headSc.X - width / 2 - 6,
                headSc.Y + (height * (1.0f - ratio)),
                headSc.X - width / 2 - 3,
                rootSc.Y,
                hpColor, 1.0f
            );
        }

        // Name + Distance
        if (Config["ESP::NAME"]) {
            char info[128];
            snprintf(info, sizeof(info), "%s [%.0fm]", name.c_str(), dist);
            canvas->drawText(info, headSc.X, headSc.Y - 12, Color::White, 11.0f);
        }
    }
}

// ================================================================
// CANVAS DRAW CALLBACK
// ================================================================
void native_onCanvasDraw(JNIEnv* env, jobject obj, jobject canvas,
                          int screenWidth, int screenHeight, float density) {
    static Canvas* m_Canvas = nullptr;
    if (!m_Canvas) {
        m_Canvas = new Canvas(env, screenWidth, screenHeight, density);
    }
    m_Canvas->UpdateCanvas(canvas);
    g_screenWidth = screenWidth;
    g_screenHeight = screenHeight;

    if (!bInitDone || !g_il2cpp) return;

    DrawESP(m_Canvas);

    if (Config["AIM::ON"]) {
        uintptr_t target = Config["AIM::FOV"] ? GetFOVTarget() : GetClosestTarget();
        if (target) DoAimbot(target);
    }
}

// ================================================================
// INIT THREAD
// ================================================================
void* Init_Thread(void*) {
    // Wait for libunity.so to load
    while (!g_il2cpp) {
        g_il2cpp = Tools::GetBaseAddress("libunity.so");
        sleep(1);
    }
    LOGI("libunity.so base: 0x%lx", g_il2cpp);

    // Wait for game to fully init
    sleep(5);

    // Verify key methods are accessible
    auto test_fn = (uintptr_t(*)())(METHOD(RVA_BRGamePlay_get_LocalPawn));
    LOGI("get_LocalPawn ptr: %p", (void*)test_fn);

    bInitDone = true;
    LOGI("CODM Mod initialized! Dump: 25/07/2026");

    // Game loop
    while (true) {
        sleep(1);
    }
    return nullptr;
}

// ================================================================
// NATIVE INIT
// ================================================================
void native_Init(JNIEnv* env, jobject context) {
    // Default configs
    Config["ESP::BOX"]    = 1;
    Config["ESP::LINE"]   = 1;
    Config["ESP::HEALTH"] = 1;
    Config["ESP::NAME"]   = 1;
    Config["AIM::ON"]     = 0;
    Config["AIM::FOV"]    = 1;
    Config["AIM::SIZE"]   = 150;
    Config["AIM::HEAD"]   = 1;

    pthread_t thread;
    pthread_create(&thread, nullptr, Init_Thread, nullptr);
}
