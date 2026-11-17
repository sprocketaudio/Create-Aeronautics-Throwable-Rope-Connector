# Create Aeronautics: Throwable Rope Connector

**Create Aeronautics: Throwable Rope Connector** lets you throw, launch and connect ropes from a distance, making airship docking and anchoring much easier.

Whether you're setting up a new landing spot, securing your ship to a cliffside or returning to an established dock, there's a launcher for the job.

## Features

- **Throwable Rope Connector** lets you throw connectors by hand to place anchors without having to reach them.
- **Rope Connector Launcher** is a handheld launcher for placing connectors at longer distances.
- **Mounted Rope Launcher** is an aimable, ship-mounted launcher that fires connectors and establishes rope connections automatically.
- **Reusable docking points** let you leave anchors in place when disconnecting and reconnect on your next visit.
- **Two ammunition types** let you establish new anchors with Throwable Rope Connectors or connect to existing ones with standard Create Simulated Rope Couplings.
- **Redstone automation** lets you fire and disconnect the Mounted Rope Launcher using redstone signals.
- **Optional Jade integration** shows loaded ammunition, quantity and connection status.
- **Configurable behaviour** includes projectile ranges, ammunition consumption and visuals.

Failed shots preserve ammunition by default.

## How It Works

### Creating a New Anchor

Craft a Throwable Rope Connector and throw it by hand or fire it using either launcher.

A successful hand throw or handheld shot places a standard Create Simulated Rope Connector and puts a Rope Coupling in your hand with the new anchor selected. Right-click a Rope Winch or Mounted Rope Launcher with the coupling to finish the connection.

The Mounted Rope Launcher connects automatically when its shot successfully places an anchor.

### Redocking to an Existing Anchor

Already have a Rope Connector at your dock? Load a standard Create Simulated Rope Coupling into either launcher and fire at the existing, unconnected anchor.

- **Handheld launcher.** Hold the ammo in your other hand. A successful shot selects the anchor on the Rope Coupling in that hand. Right-click a Rope Winch or Mounted Rope Launcher with the coupling to finish the connection.
- **Mounted launcher.** A successful shot spends one Rope Coupling and establishes the connection automatically.

### Mounted Launcher Controls

- **Left-click** to fire.
- **Right-click** to disconnect the rope and leave the anchor in place.
- **Hold your Sprint key and right-click** to disconnect and remove the anchor if you have permission.
- **Press your Sneak key** to dismount.

Controls follow your configured key bindings. The mounted help and launcher tooltip show your current bindings.

The red marker accepts a fire pulse using the last saved aim. The amber marker disconnects the rope and leaves the anchor in place. The launcher holds one stack and can be loaded by hand or through item automation.

## Configuration

Throwing, handheld launcher, mounted launcher, general and visual settings are configurable. Projectile ranges are limited by Create Simulated's `max_rope_range`.

The old `mountedAnchorReleaseMode` setting was retired in 0.5.0. Normal release always disconnects the rope and preserves the anchor.

## Requirements

- **Minecraft** 1.21.1
- **NeoForge** 21.1.228 or newer
- **Create** 6.0.10 or newer
- **Create Aeronautics (bundled)** 1.3.0 or newer
- **Sable** as required by your Aeronautics installation

Jade is optional and displays mounted launcher ammo and connection status. FTB Chunks is optional and adds claim protection for anchor placement and removal.

**Tested with** Create 6.0.10, Create Aeronautics 1.3.2, Sable 2.0.6 and NeoForge 21.1.228. The base regression suite also passed with Create Aeronautics 1.3.0 and Sable 2.0.1.

## Development

The 0.5.0 build uses Create 6.0.10, Aeronautics 1.3.2, and Sable 2.0.6 on NeoForge 21.1.228. Put the Aeronautics and Sable jars in `libs/`. The build can read the Create, Jade, and FTB Chunks jars from Fractured Skies or accept their paths through `-PcreateJar`, `-PjadeJar`, and `-PftbChunksJar`. Use `-PtestWithJade` to include Jade in a development run.

Run `./gradlew runGameTestServer` to test docking, aiming, ammo, saving, and placement protection in a dedicated test world. Add `-PtestWithJade -PtestWithClaims` to check Jade's server data and a real FTB Chunks claim. The claim tests also need FTB Teams, FTB Library, and Architectury. The build looks for them in Fractured Skies and accepts overrides through `-PftbTeamsJar`, `-PftbLibraryJar`, and `-ParchitecturyJar`.

## Changelog

See the [published changelogs on CurseForge](https://www.curseforge.com/minecraft/mc-mods/create-aeronautics-throwable-rope-connector/files/all). Select a version to read its release notes.
