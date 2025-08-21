# 🚀 Future Release Roadmap – Dynamic Jobs & Economy Pro

Current released version: 1.0.4

Use this file during planning/release sessions. Check items off as we implement them and link PRs/commits.

## ✅ How to use this file
- Mark tasks with [x] when completed and add PR/commit links.
- Keep IDs in parentheses so we can cross-reference with issue tracker/notes.
- Group by target version; move items forward/back as priorities change.

---

## 🌟 Near-term (target: 1.0.5)
- [ ] Implement "/djeconomy doctor" self-check with actionable tips (id: plan_doctor)
  - Validate config keys, DB connectivity, Vault presence, permissions sanity
  - Output clear pass/warn/fail with suggestions
  - Touchpoints: `src/main/resources/config.yml`, `plugin.yml` (new command), DB service, integrations
- [ ] CI build matrix + artifacts + JAR size guard (id: plan_ci_matrix)
  - Build profiles: default, `spigot-lite`, `spigot-ultra`, `spigot-ultra-mysql`, `spigot-linux-sqlite`
  - Upload artifacts, enforce max-size thresholds, publish checksums
  - Touchpoints: `.github/workflows/build.yml`, `pom.xml`
- [ ] Performance pass (async + caching + batching) (id: plan_perf)
  - Async DB operations, cache hot player/job data, batch writes, safe flush on shutdown
  - Add lightweight timings/diagnostics with debug toggle
- [ ] Admin Economy GUI with confirm flows (id: plan_guis)
  - Give/Take/Set with confirmation, reason logging, history viewer
  - Touchpoints: command handlers, GUI classes, `messages.yml`

### ✅ 1.0.5 Acceptance Criteria
- plan_doctor
  - `/djeconomy doctor` prints plugin version, Java version, server brand
  - Validates keys: `database.type`, `economy.admin_confirmation.threshold`, `economy.admin_confirmation.expiry_seconds`, `integrations.vault.enabled`
  - If Vault integration enabled, checks Vault plugin and economy provider presence
  - Database ping based on `database.type`:
    - sqlite: file accessible + open connection
    - mysql: TCP connect + auth test with timeout
    - mongodb: URI connect test
  - Outputs clear pass/warn/fail with suggestions; logs to console when run by console
- plan_ci_matrix
  - CI builds the following profiles: default, `spigot-lite`, `spigot-ultra`, `spigot-ultra-mysql`, `spigot-linux-sqlite`
  - Each profile uploads a separate artifact with profile in the name
  - CI logs built JAR size per profile
  - Size guard step exists and is configurable via `MAX_JAR_SIZE` (bytes); disabled when unset
- plan_guis (Admin Economy GUI)
  - Admin-only GUI gated by `djeconomy.gui.admin`
  - Supports Give/Take/Set with confirmation flow for large amounts and optional reason capture
  - Writes to existing admin history log and respects confirmation threshold/expiry from config
- plan_perf
  - Async DB for admin economy operations on main paths
  - Simple cache for hot player/job data with safe flush on shutdown
  - Lightweight timings/diagnostics behind debug toggle

---

## 📈 Mid-term (target: 1.1)
- [ ] Database schema versioning and migrations (id: plan_db_migration)
  - `schema_version` table, migration runner, safe roll-forward
- [ ] Economy balancing toolkit (id: plan_econ_balance)
  - Money sinks (taxes/fees), inflation guard, per-world/job multipliers
  - Extended audit trail for large transactions and `/djeconomy history` filters
- [ ] Jobs GUI for players (id: plan_guis)
  - Browse/join/leave jobs, perks, earnings; complements `/business gui`
- [ ] Expand i18n coverage and language pack template (id: plan_i18n)
  - Externalize any remaining strings; provide `messages_[lang].yml` templates

---

## 🧭 Longer-term (target: 1.2+)
- [ ] Public plugin API & events (id: plan_api)
  - Events: JobLevelUp, MoneyTransaction, BusinessHire, etc.; Services API
- [ ] Deeper integrations (id: plan_integrations)
  - WorldGuard region modifiers (job multipliers), McMMO synergy, ShopGUIPlus hooks
- [ ] Optional bStats telemetry (opt-in) (id: plan_bstats)
  - `metrics.enabled` config; collect anonymous usage insights

---

## ⚡ Quick wins (can ship anytime)
- [ ] Doctor command skeleton + basic checks (subset of plan_doctor)
- [ ] CI artifacts + JAR size checks (subset of plan_ci_matrix)
- [ ] i18n sweep of new admin messages (subset of plan_i18n)

---

## 📋 Suggested order of execution
1) plan_doctor
2) plan_ci_matrix
3) plan_guis (Admin Economy GUI)
4) plan_db_migration
5) plan_guis (Jobs GUI)
6) plan_econ_balance

---

## 🗂️ Done (link PRs/commits)
- [ ] (add items here when complete)
