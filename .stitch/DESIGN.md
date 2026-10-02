---
name: Cuadre
colors:
  primary: '#005f2b'
  primary-container: '#0b7a3b'     # signature green: confirmation flood, primary actions
  on-primary: '#ffffff'
  on-primary-container: '#a4ffb5'
  secondary: '#8a5100'
  secondary-container: '#fdad55'
  on-secondary-container: '#714200'
  stale: '#9a5b00'                 # "PAGO ANTERIOR" flood, setup warnings
  tertiary: '#a11813'
  tertiary-container: '#c43328'
  error: '#ba1a1a'
  error-container: '#ffdad6'
  background: '#f9f9f6'
  surface: '#f9f9f6'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#f4f4f1'
  surface-container: '#eeeeeb'
  surface-container-high: '#e8e8e5'
  surface-container-highest: '#e2e3e0'
  on-surface: '#1a1c1b'
  on-surface-variant: '#3f493f'
  outline: '#6f7a6e'
  outline-variant: '#becabc'
typography:
  display:      { fontFamily: Plus Jakarta Sans, fontSize: 64px, fontWeight: '700', lineHeight: 72px }
  headline-lg:  { fontFamily: Plus Jakarta Sans, fontSize: 32px, fontWeight: '700', lineHeight: 40px }
  headline-md:  { fontFamily: Plus Jakarta Sans, fontSize: 24px, fontWeight: '600', lineHeight: 32px }
  body-lg:      { fontFamily: Atkinson Hyperlegible Next, fontSize: 18px, fontWeight: '400', lineHeight: 26px }
  body-md:      { fontFamily: Atkinson Hyperlegible Next, fontSize: 16px, fontWeight: '400', lineHeight: 24px }
  label-lg:     { fontFamily: Atkinson Hyperlegible Next, fontSize: 16px, fontWeight: '600', lineHeight: 20px }
  label-md:     { fontFamily: Atkinson Hyperlegible Next, fontSize: 14px, fontWeight: '600', lineHeight: 18px }
rounded:
  sm: 4px
  DEFAULT: 8px
  md: 12px
  lg: 16px
  xl: 24px
  full: 9999px
spacing:
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  touch-min: 48px
  touch-primary: 56px
---

# Cuadre design system

Source of truth for the Compose UI (`app/src/main/java/pe/aido/cuadre/ui/`). Generated with Google
Stitch (project `2866795004547147911`); reference screens in [`designs/`](designs/).
Body font note: Stitch doesn't support Atkinson Hyperlegible Next, so its renders use Public Sans;
the app ships Atkinson.

## Atmosphere
Trustworthy and calm like a bank, warm and local, never corporate. Few colors, big numbers,
generous whitespace. Built for older eyes and daylight on a shop counter.

## Color roles
- **Green `#0B7A3B`** — money arrived, confirmed real, primary actions.
- **Amber `#9A5B00`** — old payment ("PAGO ANTERIOR"), pending setup, caution.
- **Red** — missing permission, error.
- Warm off-white background, white cards with hairline `outline-variant` borders.
- Never Yape purple, Plin teal, BCP blue/orange, BBVA blue. No wallet/bank logos: neutral
  rounded tiles with a single initial (`surface-container` + `on-surface`).

## Typography
- Plus Jakarta Sans: headings and every money amount (tabular figures, heavy).
- Atkinson Hyperlegible Next: body and labels. Body never below 16px.
- Money: `S/ 1,250.50`. Hero total 64px with cents at ~60% size; list amounts bold, right-aligned.
- Payer names uppercase as received; "Pagador no visible" in italic, muted.

## Shape & layout
- 12px radius cards, 16px on large cards, pill buttons for primary CTAs.
- Touch targets ≥ 48dp, primary actions 56dp in the bottom thumb zone.
- 360dp-first single column, 16–24dp side padding. Flat, hairline borders, no heavy shadows.

## Components (from the reference screens)
- **Top bar**: app tile + "Cuadre" + date; right: status pill "● Escuchando".
- **Total card**: "RECIBIDO HOY" label, count chip "23 cobros", hero amount, proportional
  wallet bar, per-wallet rows (dot · name · amount · count).
- **Till-mode row**: tinted card, icon, title, helper, large switch.
- **Payment row card**: leading status tile (check for newest, receipt otherwise, "?" for
  unknown payer), payer + "NUEVO" badge, secondary line "Yape · 14:32 · cód. 418",
  amount right.
- **Bottom navigation**: Hoy / Historial / Ajustes, selected item as filled green pill.
- **Confirmation (fresh)**: full green flood, pill "Notificación verificada por Cuadre", white
  check disc, "PAGO RECIBIDO" letter-spaced, 64px amount, payer, "Yape · 14:32:07 · hace 3 s",
  translucent security-code panel ("4 1 8", "Verifica estos 3 dígitos con el cliente"),
  queue pill "+1 pago más en espera", white 56dp "Listo →".
- **Confirmation (stale)**: amber flood, clock disc, "PAGO ANTERIOR", bordered "ATENCIÓN"
  panel with the warning sentence, security code de-emphasized, no queue pill, no sound.
- **Setup**: privacy disclosure card ("Qué lee Cuadre"), step cards (done / current with
  "AHORA" tag / warning), per-app battery rows with "Arreglar", disabled footer CTA until done.

## Voice
Peruvian Spanish, plain words: "Pago recibido", "Hoy", "Cuadre del día", "Arreglar". No jargon.
