# Create Aeronautics: Throwable Rope Connector

Throw rope connectors instead of climbing, bridging, or awkwardly landing just to place one.

Create Aeronautics: Throwable Rope Connector adds three ways to place rope connections for Create Aeronautics / Create Simulated setups: throw a connector by hand, fire one from the Rope Connector Launcher, or use the Mounted Rope Launcher for ship-mounted anchoring. Successful shots place a normal Create Simulated Rope Connector and give you a normal Rope Coupling that is already linked to it, ready to attach to a Rope Winch.

## Features

- Adds the Throwable Rope Connector item.
- Adds the Rope Connector Launcher item.
- Adds the Mounted Rope Launcher block.
- Throws like a snowball and uses the rope connector model.
- Launcher fires Throwable Rope Connectors from the other hand at longer range.
- Mounted launcher lets a player mount, aim, fire, and release rope connections directly from the block.
- Places a normal Create Simulated Rope Connector on valid block faces.
- Returns a normal Create Simulated Rope Coupling after successful placement.
- Returned Rope Coupling is already linked to the placed connector.
- Returned Rope Coupling appears in the same slot the throwable connector was used from.
- Failed shots do not consume ammo by default.
- Configurable throw, launcher, and mounted launcher range.
- Configurable throw speed, particles, projectile rope trail, and failure messages.
- Works server-side for multiplayer validation.

## How To Use

### Throwable Rope Connector

1. Craft a Throwable Rope Connector.
2. Aim at a valid block face.
3. Right-click to throw it.
4. If it lands successfully, it places a Rope Connector.
5. The item in your original slot becomes a linked Rope Coupling.
6. Right-click a Rope Winch with that Rope Coupling to finish the connection.

### Rope Connector Launcher

1. Load Throwable Rope Connectors into your inventory.
2. Hold the launcher and keep a Throwable Rope Connector in the other hand.
3. Fire at a valid block face to place a connector at longer range.
4. On success, the ammo becomes a linked Rope Coupling ready for the winch.

### Mounted Rope Launcher

1. Place the Mounted Rope Launcher.
2. Load it with Throwable Rope Connectors.
3. Right-click to mount it.
4. Aim the turret and left-click to fire.
5. Right-click while mounted to release the current rope.
6. Sneak to dismount.

## Config

```toml
[throwing]
maxThrowDistance = 10.0
throwVelocity = 1.5

[launcher]
launcherMaxDistance = 20.0

[mounted]
mountedLauncherRange = 40.0
removePlacedConnectorOnRelease = true

[general]
consumeOnSuccessOnly = true
playParticles = true
showFailureMessages = true

[visual]
showProjectileRopeTrail = true
```

Range values cannot exceed Create Simulated's `max_rope_range`. If they are set higher, they are corrected back down to that limit.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.227 or newer
- Create Aeronautics bundled 1.2.1 or newer

## Changelog

### 0.3.0

- Added the Mounted Rope Launcher block.
- Added mountable aiming, firing, and rope release controls for the mounted launcher.
- Updated default range settings to `10` for throwing, `20` for the handheld launcher, and `40` for the mounted launcher.
- Added range correction against Create Simulated's `max_rope_range`.
- Cleaned up and reorganized the config layout.
- Added embedded mod icon support and config-menu compatibility improvements.

### 0.2.2

- Fixed a full-inventory edge case where returning a prepared Rope Coupling after a successful throw or launcher shot could consume the rest of a stacked Throwable Rope Connector pile.
- Adjusted the fallback Rope Coupling drop so overflow returns land close to the player without being flung far away.

### 0.2.1

- Fixed dedicated-server dependency metadata to require the Create Aeronautics bundle instead of standalone Create Simulated.

### 0.2.0

- Added Rope Connector Launcher for longer-range connector placement.
- Added Create-style launcher visuals, recoil, cog animation, and launch sound.
- Added projectile rope trail visuals for thrown and launched connectors.
- Updated Throwable Rope Connector icon so it no longer looks identical to a normal connector.
- Changed launcher recipe to Create mechanical crafting, based on the Potato Cannon recipe with polished cut asurine slabs.
- Launcher can now be fired from either hand, using a Throwable Rope Connector in the other hand.
- Fixed main-hand throwable connectors blocking offhand launcher use.
- Improved returned Rope Coupling placement for stacked connectors and full inventories.
- Tuned launcher rope trail origin and slack so it better matches the launcher barrel.

### 0.1.0

Initial release.

- Added Throwable Rope Connector.
- Added shapeless recipe from Rope Coupling and Rope Connector.
- Added thrown projectile behavior.
- Added automatic linked Rope Coupling return.
- Added same-slot handoff for smoother winch attachment.
- Added configurable throw behavior.
