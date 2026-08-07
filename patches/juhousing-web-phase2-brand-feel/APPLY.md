# Phase 2 Brand Feel — juhousing-web

Apply on latest `juhousing-web` `main` (after Phase 1).

## Changes

1. **Punchier hero** — stronger brand line (“제이유 하우징”), shorter headline, white primary CTA (“도면 없이 1차 상담”)
2. **Less navy/gold** — gold accents removed; philosophy/mid panels shift to stone neutrals
3. **Fewer motions** — softer reveal, no card lift/scale zoom, no CTA scale bounce
4. **More whitespace** — about/philosophy section padding increased
5. **Less CTA clutter** — remove about/philosophy consult buttons; keep hero / mid / final / nav / mobile sticky

## Files

- `html/index.html`
- `html/js/site-render.js`
- `html/site-content.json`

## Apply

```bash
curl -fsSL https://raw.githubusercontent.com/kansin2804-cyber/brain/cursor/phase2-brand-feel-handoff-e7e5/patches/juhousing_phase2_brand_feel.patch | git apply
```

Or copy files from this folder into the repo root.

## Verify

- Hero brand “제이유 하우징” is large; headline “골조가 정밀하면…”
- No gold `#D4AF37` / `#c4a35a` on homepage
- About/philosophy sections have no consult open buttons
- Cards do not jump/scale on hover
