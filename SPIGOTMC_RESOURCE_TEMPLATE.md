# 🎉 Dynamic Jobs & Economy Pro v1.0.5 — Polish & Performance Reporting Fix

A small polish release that refines docs and fixes the business performance reporting output.

## ✨ Improvements
- Admin economy confirmation now uses GUI reason selection. When a large action is initiated, admins select a reason in the Admin Economy GUI (or Skip Reason); it's stored and included in the history upon confirmation.
- History command tab completion suggests `[page]` and `[size]` for easier navigation.
- Minor polish across business command help and GUI documentation.

## 🐛 Bug Fixes
- Fixed a critical bug in `ConsolidatedBusinessManager.getBusinessPerformanceReport(...)` where a missing return could lead to incorrect or empty results.

## 📚 Docs
- Updated README/INSTALLATION with JAR guidance and new business GUI commands.
- Documented GUI-only admin confirmation reason selection, updated `/djeconomy history <player> [page] [size]`, and aligned permissions.

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
  - On large actions the Admin Economy GUI will prompt you to select a reason (or Skip Reason); it's stored and shown in history on confirm.
- History command:
  - `/djeconomy history <player> [page] [size]`
  - Tab completion suggests common pages and sizes

## 🛂 Permissions
- Player GUI access: `djeconomy.gui.access`
- Admin: `djeconomy.admin`
- Granular: `djeconomy.system.reload`, `djeconomy.system.doctor`, `djeconomy.admin.businessinfo`, `djeconomy.admin.economy`, `djeconomy.admin.level.get|set|reset|addxp`, `djeconomy.admin.history.view`, `djeconomy.admin.jobs.refresh|invalidate`
- Admin Economy GUI: `djeconomy.gui.admin.economy`, `djeconomy.gui.admin.economy.balance.view`, `djeconomy.gui.admin.economy.balance.modify`, `djeconomy.gui.admin.economy.history.view`, `djeconomy.gui.admin.economy.confirm.manage`
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
