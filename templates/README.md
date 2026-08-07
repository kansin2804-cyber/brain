# Templates

Control Tower / 도메인 에이전트가 주고받는 **표준 산출물**입니다.  
스키마는 [`../schemas/`](../schemas/)를 보세요.

| 파일 | artifact | 용도 |
|------|----------|------|
| `handoff.json` | `HANDOFF_JSON` | 본부장 → HQ 총괄 |
| `synthesis.json` | `SYNTHESIS_JSON` | HQ 통합 결과 |
| `daily_report.json` | `DAILY_REPORT` | 작업일보 |
| `shorts_plan.json` | `SHORTS_PLAN` | 쇼츠 5줄·5컷 |
| `brand_guard.json` | `BRAND_GUARD` | 브랜드 감수 |

빈 필드는 예시 기본값입니다. 에이전트는 동일 키를 채운 JSON을 반환하세요.
