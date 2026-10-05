# Jev

## Jev?
- 2026/9 미국의 스타트업 TypeSafe AI가 공개한 AI 모델
- 일반적인 LLM 처럼 문장을 생성하지 않고, 미리 정해둔 질문에 대해 정해진 형식의 답만을 반환하는 판정 전용 모델이다.
- 일반적인 LLM 의 응답은 사람이 읽을 텍스트를 생성하기 때문에 코드에서 LLM 응답을 사용하려면 구조화 된 결정을 억지로 출력시키고 파싱하고 검증을 필요로 함
- Jev 는 애플리케이션이 바로 사용할 수 있는 구조화된 결정 사항을 응답한다.

```
일반적인 LLM 방식
- 입력->LLM->문자열 응답->파싱->검증->비즈니스 로직

Jev 방식
- 입력->Jev->타입이 정해진 판단->비즈니스 로직
```

## Jev State, Questions
- 요청은 두 부분을 구성
- state: 판단 대상 데이터
- questions: 데이터에 묻고 싶은 질문 목록

```
{
  "model": "jev-latest",
  "state": { ... }, // 판단 대상 데이터
  "questions": { ... } // 질문들
}
```

### state
- 문자열, 객체(json object), 배열 포맷 지원
- 객체 사용을 권장
- state 에는 관련된 컨텍스트만 포함시키는 것을 권장하므로 무관한 내용이 늘어날수록 정확도가 떨어지는 것에 유의해야 함.
- 결제 관련 질문이 필요한 경우 해당 고객의 db 데이터만 조회해서 state 에 구성시키는 것을 권장

### questions
- id
  - 예제의 role, experience, meets_requirement 가 id 에 해당
  - id 자체는 모델에 전달되지 않고 단순히 코드에서 사용하기 위함이므로 id 로 설명이 표현되더라도 instructions 에 설명을 명시해야 함.
  - 응답에서 답을 꺼낼 때 id 로 참조
  - `result.answers["role"].choice`
- instructions: 실제 질문 
- instructions 에 대한 질문 종류 (Noul, Choice, Score)
  - Noul: yes or no
  - Choice: 보기 중 하나
  - Score: 단계 중 하나
- criteria: 선택 가능한 보기
  - choice(필수), score(필수), noul(optional) 에서 사용 가능
  - choice: 선택지
  - score: 단계
  - noul: yes, no 의 의미를 명확히하는 용도로 사용 가능

```
result = client.system_one(
    state={
        "job_posting": "백엔드 개발자. Python 실무 경험 필수.",
        "resume": "3년간 Django로 쇼핑몰 서버를 개발하고 운영했습니다.",
    },
    questions={
        "role": Choice(
            instructions="`resume`의 경력은 어느 직군에 가까운가?",
            criteria={
                "backend": "서버, API, 데이터베이스 개발",
                "frontend": "웹 화면, UI 개발",
                "data": "데이터 분석, 머신러닝",
                "other": "위에 해당하지 않음",
            },
        ),
        "experience": Score(
            instructions="`resume`에 나타난 Python 경험 수준은?",
            criteria=["경험 없음", "학습 수준", "실무 사용", "깊은 전문성"],
        ),
        "meets_requirement": Noul(
            instructions="`resume`가 `job_posting`의 필수 조건을 충족하는가?"
        ),
    },
)
```

### Noul 응답
- yes or no 에 대한 질문이지만 답변은 확률로 온다.

```
{"noul": 0.97}
```

### choice, score - probabilities, confidence
- 각 선택지에 매긴 확률
- choice 의 경우 probabilities 중 가장 높은 값이 선택된 결과

```
"probabilities": {"technical": 0.85, "billing": 0.15, "sales": 0.0}
```
- probabilities 는 선택지가 3 개인 경우 모델이 결과를 전혀 모르겠는 경우 0.33으로 각 선택지에 나눠진다.
- 위 경우 모델의 choice 응답을 신뢰하기 어렵고 confidence 는 0이 왼다.
- 1.0 에 가까워 지는 경우 즉, 특정 choice 항목의 확률이 1이 되는 경우 그 응답의 신뢰도는 높다고 판단할 수 있고 이 때 confidence는 1.0이다. 


## reference
- https://docs.typesafe.ai/introduction
- https://docs.typesafe.ai/concepts/state
- https://docs.typesafe.ai/primitives
- https://docs.typesafe.ai/confidence
- https://typesafe.ai/blog/introducing-system-one-models-and-jev
- https://wikidocs.net/432835