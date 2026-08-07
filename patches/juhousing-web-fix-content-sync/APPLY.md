# Fix: live site-content.json stale vs homepage SSR

## Bug
HTML SSR is correct (Phase 1–3), but live `site-content.json` is an older marketing copy.
`site-render.js` overwrites title/hero/FAQ on load → users briefly see good copy, then old copy.

## Observed (live vs GitHub main)
- Live JSON title: `제이유 하우징 | 1mm의 정밀함...`
- Main JSON title: `가평·양주·화성·양평 목조주택 시공 | 제이유 하우징`
- Live FAQ starts with old items / duplicates; main FAQ starts with “도면이 없어도…”

## Fix
1. Redeploy `juhousing-web` `main` so `html/site-content.json` on server matches GitHub.
2. Apply cache-bust patch (`site-render.js` / `index.html` → `?v=20260807d`) so browsers refetch JSON.

## Apply patch (cache bust)
```bash
curl -fsSL https://raw.githubusercontent.com/kansin2804-cyber/brain/cursor/fix-content-json-sync-handoff-e7e5/patches/juhousing_fix_content_json_sync.patch | git apply
```

## Verify after deploy
```bash
curl -s https://juhousing.co.kr/site-content.json | python3 -c "import sys,json;d=json.load(sys.stdin);print(d['meta']['title']);print(d['hero']['title']);print(d['faq']['items'][0]['question'])"
```
Expect intent title, “골조가 정밀하면”, FAQ0 “도면이 없어도…”.
