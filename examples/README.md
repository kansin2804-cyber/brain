# Few-shot examples

Control Tower / 도메인 에이전트가 산출물을 만들 때 참고하는 **완성 예시**입니다.  
빈 스키마는 [`../templates/`](../templates/), 규칙은 [`../schemas/`](../schemas/).

## 사용법

1. 해당 도메인 지식(`knowledge/…`)을 로드
2. 아래 예시 중 **가장 가까운 시나리오 1개**를 컨텍스트에 포함
3. 동일 artifact 키로 JSON 반환
4. 마케팅·쇼츠는 `brand_guard` 예시도 함께 참고

## 목록

| 경로 | artifact | 시나리오 |
|------|----------|----------|
| `handoff/estimate_ab_ready.json` | HANDOFF_JSON | 견적 A/B 비교 완료 → HQ |
| `handoff/field_scope_change.json` | HANDOFF_JSON | 현장 이슈로 견적 범위 변경 가능 |
| `handoff/marketing_pipeline.json` | HANDOFF_JSON | 쇼츠·블로그 파이프라인 상태 |
| `synthesis/hq_weekly_priorities.json` | SYNTHESIS_JSON | HQ가 충돌 정리 후 우선순위 발행 |
| `daily_report/framing_clear_day.json` | DAILY_REPORT | 골조 정상 일보 |
| `daily_report/roof_delay_rain.json` | DAILY_REPORT | 우천 지연 + estimate 알림 |
| `shorts/insulation_myth.json` | SHORTS_PLAN | 단열 오해 해제 쇼츠 |
| `shorts/build_sequence.json` | SHORTS_PLAN | 공사 순서 5컷 |
| `brand_guard/reject_lowest_price.json` | BRAND_GUARD | 가격 과장 문구 반려 |
| `brand_guard/approve_process_copy.json` | BRAND_GUARD | 공정 투명 카피 승인 |

## 작성 원칙

- 스터드는 반드시 **2x6** (2x4 금지)
- 한옥·중목·통나무집·검증 없는 %절약·최저가 금지
- 한국어 고객 멘트는 짧고 단정하게
- 쇼츠 `overlay_lines`는 5줄, 이미지 프롬프트는 영문 5컷
