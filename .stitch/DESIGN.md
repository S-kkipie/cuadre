---
name: Cuadre — monochrome
colors:
  paper: '#ffffff'
  ink: '#111111'             # text, primary buttons, active nav, switch
  ink-muted: '#6b6b6b'       # labels, secondary lines, inactive nav
  hairline: '#eaeaea'        # row dividers, outlined buttons
  disabled-fill: '#e6e6e6'
  disabled-ink: '#8a8a8a'
  paid: '#0b7a3b'            # ONLY: confirmation flood + live "Escuchando" dot + "Listo ✓" status
  stale: '#9a5b00'           # ONLY: "Pago anterior" flood + pending-setup line
  on-flood: '#ffffff'
  on-flood-muted: 'rgba(255,255,255,0.70)'
typography:                   # Geist only. Tabular figures on every amount.
  amount-hero:   { fontFamily: Geist, fontSize: 64px, fontWeight: '700', letterSpacing: '-0.03em' }   # confirmation
  amount-total:  { fontFamily: Geist, fontSize: 48px, fontWeight: '700', letterSpacing: '-0.03em' }   # Hoy total
  title:         { fontFamily: Geist, fontSize: 28px, fontWeight: '600', letterSpacing: '-0.02em' }
  row-title:     { fontFamily: Geist, fontSize: 18px, fontWeight: '500' }
  body:          { fontFamily: Geist, fontSize: 16px, fontWeight: '400' }
  secondary:     { fontFamily: Geist, fontSize: 15px, fontWeight: '400' }   # ink-muted
  code:          { fontFamily: Geist, fontSize: 48px, fontWeight: '600', letterSpacing: '0.25em' }
rounded:
  button: 12px
  pill: 9999px
spacing:
  side: 24px
  flood-side: 28px
  row-vertical: 16px
  section-gap: 32px
  touch-min: 48px
  touch-primary: 56px
---

# Cuadre design system — monochrome

Source of truth for the Compose UI. Reference screens in [`designs/`](designs/) (Stitch project
`2866795004547147911`). Direction chosen by the user after rejecting v1 ("too AI-looking, too
complicated"; archived in `designs/v1-descartado/`). Audited against Hallmark's anti-pattern list.

## Principle
Minimalism with conviction (Stripe / Linear / Apple Wallet school). Typography and whitespace do
the work. Color is a signal, never decoration: green = real money arrived, amber = old payment.

## Rules
- **One family: Geist.** Hierarchy by size and weight only. Sentence case everywhere — no
  uppercase letter-spaced eyebrows ("Qué lee Cuadre", not "QUÉ LEE CUADRE").
- **No cards on lists.** Plain rows, 1px hairline dividers, left-aligned text, amounts right in
  tabular figures. No leading icon tiles, avatars, badges, chips or "Nuevo" tags.
- **One action per row.** No redundant buttons (e.g. no global "Arreglar" when each app has one).
- **Buttons:** primary = ink fill + white text, 56dp, full width, bottom of screen; secondary =
  white with ink 1px outline, compact; links = underlined ink text. No arrows on buttons, no
  captions under buttons.
- **Nav:** hairline on top, thin line icons, active item = ink + semibold, inactive = muted. No
  filled pill.
- **No** shadows, gradients, glass, progress bars, decorative icons, wallet/bank logos or brand
  colors. Unknown payer: "Pagador no visible" in muted italic (body text, not a heading).

## Screens
- **Hoy** — header (wordmark "Cuadre" + date right; green dot + "Escuchando pagos" muted), total
  ("Recibido hoy" muted → `amount-total` → "23 pagos" muted), wallet rows, "Modo caja" row with
  switch + muted helper, "Pagos" + "Historial" link, payment rows
  (name · amount / "Yape · 14:32 · cód. 418"), bottom nav Hoy / Historial / Ajustes.
- **Pago recibido** — full `paid` flood, left-aligned, 28dp sides. Top: "✓ Pago recibido", muted
  "Yape · 14:32:07". Middle: `amount-hero`, payer below. Then muted "Código de seguridad" +
  `code` digits, muted "hace 3 segundos". Bottom: muted "1 pago más" (plain text) + white 56dp
  "Listo" with ink text.
- **Pago anterior** — same layout on `stale` flood: "Pago anterior" with a clock glyph, muted age
  "hace 4 min", one plain sentence "Este pago no es de ahora. No lo uses para confirmar al
  cliente que tienes enfrente.", code muted, no queue line, no sound.
- **Antes de empezar** (setup) — title + one muted line; "Qué lee Cuadre" with three en-dash
  sentences + underlined "Política de privacidad" link; hairline rows: "Acceso a notificaciones"
  → green "Listo ✓"; "Avisos con sonido" + muted helper → outlined "Permitir"; "Ahorro de
  batería" + amber helper → per-app sub-rows "Yape — Arreglar", "BBVA — Arreglar"; bottom
  56dp "Empezar" (disabled until all done).

## Voice
Peruvian Spanish, short and plain: "Recibido hoy", "Pago recibido", "Listo", "Arreglar",
"Permitir", "Empezar".
