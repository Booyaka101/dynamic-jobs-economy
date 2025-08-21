# 🚀 Future Release Roadmap – Dynamic Jobs & Economy Pro

Current released version: 1.0.5

Use this file during planning/release sessions. Check items off as we implement them and link PRs/commits.

## ✅ How to use this file
- Mark tasks with [x] when completed and add PR/commit links.
- Keep IDs in parentheses so we can cross-reference with issue tracker/notes.
- Group by target version; move items forward/back as priorities change.

---

## 🌟 Near-term (target: 1.0.6)
- [x] Implement "/djeconomy doctor" self-check with actionable tips (id: plan_doctor)
  - Validate config keys, DB connectivity, Vault presence, permissions sanity
  - Output clear pass/warn/fail with suggestions
  - Touchpoints: `src/main/resources/config.yml`, `plugin.yml` (new command), DB service, integrations
  - Implementation checklist (IDs):
    - [x] Add command + usage to `plugin.yml` (id: plan_doctor_cmd)
    - [x] `DoctorCommandExecutor` skeleton and routing (id: plan_doctor_exec) — initial implementation present; refine outputs and i18n
    - [x] `ConfigValidator` for required keys (id: plan_doctor_config)
    - [x] `VaultHealthCheck` (plugin + provider) (id: plan_doctor_vault)
    - [x] `DatabaseHealthCheck` for sqlite/mysql (id: plan_doctor_db)
    - [x] Console vs player output formatting + suggestions (id: plan_doctor_output)
    - [x] Unit tests for validators (id: plan_doctor_tests_unit)
    - [x] Integration tests using Testcontainers MySQL (id: plan_doctor_tests_it)
    - [x] Externalize all doctor output strings to `messages.yml` (id: plan_doctor_i18n)
    - [x] Permissions docs alignment for `djeconomy.system.doctor` (id: plan_doctor_perms)
- [x] CI build matrix + artifacts + JAR size guard (id: plan_ci_matrix) — completed in `.github/workflows/build.yml`
  - Build profiles: default, `spigot-lite`, `spigot-ultra`, `spigot-ultra-mysql`, `spigot-linux-sqlite`
  - Upload artifacts and enforce max-size thresholds
  - Tracked in `plan_ci_checksums`
  - Touchpoints: `.github/workflows/build.yml`, `pom.xml`
- [ ] Performance pass (async + caching + batching) (id: plan_perf)
  - Async DB operations, cache hot player/job data, batch writes, safe flush on shutdown
  - Add lightweight timings/diagnostics with debug toggle
  - Acceptance criteria:
    - P95 main-thread impact of admin economy ops ≤ 2ms under light load
    - Zero synchronous DB calls on main paths (verified via logs/timings)
    - Cache hit rate ≥ 80% for hot player/job lookups in a 5-minute window
    - Clean shutdown: caches flushed without errors in logs
  - Owner: Us | Due: TBD
- [ ] Publish artifact checksums in CI (sha256) (id: plan_ci_checksums)
  - Generate checksums for each built JAR per profile
  - Upload alongside artifacts in GitHub Actions
  - Provide README snippet on how to verify checksums
  - Acceptance criteria:
    - Every artifact has a corresponding .sha256 file
    - Workflow step verifies checksum integrity before upload
    - Changelog and update guide mention checksum verification
  - Owner: Us | Due: TBD
  - Status: Completed in CI workflow and docs (see CHANGELOG Unreleased)
- [ ] Admin Economy GUI with confirm flows (id: plan_admin_gui)
  - Give/Take/Set with confirmation, reason logging, history viewer
  - Touchpoints: command handlers, GUI classes, `messages.yml`
  - Acceptance criteria:
    - Gated by `djeconomy.gui.admin` and granular perms under `djeconomy.gui.admin.economy.*`
    - Confirmation threshold/expiry honored from config; denial/approval logged to history
    - All new strings externalized to `messages.yml` with i18n keys
  - Owner: Us | Due: TBD
  - Implementation checklist:
    - Scaffold `AdminEconomyGui` views: Home, PlayerSelect, PlayerAccount, Confirmations
    - Wire `/djeconomy gui` entry; gate with `djeconomy.gui.admin.economy`
    - Inject `EconomyManager`; add `AdminEconomyService` wrappers if needed (give/take/set/history)
    - Implement confirmation store + scheduled expiry sweep task
    - Add i18n keys in `messages.yml`; config toggles in `config.yml` (gui.useFillerPanes, confirmation threshold/expiry)
    - Tests: permission gating and confirmation expiry
    - Docs: link `docs/gui/admin-economy-gui.md` and add brief README snippet
    - Changelog: add entry upon feature completion

### ✅ 1.0.6 Acceptance Criteria
- plan_doctor
  - `/djeconomy doctor` prints plugin version, Java version, server brand
  - Validates keys: `database.type`, `economy.admin_confirmation.threshold`, `economy.admin_confirmation.expiry_seconds`, `integrations.vault.enabled`
  - If Vault integration enabled, checks Vault plugin and economy provider presence
  - Database ping based on `database.type`:
    - sqlite: file accessible + open connection
    - mysql: TCP connect + auth test with timeout
  - Outputs clear pass/warn/fail with suggestions; logs to console when run by console
- plan_ci_matrix
  - CI builds the following profiles: default, `spigot-lite`, `spigot-ultra`, `spigot-ultra-mysql`, `spigot-linux-sqlite`
  - Each profile uploads a separate artifact with profile in the name
  - CI logs built JAR size per profile
  - Size guard step exists and is configurable via `MAX_JAR_SIZE` (bytes); disabled when unset
- plan_admin_gui (Admin Economy GUI)
  - Admin-only GUI gated by `djeconomy.gui.admin`
  - Supports Give/Take/Set with confirmation flow for large amounts and optional reason capture
  - Writes to existing admin history log and respects confirmation threshold/expiry from config
- plan_perf
  - Async DB for admin economy operations on main paths
  - Simple cache for hot player/job data with safe flush on shutdown
  - Lightweight timings/diagnostics behind debug toggle

### ⏱ Timeline (tentative)
- Week 1: `DoctorCommandExecutor` + `ConfigValidator` + SQLite `DatabaseHealthCheck` + i18n keys
- Week 2: `VaultHealthCheck` + MySQL checks + unit tests for validators/health checks
- Week 3: Admin Economy GUI scaffolding and confirm flows; wire granular permissions
- Ongoing: performance pass (async DB, caching, batching); CI checksum publishing

### 🔗 Tracking/PR mapping
- plan_doctor_cmd: done — see commit adding `djeconomy.system.doctor` to `plugin.yml` and docs
- plan_ci_matrix: done — `.github/workflows/build.yml` matrix + size guard
 - test-config: local tests skip Testcontainers by default; run with `-P integration-tests`. Docker-enabled MySQL ITs: `-P integration-tests-docker` with `$env:DJE_DOCKER='true'` on PowerShell (tracked in `pom.xml`; documented in `README.md`/`USER_MANUAL.md`)
 - plan_doctor_config: done — `ConfigValidator` implemented and tested (`ConfigValidatorTest`)
 - plan_doctor_vault: done — `VaultHealthCheck` implemented
 - plan_doctor_db: done — SQLite/MySQL checks implemented (`DatabaseHealthCheck`)
 - plan_doctor_output: done — `/djeconomy doctor` prints PASS/WARN/FAIL with tips in `DoctorCommandExecutor`
 - plan_doctor_i18n: done — doctor strings in `messages.yml` including permissions tips
 - plan_doctor_perms: done — `djeconomy.system.doctor` permission declared and used; `PermissionsHealthCheck` integrated
 - plan_doctor_tests_unit: done — unit tests for validators (`ConfigValidatorTest`, `PermissionsHealthCheckTest`)

### 🧾 1.0.6 Release Checklist
- [ ] Update docs: `CHANGELOG.md`, `README.md`, `INSTALLATION.md`, `QUICK_START.md`, `USER_MANUAL.md`, Spigot templates
- [ ] Build artifacts for profiles: default, `spigot-lite`, `spigot-ultra`, `spigot-ultra-mysql`, `spigot-linux-sqlite`
- [ ] Generate and upload checksums (.sha256) for each artifact (`plan_ci_checksums`)
- [ ] Sanity test DBs: SQLite and MySQL on 1.20.4–1.21.x
- [ ] Tag release and publish resource/update notes

---

## 📈 Mid-term (target: 1.1)
- [ ] Database schema versioning and migrations (id: plan_db_migration)
  - `schema_version` table, migration runner, safe roll-forward
  - Acceptance criteria:
    - `schema_version` records current version; runner applies only forward migrations
    - Pre-migration backup step for SQLite file and MySQL (optional, documented)
    - Unit tests for up/down migration scripts; integration tests for SQLite/MySQL upgrade path
    - No data loss in migration smoke tests (jobs, balances, businesses)
- [ ] Economy balancing toolkit (id: plan_econ_balance)
  - Money sinks (taxes/fees), inflation guard, per-world/job multipliers
  - Extended audit trail for large transactions and `/djeconomy history` filters
  - Acceptance criteria:
    - New config keys documented in `config.yml` and `USER_MANUAL.md`
    - Unit tests for multiplier application and tax/fee calculations
    - Admin history includes reason and context for large adjustments
- [ ] Jobs GUI for players (id: plan_jobs_gui)
  - Browse/join/leave jobs, perks, earnings; complements `/business gui`
  - Acceptance criteria:
    - Gated by `djeconomy.gui.access`
    - Supports view -> join/leave -> stats flow; reads from same services as commands
    - Strings externalized to `messages.yml` with i18n keys
- [ ] Expand i18n coverage and language pack template (id: plan_i18n)
  - Externalize any remaining strings; provide `messages_[lang].yml` templates
  - Acceptance criteria:
    - 100% coverage for new admin/player-facing strings
    - Provide `messages_en.yml` baseline and `messages_template.yml` for translators

---

## 🧭 Longer-term (target: 1.2+)
- [ ] Public plugin API & events (id: plan_api)
  - Events: JobLevelUp, MoneyTransaction, BusinessHire, etc.; Services API
  - Acceptance criteria:
    - `api` package with stable event classes and services interfaces
    - Javadocs for all public API elements; example plugin demonstrating event usage
- [ ] Deeper integrations (id: plan_integrations)
  - WorldGuard region modifiers (job multipliers), McMMO synergy, ShopGUIPlus hooks
  - Acceptance criteria:
    - Region multiplier rules configurable and honored in job payouts
    - Basic McMMO synergy demonstrated; hooks behind a toggle to avoid hard deps
    - ShopGUIPlus hooks documented; safe no-op when plugin absent
- [ ] Optional bStats telemetry (opt-in) (id: plan_bstats)
  - `metrics.enabled` config; collect anonymous usage insights
  - Acceptance criteria:
    - Disabled by default; enabling sends only anonymous counts (players, jobs usage)
    - README documents metrics and opt-in/out process; no sensitive data collected

---

## ⚡ Quick wins (can ship anytime)
- [x] Doctor command skeleton + basic checks (subset of plan_doctor)
- [x] CI artifacts + JAR size checks (subset of plan_ci_matrix)
- [x] Shade plugin relocations/filters/services; minimized JAR; services transformer (id: quick_shade)
- [x] Test profiles and Docker gating for ITs; documented usage (id: quick_tests)
 - [ ] Publish artifact checksums (sha256) in CI (subset of plan_ci_checksums)
 - [ ] i18n sweep of new admin messages (subset of plan_i18n)

---

## 📋 Suggested order of execution
1) plan_doctor
2) plan_ci_matrix
3) plan_ci_checksums
4) plan_admin_gui (Admin Economy GUI)
5) plan_db_migration
6) plan_jobs_gui (Jobs GUI)
7) plan_econ_balance

---

## 🗂️ Done (link PRs/commits)
- [x] plan_ci_matrix — build matrix, artifact upload, and size guard in `.github/workflows/build.yml`
