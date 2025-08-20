# 🎉 Dynamic Jobs & Economy Pro v1.0.4 — Admin Economy Fixes & i18n

A quality-of-life update focused on safer admin economy actions, better i18n consistency, and faster access to the Business GUI.

## ✨ Improvements
- Prefix precedence in admin messages now respects `config.yml` first, then `messages.yml`, then defaults; reloaded on `/djeconomy reload`.
- Added `/business gui` and `/business menu` helpers to open the business GUI faster, with tab completion. (perm: `djeconomy.gui.access`)

## 🐛 Bug Fixes
- Offline player deposits now correctly use `EconomyManager.depositPlayer(...)` for offline targets.
- Confirmation threshold/expiry now read with defaults using the correct `FileConfiguration#get*` overloads.
- Added guard for missing economy manager with a friendly `admin.economy_unavailable` message.
- Aligned i18n keys and placeholders across give/take/set, confirm prompts, expiry, offline flow, and reload success.

## 📚 Docs
- Updated README/INSTALLATION with JAR guidance and new business GUI commands.

---

## 🔌 Compatibility
- Minecraft: 1.20.4 – 1.21.x
- Integrations: Vault, WorldGuard, McMMO, LuckPerms
- Databases: SQLite, MySQL, MongoDB

## ⬇️ Download
- Use the main JAR: `DynamicJobsEconomy-1.0.4.jar`

## 🆙 How to Update
1. Stop your server
2. Replace the old JAR with `DynamicJobsEconomy-1.0.4.jar`
3. Start your server
4. (Optional) `/djeconomy reload`

## 🧭 Quick Highlights
- Business GUI access:
  - `/business gui` — open GUI (perm: `djeconomy.gui.access`)
  - `/business menu` — alias
- Admin economy safety:
  - Large amounts require confirmation using `/djeconomy confirm`
  - Configure thresholds via `economy.admin_confirmation.threshold`
  - Configure expiry via `economy.admin_confirmation.expiry_seconds`

## 🛂 Permissions
- Player GUI access: `djeconomy.gui.access`
- (See `permissions.yml` for full list)

## 🛠️ Admin Setup (Vault, DB, Permissions)

- Vault (optional, recommended):
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
  - MongoDB example:
    ```yaml
    database:
      type: mongodb
      mongodb:
        connection_string: "mongodb://localhost:27017/dynamicjobs"
    ```

- Permissions:
  - Admin: `djeconomy.admin`
  - Granular: `djeconomy.system.reload`, `djeconomy.admin.economy`, `djeconomy.admin.level.get|set|reset|addxp`, `djeconomy.admin.history.view`, `djeconomy.admin.jobs.refresh|invalidate`
  - Player GUI: `djeconomy.gui.access`

- Command safety (large amounts):
  - In `config.yml`:
    ```yaml
    economy:
      admin_confirmation:
        threshold: 100000.0
        expiry_seconds: 30
    ```
  - Use `/djeconomy confirm` to finalize large economy actions.

## 🆘 Need Help?
- Read: `README.md`, `INSTALLATION.md`, `QUICK_START.md`, `USER_MANUAL.md`
- Have questions? Start a discussion or reach out via your usual support channel.
