# CODM Garena Mod Menu
### Updated for CODM Garena - Fresh Dump 25 July 2026

Based on LGLTeam's CODM ESP & Aimbot Mod Menu, updated for Garena version.

## Changes from Original
- Library target: `libunity.so` (Garena uses libunity, not libil2cpp)
- Namespaces verified from fresh dump.cs (25/07/2026)
- All class/field names confirmed working

## Verified Offsets (from fresh dump 25/07/2026)
| Class | Field | Offset |
|-------|-------|--------|
| BaseWorld | m_Game | 0x18 |
| BaseGame | EnemyPawns | 0x178 |
| Pawn | m_HeadBone | 0x308 |
| Pawn | m_IsAlive | 0x548 |
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
- ESP: Player Line, Box, Health, Name, Distance
- Aimbot: Head/Neck/Body target
- Trigger: Always/Firing/Aiming

## Build
Use GitHub Actions - push to main branch or trigger manually.

## DISCLAIMER
FOR EDUCATIONAL PURPOSES ONLY.
Use at your own risk. Test on guest/alt account first.
