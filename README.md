# Create Aeronautics: Throwable Rope Connector

Throw rope connectors instead of climbing, bridging, or awkwardly landing just to place one.

Create Aeronautics: Throwable Rope Connector adds a throwable connector item for Create Aeronautics / Create Simulated rope setups. Throw it like a snowball at a valid block face, and it places a normal Create Simulated Rope Connector. Your item then becomes a normal Rope Coupling that is already linked to the placed connector, ready to attach to a Rope Winch.

## Features

- Adds the Throwable Rope Connector item.
- Adds the Rope Connector Launcher item.
- Throws like a snowball and uses the rope connector model.
- Launcher fires Throwable Rope Connectors from the other hand at longer range.
- Places a normal Create Simulated Rope Connector on valid block faces.
- Returns a normal Create Simulated Rope Coupling after successful placement.
- Returned Rope Coupling is already linked to the placed connector.
- Returned Rope Coupling appears in the same slot the throwable connector was used from.
- Failed throws do not consume the item by default.
- Configurable throw distance, throw speed, cooldown, particles, and failure messages.
- Works server-side for multiplayer validation.

## How To Use

1. Craft a Throwable Rope Connector.
2. Aim at a valid block face.
3. Right-click to throw it.
4. If it lands successfully, it places a Rope Connector.
5. The item in your original slot becomes a linked Rope Coupling.
6. Right-click a Rope Winch with that Rope Coupling to finish the connection.

## Recipe

Shapeless crafting:

- 1 Create Simulated Rope Coupling
- 1 Create Simulated Rope Connector

Result:

- 1 Throwable Rope Connector

## Config

```toml
[throwable_rope_connector]
maxThrowDistance = 20.0
throwVelocity = 1.5
consumeOnSuccessOnly = true
cooldownTicks = 10
playParticles = true
showFailureMessages = true
```

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.228 or newer
- Create Aeronautics bundled 1.2.1 or newer

## Changelog

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
