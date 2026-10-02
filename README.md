# Cuadre

Working codename. An Android cash register for Peruvian micro-merchants that **captures incoming Yape/Plin payments from the phone's notifications**, confirms a payment is real (kills the fake-screenshot scam), and keeps a clean daily close and a SUNAT-ready income record — **without a POS, without handing over the Yape account, and without Yape Empresa's 2.95%**.

## Why

- Fake-Yape screenshot fraud is one of the most frequent problems for merchants across Peru. The only valid proof of payment is the money actually landing in **your own** app — i.e. the real incoming notification, not the customer's screen.
- Yape Empresa solves remote visibility + helpers, but only pays off above ~S/25,750/month and charges 2.95%. Yape itself says the other **99.5%** (micro) stays on free Yape — with **no official anti-fraud / remote-visibility / income-record tool**.
- SUNAT now fiscalizes undeclared business income over ~S/45,000/year received via Yape/Plin. Millions of micro-merchants suddenly need an income record. Fear-driven willingness to pay.

iOS cannot read other apps' notifications; Android can. Peru is ~85% Android. The platform and the market line up.

## What it does (v0)

1. Reads **incoming** payment notifications from Yape/Plin (and later bank apps) via a `NotificationListenerService`, on device.
2. A deterministic core parses amount / sender / time, **dedupes**, and marks the payment **confirmed real** — the anti-fraud signal a screenshot can't fake.
3. Loud, unspoofable in-app confirmation the moment a real payment lands.
4. Daily close ("cuadre"): total received, by wallet, who paid.
5. SUNAT-ready income ledger + export.
6. Remote visibility: owner sees each confirmed payment on their own device while an employee runs the till (later: multi-device sync).

## What it is NOT

Not a payment processor, not affiliated with Yape/BCP, not a POS. It reads notifications the user already receives on their own phone and organizes them. No bank login, no account access, no money movement.

## Moat

- **Resilient parser + deterministic verification** (Vigil-style reliability): incoming-only, deduped, confirmed from the listener event — never from a screenshot. Bank/Yape notification text changes → the parser is built to degrade safely and be patched fast.
- **The segment Yape abandoned on purpose** (sub-S/25k, anti-2.95%), so the platform owner is not competing here.
- **Peru/Yape-native + SUNAT framing** that global apps don't do.

## Build

Android, **builds entirely on Linux/WSL** (no Mac). Android Studio or Gradle CLI.
- Unit tests (parser + verification) run on the JVM: `./gradlew testDebugUnitTest` — no device needed. This is where the moat is proven first.
- The `NotificationListenerService` needs a real device/emulator and the user granting notification access in Settings.

See [`docs/SPEC.md`](docs/SPEC.md) for the full spec, Play policy notes, monetization, and the 2-week plan.

## Status

Scaffold. Deterministic core + parser + tests are real; listener/UI are thin.
