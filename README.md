# Dynamic Jobs & Economy Pro

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
- `/djeconomy give <player> <amount>` - Give money
- `/djeconomy reset <player>` - Reset someone's data

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
```bash
mvn clean package
```

## Questions?

If something breaks or you need help, just ask. I actually read messages.

---

**Made by BooPug Studios**  
**Works on Minecraft 1.20.4 - 1.21.x**
