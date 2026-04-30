# Create Aeronautics: Throwable Rope Connector

Throw rope connectors instead of climbing, bridging, or awkwardly landing just to place one.

Create Aeronautics: Throwable Rope Connector adds a throwable connector item for Create Aeronautics / Create Simulated rope setups. Throw it like a snowball at a valid block face, and it places a normal Create Simulated Rope Connector. Your item then becomes a normal Rope Coupling that is already linked to the placed connector, ready to attach to a Rope Winch.

## Features

- Adds the Throwable Rope Connector item.
- Throws like a snowball and uses the rope connector model.
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
- Create Simulated 1.2.1 or newer
- Create Aeronautics bundle that includes Create Simulated

## Changelog

### 0.1.0

Initial release.

- Added Throwable Rope Connector.
- Added shapeless recipe from Rope Coupling and Rope Connector.
- Added thrown projectile behavior.
- Added automatic linked Rope Coupling return.
- Added same-slot handoff for smoother winch attachment.
- Added configurable throw behavior.
