# Cuadre — Spec

Android, Peru-first. Micro-merchant cash register fed by incoming Yape/Plin notifications.

## 1. User

Micro-merchant receiving payments on personal Yape/Plin: bodega, market stall, menú/comida, taxi/mototaxi, salon, informal seller. Receives under ~S/25k/month (below Yape Empresa's threshold), often leaves the phone at the till with an employee, fears both fake-Yape scams and SUNAT.

## 2. Jobs to be done

- "When a customer says they paid, tell me **instantly and truthfully** whether the money actually landed — so a fake screenshot can't rob me."
- "At day's end, show me **how much really came in** by Yape/Plin/cash and who paid, without me ringing up every sale."
- "Keep a **clean income record** so if SUNAT comes I'm covered."
- "Let me see the till from home while my employee runs it."

## 3. What it reads (and what it never does)

- Reads **incoming** payment notifications only, via `NotificationListenerService`, from a known set of source packages (Yape, Plin, later bank apps). Everything stays on device.
- Never reads the bank account, never logs in, never moves money, never trusts the customer's screen. The **listener event is the only source of truth.**
- Outgoing/other notifications are ignored.

## 4. The deterministic core (moat + anti-fraud)

No LLM in the trust path.

- **Parser** (`PaymentParser`): from a notification's package + title + text, extract `amount`, `counterparty` (payer name if present), `direction` (must be incoming), `wallet`. Per-wallet rule sets; unknown/ambiguous text → `null` (not a guess), surfaced as "couldn't read — check manually."
- **Verification** (`VerificationEngine`): dedupe by (wallet, amount, counterparty, timestamp window) so one payment isn't counted twice from re-posted notifications; assign a stable id; mark `confirmedReal`. A payment exists only if it came through the listener — a screenshot never produces one.
- **Daily close**: deterministic sum by wallet over the business day.

All pure Kotlin, JVM-unit-tested, no Android device needed to verify.

## 5. The model's job (secondary, off the trust path)

Optional, later: categorize payments, surface insights ("Tuesdays are your slowest"), draft the SUNAT income summary in plain language. It may never assert that a payment is real/received — only the core does that.

## 6. Anti-fraud UX

- The moment a real incoming payment matching the expected amount lands, a loud, full-screen, hard-to-fake confirmation (sound + color + amount + payer + time). The scammer controls their own screen, not yours; this fires only on the real listener event.
- A manual "waiting for S/ ___" mode: merchant enters the amount the customer claims; app confirms only when a matching real notification arrives within N seconds.

## 7. Storage & privacy

- Room (SQLite) on device. No server account in v0. Nothing leaves the phone.
- Prominent in-app disclosure before requesting notification access, explaining exactly what is read and that it stays local.

## 8. Google Play policy (must-do, not optional)

- `NotificationListenerService` access is granted by the user in system Settings; the app must have a **core feature** that needs it and show **prominent disclosure** + a privacy policy.
- Complete the **Financial features declaration** in Play Console.
- Do **not** use "Yape"/"Plin"/"BCP" as the app name, icon, or in a way implying affiliation (trademark). Describe as "works with your Yape/Plin notifications."
- No reading of SMS/Call Log (not needed; heavily restricted). Notifications only.

## 9. Monetization

- Freemium. **Free:** live capture, real-payment confirmation, today's close. **Paid** (~S/9.90/mo or annual): SUNAT income report + export, history beyond 30 days, multiple employees/devices, low-sales and mismatch alerts.
- Upsell driven by the two fears already in the market: getting scammed, and SUNAT.

## 10. Risks

- **Parser fragility:** Yape/Plin change notification text → capture breaks. Mitigate with defensive parsing, a fast patch path, and a remote rule-set update later.
- **Platform risk:** Yape could add free anti-fraud (they shipped a security code) or lower the Empresa threshold. Hedge: own the micro segment + SUNAT record, which Yape does not serve.
- **Play review** of notification access. Mitigate with clear core-feature justification + disclosure.
- **Notification encryption:** if Yape ever stops putting amount/payer in the notification, capture degrades to "a payment arrived" without detail. Design for that fallback.

## 11. Two-week plan (solo, Linux-only)

**Week 1 — the core, provable without a device**
- D1: Gradle project, modules, CI-free JVM test setup.
- D2: `PaymentEvent` / `Wallet` / `PaymentType` domain; Room entity + DAO.
- D3: `PaymentParser` with Yape + Plin rule sets. **Unit tests** against captured notification-text fixtures.
- D4: `VerificationEngine` — dedupe + confirm + daily-close sum. **Unit tests** (double-post, near-duplicate, wrong-direction, ambiguous).
- D5: `PaymentNotificationListenerService` wiring + onboarding/disclosure screen.

**Week 2 — device, UX, money**
- D6: Live capture on a real phone; confirm against a second phone sending Yapes. Collect real notification fixtures; harden the parser.
- D7: Anti-fraud confirmation UX (loud, full-screen) + "waiting for S/__" mode.
- D8: Daily close screen + history (Room) + manual-entry fallback for cash / unreadable notifications.
- D9: SUNAT income report + export (CSV/PDF).
- D10: Paywall (freemium gating), Financial features declaration, privacy policy, store listing.
- Buffer: closed test on Play, record demo (real payment confirms vs. a fake screenshot that does not).

Distribution in parallel: short clips showing a **fake Yape screenshot failing to confirm** while a real one fires the alert — in bodega/market-vendor communities. Distribution is the bottleneck, not the code.
