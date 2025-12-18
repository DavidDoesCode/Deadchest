# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

DeadChest is a Minecraft Bukkit/Spigot/Paper plugin that preserves player inventory in a chest when they die. The plugin creates a "dead chest" at the death location containing the player's items, with configurable expiration times, permissions, and protections.

## Build System

This is a multi-module Gradle project with two modules:
- `deadchest-core`: Core plugin logic, listeners, database, and utilities
- `deadchest-plugin`: Main plugin entry point and bStats integration

### Common Commands

**Build the project:**
```bash
./gradlew build
```

**Generate plugin JAR:**
```bash
./gradlew shadowJar
```
JAR will be output to `deadchest-plugin/build/libs/dead-chest-SNAPSHOT.jar`

**Run tests:**
```bash
./gradlew test
```

**Run tests for a specific module:**
```bash
./gradlew :deadchest-core:test
```

**Run a specific test class:**
```bash
./gradlew :deadchest-core:test --tests "ClickListenerTest"
```

**Development workflow (auto-copy JAR to server):**
1. Configure `pluginDir=<path_to_server_plugins>` in `gradle.properties`
2. Run: `./gradlew copyJar --continuous`

This automatically rebuilds and copies the JAR to your server's plugin folder on each code change.

## Architecture

### Module Structure

**deadchest-core** contains:
- Event listeners (9 listeners in `listener/` package)
- Database layer using SQLite with WAL mode
- Core business logic and managers
- Configuration management
- Utilities and helpers
- Comprehensive test suite using MockBukkit and JUnit 5

**deadchest-plugin** contains:
- `DeadChest` class: Main plugin entry point extending `JavaPlugin`
- Event registration (done in plugin, not core)
- bStats metrics integration

### Key Design Patterns

**Separation of Concerns:**
- `DeadChest` (plugin module): Bukkit plugin lifecycle, event registration
- `DeadChestLoader` (core): Initialization, configuration, database setup, repeating tasks
- `DeadChestManager` (core): Business logic for chest management (creation, removal, expiration)
- Listeners (core): Individual event handlers for specific Minecraft events

**Data Flow:**
1. Player death triggers `PlayerDeathListener`
2. Creates `ChestData` object with inventory snapshot
3. Spawns chest block and holographic armor stands
4. Stores data in SQLite via `ChestDataRepository`
5. Repeating task (20-tick interval) checks for expired chests
6. Other listeners protect chests from destruction/modification

**Database Architecture:**
- SQLite with WAL (Write-Ahead Logging) mode for concurrent access
- Two repositories: `ChestDataRepository`, `IgnoreItemListRepository`
- Async saves via `SQLExecutor` thread pool
- Legacy migration from YAML-based storage

**Event-Driven Protection:**
Each listener protects dead chests from specific interactions:
- `BlockBreakListener`: Prevents breaking dead chest blocks
- `ExplosionListener`: Protects from explosions
- `PistonListener`: Prevents piston movement
- `ClickListener`: Controls chest opening (owner-only or permissions)
- `InventoryClickListener`: Handles item removal and chest pickup
- etc.

### Important Java Version Handling

The project uses a dual Java version strategy:
- **Production code**: Compiled with Java 8 compatibility (`options.release.set(8)`)
- **Test code**: Compiled with Java 17 (required for MockBukkit)
- Toolchain: Java 17

This maintains compatibility with older Minecraft servers while using modern testing frameworks.

### Key Classes

**ChestData**: Immutable-style data class representing a dead chest
- Stores inventory snapshot, location, owner, timestamps, hologram references
- Contains XP storage if configured

**DeadChestLoader**: Initialization and lifecycle management
- Database initialization
- Configuration registration and migration
- Repeating task setup (runs `handleEvent()` every 20 ticks)
- WorldGuard soft dependency detection

**DeadChestManager**: Static utility methods for chest operations
- `cleanAllDeadChests()`: Remove all active chests
- `generateHologram()`: Create armor stand holograms
- `playerDeadChestAmount()`: Get player's chest count
- Expiration and replacement logic

**Configuration**: `DeadChestConfig` with strongly-typed `ConfigKey` enum
- Auto-update, chest duration, max per player, permissions, etc.
- Localization support via separate YAML file

### Database Details

- Location: `plugins/DeadChest/data.db`
- Two tables: chest data and ignore item list
- PRAGMA settings: WAL mode, NORMAL synchronous, foreign keys enabled
- All saves are asynchronous to avoid blocking game thread

### Testing Approach

Tests use MockBukkit (1.20) for Bukkit API mocking:
- Listener tests verify event handling logic
- Server and player mocking for integration-style tests
- JUnit 5 with parameterized tests
- Mockito for additional mocking needs

When writing tests, always use `MockBukkit.mock()` for server initialization and `MockBukkit.unmock()` in teardown.
