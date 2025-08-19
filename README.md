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

Want to customize? Check `plugins/DynamicJobsEconomy/config.yml`

## Commands

**For players:**
- `/jobs` - See your jobs, join new ones
- `/gigs` - Browse gigs, create your own
- `/business` - Manage your company

**For admins:**
- `/djeconomy reload` - Reload config
- `/djeconomy economy <give|take|set> <player> <amount>` - Manage player money
- `/djeconomy setlevel <player> <job> <level>` - Set a player's job level (supports offline). Job name is case-insensitive.
- `/djeconomy addxp <player> <job> <amount>` - Add XP to a player's job (online only; player must have joined the job). Job name is case-insensitive.
- `/djeconomy refreshjobs <player>` - Reload a player's job data from DB (online only)
- `/djeconomy invalidatejobs <player>` - Invalidate cached job data (online only)

### Tab Completion

- Admin command tab completion is case-insensitive for both player names and job names.

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

## Questions?

If something breaks or you need help, just ask. I actually read messages.

---

**Made by BooPug Studios**  
**Works on Minecraft 1.20.4 - 1.21.x**
