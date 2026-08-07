# Phase 1 Conversion Pack — juhousing-web

Homepage conversion improvements. Apply on latest `juhousing-web` `main`.

## Changes

1. **Slim consult modal (1st step)** — visible fields: name / phone / region only. Email, pyeong, budget, message stay as hidden defaults; webhook payload keys unchanged.
2. **Anxiety copy** — mid/final CTA emphasize “도면이 없어도 됩니다” and “1차 참고 견적”.
3. **Mobile CTA bar** — sticky phone + consult; show header consult earlier; hide estimate-mobile / floating call when bar is active.
4. **CTA focus** — remove per-card consult buttons (`competency_card`); keep hero / mid / final / nav paths.

## Files

- `html/index.html`
- `html/js/site-render.js` (must match SSR — otherwise client re-render restores card CTAs)
- `html/site-content.json` (`midCta` / `finalCta`)

## Apply

```bash
curl -fsSL https://raw.githubusercontent.com/kansin2804-cyber/brain/cursor/phase1-conversion-handoff-e7e5/patches/juhousing_phase1_conversion.patch | git apply
```

Or copy files from this folder into the repo root (`html/...`).

## Verify

- Homepage: consult modal shows only 이름/연락처/지역
- Mobile width: bottom bar with 전화 + 상담
- Competency cards: no “건축 상담 신청” button
- Mid/final CTA labels include “도면 없이 1차 상담”
- No `2x4` / banned brand terms introduced
- Submit still posts webhook keys: `성함`, `연락처`, `이메일`, `부지위치`, `예산`, `문의내용`, `평수`, …
