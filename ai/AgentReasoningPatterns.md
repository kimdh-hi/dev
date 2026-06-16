# Agent Reasoning Patterns

## Agent Reasoning Patterns

- ReAct
- PreAct
- Plan-and-Execute
- ReWOO (Reasoning Without Observation)
- LLMCompiler

### Langgraph Plan-and-Execute Agent 3가지 패턴

- https://www.langchain.com/blog/planning-agents (2024)
- pattenrs
    - Plan-and-Execute
    - ReWOO
    - LLMCompiler
- Plan-and-Execute
    - https://arxiv.org/abs/2305.04091
    - plan → 순차적으로 step execute → replan
    - plan 에만 좋은 모델 사용 실제 step 실행은 낮은 모델 사용하여 비용 절감
- ReWOO (Reasoning Without Observation)
    - https://arxiv.org/abs/2305.18323
    - plan(전체 계획 수립) → 전체 step execute → execute 결과 정리
    - replan 과정을 제거하여 토큰 비용르 최소화한다.
    - 계획, 결과 정리에만 좋은 LLM 모델 2번 호출
    - 각 step 에서는 worker 가 tool 실행 혹은 필요하다면 sub llm 호출 수행
- LLMCompiler
    - https://arxiv.org/abs/2312.04511
    - DAG(방향성 비순환 그래프) 수립 → 각 작업 병렬 실행 → 결과가 충분한지 판단 → loop/end
    - 병렬처리 가능한 부분은 병렬처리 수행하여 응답속도 개선
    - 병렬실행 완료 후 `Joiner` 가 결과 충분여부 판단하여 재계획(loop) 또는 응답 처리

## PreAct

- https://arxiv.org/abs/2505.09970

```python
1. step 수립 (순차 plan: s1 → s2 → ... → sn → s_fa)
1-1. 각 step 은 최대 1개 action(tool 또는 generate) 소유
1-2. ★각 step에 상세 reasoning 포함★
2. 순차적으로 step 실행
3. 실행결과(observation) 누적 → 다음 계획 갱신(refine)
5. 마지막 step에서 최종 응답
```

- Plan-and-Execute, PreAct 차이
    
    
    |  | 초기 전체 계획 | 매 스텝 reasoning 범위 | 계획 재검토 타이밍 | 재검토 내용 |
    | --- | --- | --- | --- | --- |
    | **ReAct** | ❌ 없음 | 바로 다음 액션 1개 (근시안적) | 매 스텝 (단, "플랜"이 아니라 다음 행동 결정) | 누적된 thought+관찰 기반으로 **다음 액션 하나** 결정 |
    | **Plan-and-Execute** | ✅ 처음에 1회 수립 | 없음 (계획대로 실행만) | 모든 스텝 끝난 후 1회 | 끝낼지 / 추가 플랜 짤지 |
    | **Pre-Act** | ✅ 있음 (매 스텝 갱신) | 전체 멀티스텝 플랜을 매번 재생성 | 매 스텝 실행 직후 | 관찰 결과 + reasoning 누적해서 플랜 전체 갱신 |