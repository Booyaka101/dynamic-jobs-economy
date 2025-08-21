# Admin Economy GUI – Design Spec

## Goals
- Provide an in-game admin interface for economy moderation tasks.
- Minimize command typing; offer safe, auditable actions with confirmation.
- Respect granular permissions and localization.

## Permissions (plugin.yml)
- djeconomy.gui.admin.economy — open Admin Economy GUI
- djeconomy.gui.admin.economy.balance.view — view balances
- djeconomy.gui.admin.economy.balance.modify — give/take/set
- djeconomy.gui.admin.economy.history.view — view transaction history
- djeconomy.gui.admin.economy.confirm.manage — manage pending confirmations

Notes:
- If a user lacks a permission, hide or disable the related controls and show a tooltip indicating missing permission.

## Entry Points
- Command: `/djeconomy gui` (admin routes to Admin Economy home if has djeconomy.gui.admin.economy)
- Future: Dedicated subcommand `/djeconomy admin economy`
- Programmatic: `AdminEconomyGui.open(Player, Optional<Target>)`

## Primary Views
1) Home
- Sections: Player Tools, Business Tools, Confirmations, Search
- Quick actions tiles (open player selector, open confirmations)

2) Player Selector
- Paginated list with search bar (name startsWith/contains)
- Item: player head + name + balance snapshot (requires balance.view)
- Click -> Player Account View

3) Player Account View
- Header: Player head, name, UUID, current balance
- Actions (requires permissions):
  - Give +N, Take −N, Set =N (pre-filled amounts: 100, 1k, 10k, custom anvil input)
  - Confirm flow: if amount >= `economy.admin_confirmation.threshold`, create pending confirmation (expiry `economy.admin_confirmation.expiry_seconds`).
- Tabs:
  - Overview (default)
  - History (requires history.view) — last 50 transactions; filter by type/date

4) Confirmations Queue (requires confirm.manage)
- List pending items: requester, target, action, amount, created at, expires at
- Actions: Approve, Deny; optional reason (anvil input)
- Auto-expire handling shown inline

## Layout and UX
- Inventory size: 54 (6 rows) standard; use `config.yml` gui.useFillerPanes framing
- Common controls:
  - Back: slot 45
  - Close: slot 49
  - Next/Prev page: slots 53/45 (if Back repurposed, move to 44)
  - Search: slot 4 (anvil input)
- Color/status conventions: PASS/OK green, WARN yellow, FAIL red (align with doctor)
- Tooltips show action, needed permission, and confirmation policy if applicable

## Data + Services
- Economy access via `EconomyManager` (existing). Required ops:
  - getBalance(playerUUID)
  - give(playerUUID, amount, source)
  - take(playerUUID, amount, source)
  - set(playerUUID, amount, source)
  - listRecentTransactions(playerUUID, limit, filters)
- Confirmation service (simple in-memory + persistent mirror):
  - createPending(actorUUID, targetUUID, action, amount, expiresAt)
  - listPending()
  - approve(id, approverUUID)
  - deny(id, approverUUID, reason)
  - autoExpireSweep()

Note: If methods do not exist, provide wrappers in a dedicated `AdminEconomyService`.

## Safety & Validation
- Clamp negative amounts; validate decimals; hard cap using `economy.max_money`
- Respect admin confirmation thresholds; display timer on pending entries
- All actions write audit entries (who, what, when, old->new, source)

## Localization Keys (messages.yml)
- gui.admin.economy.title.home: "Admin Economy"
- gui.admin.economy.title.player_select: "Select Player"
- gui.admin.economy.title.player: "Player • %player%"
- gui.admin.economy.action.give: "Give %amount%"
- gui.admin.economy.action.take: "Take %amount%"
- gui.admin.economy.action.set: "Set %amount%"
- gui.admin.economy.history.header: "Recent Transactions"
- gui.admin.economy.confirm.required: "Confirmation required for %amount%+"
- gui.admin.economy.confirm.created: "Pending confirmation created (expires in %seconds%s)"
- gui.admin.economy.confirm.queue.title: "Pending Confirmations"
- gui.admin.economy.confirm.approved: "Approved by %admin%"
- gui.admin.economy.confirm.denied: "Denied by %admin% (%reason%)"
- gui.admin.economy.error.no_permission: "You lack permission: %node%"
- gui.common.close, gui.common.back, gui.common.next, gui.common.prev

Provide sensible defaults with placeholders; reuse status coloring from doctor.

## Telemetry / Audit
- Log each admin economy action at INFO; WARN on denied; ERROR on failures
- Include correlation IDs for multi-step (request -> approve)

## Minimal Implementation Plan
1) Create `AdminEconomyGui` with views: Home, PlayerSelect, PlayerAccount, Confirmations
2) Inject `EconomyManager` + `AdminEconomyService`
3) Wire commands to open GUI (permission-gated)
4) Add confirmation store + scheduled sweep task
5) Add unit tests for confirmation expiry and permission gating

## Future Enhancements
- Business account tabs
- Batch actions for multiple players
- CSV export of history (admin download)
- Filters by Vault provider when applicable
