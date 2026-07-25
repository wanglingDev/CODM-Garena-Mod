# CODM Garena Mod Menu
### Updated for CODM Garena - Fresh Dump 25 July 2026
Based on LGLTeam/springmusk026 CODM ESP & Aimbot Mod Menu

## Fixes dari Original Source
1. `libil2cpp.so` → `libunity.so` (CODM Garena pake libunity!)
2. `Il2CppAttach()` → `Il2Cpp::Attach("libunity.so")` (API namespace fix)
3. `Il2CppGetMethodOffset()` → `Il2Cpp::GetMethodOffset()` (API namespace fix)
4. `Il2CppGetFieldOffset()` → `Il2Cpp::GetFieldOffset()` (API namespace fix)
5. `Il2CppGetStaticFieldValue()` → `Il2Cpp::GetStaticFieldValue()` (API namespace fix)
6. `GAME_LIB_ENGINE` → `"libunity.so"` di Il2Cpp.cpp
7. `Pawn::m_PlayerInfo` offset verified = `0x5C0`

## Verified Fields (Fresh Dump 25/07/2026)
| Class | Field | Offset |
|-------|-------|--------|
| BaseWorld | m_Game | 0x18 |
| BaseGame | EnemyPawns | 0x178 |
| Pawn | m_HeadBone | 0x308 |
| Pawn | m_IsAlive | 0x548 |
| Pawn | m_PlayerInfo | 0x5C0 |
| Pawn | m_Mesh | 0x628 |
| AttackableTarget | m_AttackableInfo | 0x18 |
| AttackableTargetInfo | m_Health | 0x34 |
| AttackableTargetInfo | m_MaxHealth | 0x38 |
| PlayerInfo | m_NickName | 0x158 |

## Verified Namespaces
| Class | Namespace |
|-------|-----------|
| BaseWorld | GameEngine |
| BaseGame | GameBase |
| Pawn | GameBase |
| GamePlay | GameEngine |
| AttackableTarget | GameEngine |
| AttackableTargetInfo | GameEngine |
| PlayerInfo | GameEngine |

## Features
- ESP: Player Box, Line, Health Bar, Name, Distance
- Aimbot: Head/Neck/Body
- Trigger Bot

## How to Build
1. Fork repo ini ke GitHub lo
2. Actions → Build → Run workflow
3. Download APK dari artifact

## DISCLAIMER
FOR EDUCATIONAL PURPOSES ONLY.
