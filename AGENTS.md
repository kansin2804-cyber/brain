# brain

Knowledge corpus for the JU Housing (제이유 하우징) HQ multi-agent "brain". Content lives under `knowledge/` and is consumed by an external local AI system ("Control Tower"), which is not part of this repository.

## Cursor Cloud specific instructions

This repository is **content + light automation**: Markdown knowledge under `knowledge/`, plus `templates/`, `schemas/`, and `scripts/check_knowledge.py`. There is no application server to start.

- Install: `npm install` (markdownlint only). Python 3 stdlib is enough for `npm run check`.
- Quality gate (preferred): `npm run check` — manifest integrity, banned-terms scan, required sections, template schema validation.
- Optional style lint: `npm run lint` (config: `.markdownlint-cli2.jsonc`). Existing compact Markdown may still report blank-line style findings; do not mass-reformat unless explicitly asked.
- Agent map / file inventory: `knowledge/manifest.json` is the source of truth. Update it when adding or renaming knowledge files.
- Standard agent artifacts: `templates/*.json` validated by `schemas/*.schema.json`.
- Banned marketing/visual terms: `scripts/banned_terms.json` (allow-listed in the rule docs themselves).
- Website work belongs in `juhousing-web`, not this repo.
