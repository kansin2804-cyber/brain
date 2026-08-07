# Fix: transparent text from ju-reveal

## Bug
`.ju-reveal { opacity: 0 }` hid section copy until IntersectionObserver added `is-visible`. Slow/blocked JS = transparent text.

## Fix
- Default `.ju-reveal` to visible (`opacity: 1`)
- Only hide-until-visible when `html.js-reveal` is set after observer init

## Apply
```bash
curl -fsSL https://raw.githubusercontent.com/kansin2804-cyber/brain/cursor/fix-ju-reveal-handoff-e7e5/patches/juhousing_fix_ju_reveal_visibility.patch | git apply
```
