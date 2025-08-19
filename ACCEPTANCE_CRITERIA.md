# DynamicJobsEconomy — End-to-End Acceptance Criteria

This document defines end-to-end acceptance criteria for commands, permissions, messages, persistence, and tab completion across the admin command surface.

Applies to command root `djeconomy`.

## Conventions
- Prefix comes from `config.yml` at `messages.prefix` and is prepended to all plugin messages.
- Player resolution: prefer online players; if not online, accept offline players who have played before. For offline targets, show an informational note before processing.
- Money formatting is `$<amount>` with 2 decimals as appropriate; thousands separators may vary by JVM locale.
- History log file path: `<plugin data folder>/admin-economy-history.log`.
- History lines format: `timestamp|admin|ACTION|target|amount` with `ACTION` in `GIVE|TAKE|SET`.
- History `limit` argument: default 10. Clamp to [1, 100]. Non-numeric -> default.
- Large economy transactions: require confirmation for amounts >= 100,000. Pending confirmation expires in 30s.

## Permissions
- Root visibility/tab suggestions are permission-aware.
- Granular nodes (examples):
  - `dynamicjobseconomy.admin.setlevel`
  - `dynamicjobseconomy.admin.getlevel`
  - `dynamicjobseconomy.admin.resetlevel`
  - `dynamicjobseconomy.admin.addxp`
  - `dynamicjobseconomy.admin.economy`
  - `dynamicjobseconomy.admin.history`
  - `dynamicjobseconomy.admin.reload`
  - `dynamicjobseconomy.admin.confirm`
- Without permission, command executes with a clear "no permission" message and no side effects.

## Commands & Messages

### setlevel <player> <job> <level>
- Level must be a positive integer.
- On non-numeric/invalid: error message indicating invalid level number.
- On success: message indicating new level set for player/job.
- Applies only to online players; offline players are rejected with a clear message.

### getlevel <player> <job>
- Resolves player (online/offline) and job. If player never joined or job invalid -> error message.
- On success: prints current level for the job.

### resetlevel <player> <job>
- Resets the player's progress in the specified job.
- Online player required; offline rejected with message.
- On success: confirmation message.

### addxp <player> <job> <amount>
- `amount` must be a non-negative integer.
- Non-numeric/negative -> error message.
- Requires player currently has the job; otherwise error message stating the player hasn't joined the job.
- Online player required; offline rejected with message.
- On success: message with amount added (accepts large values).

### economy <give|take|set> <player> <amount>
- Valid actions: `give`, `take`, `set`. Invalid action -> error message.
- `amount` must be numeric and non-negative; non-numeric/negative -> error message.
- Upper bound protected (reject clearly if beyond maximum, e.g., > 1,000,000,000).
- Large amounts (>= 100,000) require confirmation:
  - First call: prints a warning and instructs to run `/djeconomy confirm` within 30 seconds. No funds move yet.
  - Offline targets: show a note that the player is offline before processing.
- Action behaviors:
  - give: increases balance. Uses online API for online players; offline-safe path for offline players.
  - take: decreases balance. If insufficient funds, error indicating current balance; no withdrawal attempted.
  - set: sets exact balance to `amount`.
- On success: confirmation message stating action, amount, and target.
- Persistence: append action to history log (see format above).

### confirm
- Only usable by players (not console). Console usage produces a clear error.
- Requires `dynamicjobseconomy.admin.confirm`.
- If no pending action: message indicating nothing to confirm.
- If pending and not expired: executes the queued economy action and produces the same success message as if performed immediately.

### history [limit]
- Optional `limit` numeric; default 10; clamp to [1, 100]. Non-numeric -> default.
- If history file missing/empty: message indicating no history.
- IO errors surface as an error message without crashing.
- Output includes the latest entries up to limit, with formatting consistent with the stored log lines.

### reload
- Reloads configuration and applies runtime changes (e.g., message prefix).
- The first message after reload still uses the old prefix (pre-reload) for that specific call; subsequent messages use updated prefix.
- Requires `dynamicjobseconomy.admin.reload`.

## Tab Completion
- Root subcommands filtered by the sender's permissions.
- Arguments:
  - For player arguments: suggest online player names; also include previously seen offline players when applicable.
  - For job arguments: suggest known job IDs.
  - For numeric arguments: suggest placeholders (e.g., `10`, `100`, `1000`).
- Empty or prefix filters return appropriate subset.

## Persistence & Side Effects
- All successful economy operations write a single line to the history log.
- History command reads from the same log file.
- No file is created on read-only actions until a write occurs.

## Non-Functional
- Commands should not throw exceptions to the caller on invalid input; they must respond with friendly, prefixed error messages and return `true` to signal handling.
- All operations must be compatible with both Vault-backed and internal economy modes.
