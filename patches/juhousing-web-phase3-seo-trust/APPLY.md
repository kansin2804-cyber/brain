# Phase 3 SEO / Trust — juhousing-web

Apply on latest `juhousing-web` `main` (after Phase 1–2).

## Changes

1. **Intent title** — `가평·양주·화성·양평 목조주택 시공 | 제이유 하우징`
2. **FAQ** — region / budget / process intents (8 items); SSR + FAQPage JSON-LD synced
3. **JSON-LD** — `HomeAndConstructionBusiness` enriched + `Service` schema; FAQPage updated
4. **NAP checklist** — `docs/SEO_TRUST.md` (Google/Naver Business match table)
5. **Perf** — hero preload, font preconnect, remove hero console logs; sitemap lastmod
6. **Tests** — phase3 intent/FAQ/schema assertions in `test_site_content.py`
7. **Fix** — add `id="ju-faq-list"` / `id="ju-regions-nav"` so `site-render.js` can hydrate

## Apply

```bash
curl -fsSL https://raw.githubusercontent.com/kansin2804-cyber/brain/cursor/phase3-seo-trust-handoff-e7e5/patches/juhousing_phase3_seo_trust.patch | git apply
```

Or copy files from this folder into the repo root.

## Verify

```bash
python3 -m pytest harness/regression/test_site_content.py -q
```

- Homepage `<title>` contains 가평·양주
- FAQ includes “도면이 없어도 1차 상담”
- JSON-LD has Business + Service + FAQPage
- `docs/SEO_TRUST.md` NAP table present

## Manual after deploy

Match Google Business / 네이버 스마트플레이스 NAP to `docs/SEO_TRUST.md`, then resubmit sitemap.
