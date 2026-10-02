# Cuadre — Design brief

Input for designing the UI (Google Stitch → `DESIGN.md` → Compose components in `ui/`).
Product context and plan: [`SPEC.md`](SPEC.md).

## Who uses it

- Peruvian micro-merchants: bodega, market stall, menú, salon, mototaxi, informal seller.
- Often 35–65 years old, mid/low-end Android (Samsung A-series, Xiaomi Redmi, Motorola), sometimes cracked screens.
- The phone sits on the counter, plugged in, or in one hand while the other gives change.
- An employee may run the till while the owner checks from home.
- Spanish (Peru) only. Plain words: "Pago recibido", "Hoy", "Cuadre del día". No jargon ("transacción", "dashboard").

## What the UI must do well

1. **Prove a payment is real, from 2 meters away.** The full-screen confirmation is the product. Huge amount, payer, time with seconds, wallet, security code. Unmistakable color + sound. Must look nothing like Yape's own screen (so it can't be "shown" by a scammer as Cuadre).
2. **Make old payments look old.** A payment older than 60 s is visibly different (amber, "PAGO ANTERIOR", no sound).
3. **Answer "how much came in today" in one glance.** Total first, then by wallet, then the list.
4. **Make setup hard to get wrong.** Notification access, alerts permission and battery restrictions are what silently breaks capture; each needs a clear status (✅ / ⚠️) and one button to fix.

## Constraints

- Legible in sunlight and for older eyes: body ≥ 16 sp, key numbers ≥ 32 sp, contrast ≥ WCAG AA (AAA for amounts).
- Touch targets ≥ 48 dp, primary actions ≥ 56 dp, reachable by thumb (bottom half).
- Light theme first (counter, daylight); dark theme supported.
- Works on 360 dp wide screens; no horizontal scroll.
- Money always `S/ 1,250.50` (es-PE). Times `14:32` in lists, `14:32:07` on confirmations.
- No Yape/Plin/BCP/BBVA logos or brand colors (trademark; see SPEC §8). Wallets are named in text only.
- Offline: nothing depends on network.

## Screens (v0)

| Screen | Purpose | Key content |
|---|---|---|
| **Hoy** (home) | Live till | Total received today, by wallet, list of payments, till-mode switch, setup warnings |
| **Confirmación** | Anti-fraud moment | Full-screen, fresh (green) vs. old (amber), queue "+N más", dismiss |
| **Configuración inicial** | First run | Disclosure (what is read, stays on phone) → notification access → alerts → battery, each with status |
| **Historial** | Past days | Day list with totals, tap → that day's payments |
| **Cierre del día** | End of day | Total, by wallet, count, cash entered manually, share/export |
| **Reporte SUNAT** (paid) | Monthly income record | Month total, export CSV/PDF |

## Tone

Trustworthy and calm like a bank, but warm and local, not corporate. Confident green for "money arrived". Few colors, big numbers, lots of whitespace.
