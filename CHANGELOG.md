# Dynamic Jobs & Economy Pro - Changelog

## Unreleased

### ✨ Improvements
- CI now publishes SHA-256 checksums for all built artifacts and verifies integrity before upload.

### 🐛 Bug Fixes
- 

### 🧪 Testing
- 

### 📚 Docs
- Removed all MongoDB references and documented dropped MongoDB support.
- Added checksum verification instructions to `README.md` and `SPIGOTMC_UPDATE_GUIDE.md`.

## Version 1.0.5 - "Polish & Performance Reporting Fix" (August 2025)

### ✨ Improvements
- Minor polish across business command help and GUI documentation.

### 🐛 Bug Fixes
- Fixed a critical bug in `ConsolidatedBusinessManager.getBusinessPerformanceReport(...)` where a missing return could lead to incorrect or empty results.
- Fixed a lot of misbeaving Business GUI related issues.

### 🧪 Testing
- Verified full compile via Maven and prepared test plan for GUI navigation and reload behavior (no duplicate revenue tasks).

### 📚 Docs
- Updated `README.md` to reference 1.0.5 artifacts and clarified admin vs player command visibility.
- Updated `ROADMAP.md` current version to 1.0.5 and rolled near-term targets forward.

## Version 1.0.4.1 - "GUI Tooltip Hotfix, and GUI Protection" (August 2025)

### 🐛 Bug Fixes
- Fixed persistent blank tooltip when hovering over empty GUI slots by introducing a config toggle to disable filler panes.
- Hardened GUI protections to prevent moving items on Business GUI screens by cancelling both click and drag interactions while a `BusinessGUI` is open.
- Fixed broken/inconsistent UI screens by ensuring all Business GUI inventories use `BusinessGUIHolder` and validating top-inventory checks.

### 🔧 Config
- New key in `config.yml`:
  - `gui.useFillerPanes` (default: `true`) — when `false`, empty GUI slots are left empty to avoid hover tooltips.

### Notes
- Behavior for click/drag blocking in GUI is unchanged; interactions remain canceled while a `BusinessGUI` inventory is open.

---

## Version 1.0.4 - "Admin Economy Fixes & i18n" (August 2025)

### ✨ Improvements
- Prefix precedence: `AdminCommand.getPrefix()` now prefers `config.yml` value, then messages.yml, then default; recalculated on `/djeconomy reload`.
- Added quick-access commands `/business gui` and `/business menu` (with tab completion) to open the business GUI faster. Permission: `djeconomy.gui.access`.

### 🐛 Bug Fixes
- Offline player deposits now call `EconomyManager.depositPlayer(...)` when the target is offline.
- Confirmation threshold/expiry correctly use `FileConfiguration#get*` overloads with defaults.
- Guarded against missing economy manager and send `admin.economy_unavailable` message.
- i18n keys and placeholders aligned for give/take/set, confirm prompts, expiry, offline notes, and reload success.

### 🧪 Testing
- AdminCommand economy edge cases and messages integration tests are green in CI.
- Verified confirmation prompt and expiry window behavior using the time seam.
- Verified offline player path triggers `depositPlayer` and correct messages.

### 📚 Docs
- Verified `README.md`, `USER_MANUAL.md`, and `permissions.yml` alignment with behavior.

---

## Version 1.0.3 - "Admin Economy Confirmation & Tests" (August 2025)

### 🧪 Testing
- Added success-path tests for admin economy commands:
  - `give` executes immediately for small amounts (online)
  - `take` executes when sufficient balance (online/offline)
  - `set` executes for online and offline players
- Added confirmation expiry test for large transactions using a testable time seam (no sleeps).

### 🔧 Code
- Introduced a time seam in `AdminCommand` via `nowMillis()`.
- Refactored `PendingAdminAction.isExpired(long now)` and updated all call sites.
- Behavior unchanged for players; enables reliable automated testing of expiry.

### 📚 Docs & CI
- Verified `permissions.yml`, `README.md`, and `USER_MANUAL.md` align with implementation.
- Documented large-amount confirmation and 30s expiry window.
- CI: All tests green.

---

## Version 1.0.2 - "Admin UX + Tests" (August 2025)

### ✨ Improvements
- Added `JobNameUtil` for consistent case-insensitive job handling and suggestions
- `JobManager.getJob()` now delegates to `JobNameUtil` for cleaner lookup
- Improved admin tab completion for `/djeconomy setlevel|addxp` using canonical job suggestions

### 🧪 Testing
- Added `JobNameUtilTest` (JUnit 5) covering case-insensitive lookup and suggestions

### 📚 Docs & CI
- Admin command docs already reflect case-insensitive usage
- CI continues to run unit tests on PRs and pushes

---

## Version 1.0.1 - "Polish & Security Update" (January 2025)

### 🛡️ **Security Enhancements**
- **Gig Escrow System**: Gigs now require poster approval before payment is released
- **Employee Consent**: Business hiring/firing now requires employee acceptance
- **Admin Transaction Safety**: Large admin transactions (>$100k) require confirmation
- **Atomic Business Operations**: All business transactions are now rollback-safe

### 🤖 **New Automated Features**
- **Auto-Payroll**: Businesses automatically pay employee salaries every hour
- **Auto-Save**: Player data saves every 5 minutes and on logout
- **Memory Cleanup**: Automatic cleanup of tracking data to prevent memory leaks

### 👨‍💼 **Enhanced Admin Commands**
- **Offline Player Support**: All admin economy commands now work with offline players
- **New Confirmation System**: Use `/djeconomy confirm` for large transactions
- **Enhanced Logging**: All admin actions are logged with timestamps

### 🎮 **Improved User Experience**
- **Complete Tab Completion**: All commands now have full tab completion
- **Real Job Perks**: Job perks now provide actual potion effects
- **Anti-Exploit Protection**: Added cooldowns to prevent farming abuse
- **Better Error Messages**: Clearer feedback for all operations

### 🔧 **New Command Features**

**Business Commands:**
- `/business accept-hire` - Accept a business job offer
- `/business reject-hire` - Reject a business job offer
- Enhanced `/business hire <player> <business_id>` - Now requires employee consent

**Gig Commands:**
- `/gigs approve <gig_id>` - Approve completed gig work (poster only)
- `/gigs reject <gig_id>` - Reject completed gig work (poster only)
- Enhanced `/gigs complete <gig_id>` - Now submits for approval instead of auto-paying

**Admin Commands:**
- `/djeconomy confirm` - Confirm large admin transactions
- Enhanced `/djeconomy economy give/take/set <player> <amount>` - Now works with offline players
- Enhanced `/djeconomy setlevel <player> <job> <level>` - Now works with offline players

### 🐛 **Critical Bug Fixes**
- Fixed compilation errors in command classes
- Resolved null pointer exceptions in admin commands
- Fixed missing employee tracking in businesses
- Corrected offline player economy operations
- Improved plugin reload functionality

### ⚡ **Performance Improvements**
- Optimized database queries
- Fixed memory leaks in tracking systems
- Enhanced async task management
- Reduced server lag impact

---

## Version 1.0.0 - "Initial Release" (January 2025)

### 🚀 **Initial Features**

**Core Systems**
- Complete job system with 5 job types (Miner, Chef, Farmer, Builder, Merchant)
- Business management with employee hiring and payroll
- Gig marketplace for freelance work
- Advanced economy system with Vault integration
- Multi-database support (SQLite, MySQL)

**Command System**
- `/jobs` - Complete job management
- `/business` - Business creation and management
- `/gigs` - Gig marketplace operations
- `/djeconomy` - Admin tools and commands

**Integration Support**
- Vault economy integration
- WorldGuard region support
- McMMO skill integration
- LuckPerms permission system
- ShopGUIPlus framework

---

*Dynamic Jobs & Economy Pro is developed by **BooPug Studios** with ❤️*
*For support, visit our Discord or create an issue on our GitHub repository.*
