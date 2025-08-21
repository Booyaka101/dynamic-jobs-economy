# Dynamic Jobs & Economy Pro

[![Build DynamicJobsEconomy](https://github.com/Booyaka101/dynamic-jobs-economy/actions/workflows/build.yml/badge.svg)](https://github.com/Booyaka101/dynamic-jobs-economy/actions/workflows/build.yml)

A Minecraft economy plugin that actually makes sense. Jobs, gigs, and businesses all in one.

## What it does

**Jobs**: 20+ jobs (Miner, Chef, Farmer, Builder, Merchant, and more) with skill progression and dynamic perks. Level up, unlock perks, earn money.

**Gigs**: Players can post freelance jobs for each other. Need someone to build a house? Post a gig. Want to make some extra cash? Accept one.

**Businesses**: Create companies, hire employees, manage finances. Build your empire.

**Works with other plugins**: Vault, WorldGuard, McMMO, LuckPerms. If you don't have them, no problem - everything still works.

## Setup

1. Drop the JAR in your plugins folder
2. Restart server
3. Done! It works out of the box

Note: The built JAR is shaded. Use the JAR from `target/`, e.g. `DynamicJobsEconomy-1.0.5.jar`.

Want to customize? Check `plugins/DynamicJobsEconomy/config.yml`

### Admin Setup (Recommended)

- Vault integration (optional):
  - Install Vault and a compatible economy plugin to use your server-wide economy.
  - In `plugins/DynamicJobsEconomy/config.yml`, ensure `integrations.vault.enabled: true` and `integrations.vault.use_vault_economy: true`.
  - Without Vault, the plugin uses its internal economy.

- Database:
  - Default `sqlite` works out-of-the-box.
  - MySQL example:
    ```yaml
    database:
      type: mysql
      mysql:
        host: localhost
        port: 3306
        database: dynamicjobs
        username: your_user
        password: your_pass
        useSSL: false
    ```

- Permissions:
  - Admin: `djeconomy.admin`
  - Granular: `djeconomy.system.reload`, `djeconomy.admin.economy`, `djeconomy.admin.level.get|set|reset|addxp`, `djeconomy.admin.history.view`, `djeconomy.admin.jobs.refresh|invalidate`
  - Player GUI: `djeconomy.gui.access`

- Command safety for large amounts:
  - Configure in `config.yml`:
    ```yaml
    economy:
      admin_confirmation:
        threshold: 100000.0
        expiry_seconds: 30
    ```
  - Use `/djeconomy confirm` to finalize large economy actions.

- Choose the right JAR:
  - Use the main shaded JAR for most servers: `DynamicJobsEconomy-1.0.5.jar`.
  - Advanced: minimal profiles like `spigot-lite`, `spigot-linux-sqlite`, `spigot-ultra`, `spigot-ultra-mysql` offer smaller JARs with trade-offs.

#### Verify download integrity (checksums)
- CI publishes a `.sha256` file alongside each artifact.
- Linux/macOS:
  ```bash
  sha256sum -c DynamicJobsEconomy-<version>.jar.sha256
  ```
- Windows PowerShell:
  ```powershell
  Get-FileHash .\DynamicJobsEconomy-<version>.jar -Algorithm SHA256
  Get-Content .\DynamicJobsEconomy-<version>.jar.sha256
  ```
  Compare the hash values; they must match.

## Commands

**For players:**
- `/jobs` - See your jobs, join new ones
- `/gigs` - Browse gigs, create your own
- `/business` - Manage your company
- `/business gui` - Open the business GUI
- `/business menu` - Open the business GUI (alias)

**For admins:**
- `/djeconomy reload` - Reload config
- `/djeconomy getlevel <player> <job>` - Show a player's job level (supports offline)
- `/djeconomy setlevel <player> <job> <level>` - Set a player's job level (supports offline)
- `/djeconomy resetlevel <player> <job>` - Reset a player's job level to 1 (supports offline)
- `/djeconomy addxp <player> <job> <amount>` - Add XP to a player's job (online only; player must have joined the job)
- `/djeconomy economy <give|take|set> <player> <amount>` - Manage player money (supports offline)
- `/djeconomy confirm` - Confirm the last pending large economy action
- `/djeconomy history <player> [limit]` - View recent admin economy actions for a player
- `/djeconomy refreshjobs <player>` - Reload a player's job data from DB (online only)
- `/djeconomy invalidatejobs <player>` - Invalidate cached job data (online only)

Note: Large economy amounts at or above the configured threshold require confirmation within the configured expiry window using `/djeconomy confirm`. Configure via `economy.admin_confirmation.threshold` and `economy.admin_confirmation.expiry_seconds` in `config.yml` and apply changes with `/djeconomy reload`.

### Tab Completion

- Admin command tab completion is case-insensitive for both player names and job names.
- Business command includes tab completion for `gui` and `menu`.

### Permissions

- Grant all admin features with `djeconomy.admin` or use granular nodes:
  - `djeconomy.system.reload`, `djeconomy.admin.economy`, `djeconomy.admin.level.get|set|reset|addxp`,
    `djeconomy.admin.history.view`, `djeconomy.admin.jobs.refresh|invalidate`.

- Player GUI access: `djeconomy.gui.access`.

That's it. Everything else is pretty self-explanatory.

## Configuration

The config file is pretty straightforward. Main things you might want to change:

- How much money jobs pay
- How much XP players get
- Which database to use (SQLite works fine for most servers)
- Starting player balance

Everything has sensible defaults, so you probably don't need to touch anything.

## Building

If you want to build from source:
CI builds the project and runs tests automatically. If you build locally, use:
```bash
mvn -B package
```

## Testing

- __Unit tests only__ (fast):
  ```bash
  mvn test
  ```

- __Integration tests (SQLite only)__:
  ```bash
  mvn -P integration-tests test
  ```
  This runs tests tagged `integration` and excludes those tagged `docker`.

- __Integration tests including Docker (MySQL via Testcontainers)__:
  - Windows PowerShell:
    ```powershell
    $env:DJE_DOCKER='true'; mvn -P integration-tests-docker test; Remove-Item Env:DJE_DOCKER
    ```
  The MySQL Testcontainers test is additionally gated by `@EnabledIfEnvironmentVariable(name = "DJE_DOCKER", matches = "true")` to prevent accidental Docker startup on machines without Docker.


## Questions?

If something breaks or you need help, just ask. I actually read messages.

---

**Made by BooPug Studios**  
**Works on Minecraft 1.20.4 - 1.21.x**
