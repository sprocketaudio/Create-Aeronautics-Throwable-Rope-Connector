# Create Aeronautics: Throwable Rope Connector

Place rope connectors from a distance instead of awkwardly climbing over to every docking point.

Create Aeronautics: Throwable Rope Connector adds three ways to place rope connections for Create Aeronautics / Create Simulated setups:

- throw a connector by hand
- fire one from the Rope Connector Launcher
- use the Mounted Rope Launcher for ship-mounted anchoring

Successful throws and shots place a normal Create Simulated Rope Connector and give you a normal Rope Coupling that is already linked to it, ready to attach to a Rope Winch.

## Features

- Throwable Rope Connector item for hand-thrown placement.
- Rope Connector Launcher for longer-range placement.
- Mounted Rope Launcher for ship-mounted firing and anchoring.
- Successful placement returns a linked Rope Coupling ready for a Rope Winch.
- Failed shots do not consume ammo by default.
- Configurable throw, launcher, mounted, general, and visual settings.
- Optional redstone automation for the Mounted Rope Launcher.

## How To Use

1. Craft a Throwable Rope Connector.
2. Aim at a valid block face and throw it by hand, fire it from the launcher, or use the mounted launcher.
3. If it lands successfully, it places a Rope Connector.
4. The used connector becomes a linked Rope Coupling.
5. Right-click a Rope Winch with that Rope Coupling to finish the connection.

## Config

Throwing, launcher, mounted, general, and visual settings are configurable in the generated config file.

Range values cannot exceed Create Simulated's `max_rope_range`.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Create Aeronautics bundled 1.3.0 or newer

## Changelog

### 0.4.0

### Added

- Added redstone automation for the Mounted Rope Launcher using side-specific inputs.
- Added red and orange side markers to the Mounted Rope Launcher to show the redstone fire and release sides.
- Added config options to disable mounted launcher redstone firing and to disable remote release.
- Added mounted launcher tooltip/help improvements, including config-aware help text and red/orange side marker guidance.

### Fixed

- Fixed mounted launcher compatibility with the newer Simulated rope API used by Aeronautics 1.3.x.
- Improved mounted launcher rope trail rendering and automated shot origin visuals.

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
