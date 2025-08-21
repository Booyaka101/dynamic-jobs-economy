# 🎉 Dynamic Jobs & Economy Pro v1.0.5 — Polish & Performance Reporting Fix

A small polish release that refines docs and fixes the business performance reporting output.

## ✨ Improvements
- Minor polish across business command help and GUI documentation.

## 🐛 Bug Fixes
- Fixed a critical bug in `ConsolidatedBusinessManager.getBusinessPerformanceReport(...)` where a missing return could lead to incorrect or empty results.

## 📚 Docs
- Updated README/INSTALLATION with JAR guidance and new business GUI commands.

---

## 🔌 Compatibility
- Minecraft: 1.20.4 – 1.21.x
- Integrations: Vault, WorldGuard, McMMO, LuckPerms
- Databases: SQLite, MySQL

## ⬇️ Download
- Use the main JAR: `DynamicJobsEconomy-1.0.5.jar`

## 🆙 How to Update
1. Stop your server
2. Replace the old JAR with `DynamicJobsEconomy-1.0.5.jar`
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
