# brain — JU Housing HQ Agent Knowledge

제이유 하우징(JU Housing) HQ 멀티 에이전트용 **지식 코퍼스**입니다.  
Control Tower(로컬 AI)가 이 Markdown을 읽어 견적·현장·마케팅을 조율합니다.

> 이 레포에는 실행 앱/서버가 없습니다. 웹사이트 소스는 [`juhousing-web`](https://github.com/kansin2804-cyber/juhousing-web)을 보세요.

## 빠른 시작

```bash
# 의존성 (품질 검사용)
npm install

# 지식베이스 품질 검사 (금지표현·구조·매니페스트)
npm run check

# Control Tower 로드 순서 (shared → HQ/chief → specialist)
python3 scripts/load_control_tower.py --agent ju_orchestrator --format paths
python3 scripts/load_control_tower.py --domain estimate --with-examples handoff --format json
python3 scripts/load_control_tower.py --agent ju_chief_field --with-examples daily_report --format bundle

# 마크다운 스타일 린트 (선택)
npm run lint
```

### Control Tower 지식 로드

`scripts/load_control_tower.py`는 `knowledge/manifest.json`을 읽어 **shared 먼저**, 이어서 HQ/chief → specialist/guide 순으로 경로를 만듭니다.

| 플래그 | 의미 |
|--------|------|
| `--agent` | 로드할 에이전트 ID (필수, 또는 `--domain`) |
| `--domain` | `estimate` / `field` / `marketing` / `shorts` 본부장(또는 도메인 루트) |
| `--format paths\|json\|bundle` | 경로 목록 / JSON / 프롬프트 합본 |
| `--with-examples` | `handoff,synthesis,daily_report,shorts_plan,brand_guard,all` |
| `--examples-limit` | 그룹당 최대 예시 수 (기본 2) |
| `--self-check` | shared-first · orchestrator→chiefs 포함 자가검증 |

자세한 규칙: [`AGENTS.md`](AGENTS.md)
## 디렉터리 구조

```text
knowledge/
  core/        # HQ 총괄 오케스트레이터
  estimate/    # 견적 본부
  field/       # 현장 본부
  marketing/   # 마케팅 본부
  shorts/      # Shorts 파이프라인
  shared/      # 회사·시공·비주얼 공통 규칙
  manifest.json
templates/     # HANDOFF / 일보 / 쇼츠 표준 JSON·MD 템플릿
schemas/       # JSON Schema
examples/      # few-shot 예시 JSON
scripts/       # 품질 검사·Control Tower 로드 순서
```

## 에이전트 맵

```text
ju_orchestrator (HQ 총괄)
├── ju_chief_estimate
│   ├── ju_consultation
│   ├── ju_compare_ab
│   └── ju_pricing_review
│   (+ ju_estimate_guide)
├── ju_chief_field
│   ├── ju_daily_report
│   ├── ju_schedule
│   └── ju_quality
│   (+ ju_field_ops)
└── ju_chief_marketing
    ├── ju_marketing_playbook / ju_blog_seo
    ├── ju_brand_guard
    └── ju_shorts_planning → ju_shorts_editing → ju_visual_director

shared (전 도메인 공통)
├── ju_company_core
├── ju_construction_basics
└── ju_visual_rules
```

상세 ID·파일 경로·입출력은 [`knowledge/manifest.json`](knowledge/manifest.json)을 정본으로 사용하세요.

## 사용 방법 (Control Tower / 에이전트)

1. **공통 규칙 먼저 로드** — `knowledge/shared/*.md`
2. **도메인 본부장 로드** — estimate / field / marketing
3. **하위 전문가 로드** — 필요한 세부 파일만
4. **산출은 템플릿 준수** — `templates/` 의 JSON 스키마
5. **HQ 통합** — 본부장 `HANDOFF_JSON` / `SYNTHESIS_JSON`을 `ju_orchestrator`가 병합

표준 템플릿:

| 용도 | 파일 |
|------|------|
| 본부장 → HQ handoff | [`templates/handoff.json`](templates/handoff.json) |
| HQ 통합 결과 | [`templates/synthesis.json`](templates/synthesis.json) |
| 작업일보 | [`templates/daily_report.json`](templates/daily_report.json) |
| 쇼츠 5줄 기획 | [`templates/shorts_plan.json`](templates/shorts_plan.json) |
| 브랜드 감수 | [`templates/brand_guard.json`](templates/brand_guard.json) |

## 품질 자동화

`npm run check`가 수행하는 일:

- 매니페스트에 등록된 지식 파일 존재 여부
- 금지 표현(한옥·중목·통나무집·최저가·검증 없는 절약률 등) 스캔
- 필수 섹션 헤더 존재 여부
- 템플릿 JSON이 스키마에 맞는지 검증

금지 목록 정본: [`scripts/banned_terms.json`](scripts/banned_terms.json)  
(회사 규칙과 동기: `knowledge/shared/ju_company_core.md`, `ju_visual_rules.md`)

## 콘텐츠 작성 규칙

- 한국어 단정·전문가 톤. 과장·허위 통계 금지.
- 시공 정체성: **2x6 + OSB + platform framing**, 세라믹/섬유시멘트, 스탠딩심 메탈.
- 마케팅·이미지·대본은 `ju_brand_guard` / `ju_visual_rules`를 통과해야 함.
- 새 에이전트 파일을 추가하면 **반드시** `knowledge/manifest.json`도 갱신.

## 관련 시스템 (이 레포 밖)

| 시스템 | 역할 |
|--------|------|
| Control Tower | 로컬 AI 오케스트레이션 |
| HQ 통합 견적 v5 | 견적 A/B·PDF |
| 작업일보 PC / field | 현장 일보 |
| juhousing.co.kr | 공개 웹·상담 CTA |
| Imagen / 쇼츠 디렉터 | Shorts 비주얼 |
