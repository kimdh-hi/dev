## ReAct (추론(Reason) + 행동(Act))

### 2024년 노벨 물리학상 수상자의 출생지 인구는?

1. "먼저 2024년 노벨 물리학상 수상자가 누구인지 검색해야겠다" (추론)
2. 검색을 실행한다 (행동)
3. "제프리 힌턴이구나" (관찰)
4. "힌턴의 출생지가 어디인지 다시 검색해야겠다" (추론)
5. 검색을 실행한다 (행동)
6. "런던이구나. 런던 인구를 찾아보자" (관찰 + 추론)

### spring ai 는 ReAct 를 제공하지 않는다?

- ReAct 에 대한 원논문에서는 프롬프트에 ReAct 동작을 유도하는 내용이 포함하는 방식을 소개한다. (2022 시기..)
  - `Thought: ... / Action: ... / Observation: ...`
- langchain 의 create_react_agent(=create_agent) 은 위 프롬프트 방식으로 ReAct 를 지원했다.
- 현재 ReAct 는 명시적 프롬프트를 통하지 않고 tool calling loop 가 이를 대체한다.
  - langchain 의 create_react_agent(=create_agent) 도 tool calling loop 만 지원한다.
- spring ai 도 @Tool calling loop 를 동일하게 지원한다.
  - 실행할 tool 이 있을때까지 계속해서 반복
- 즉, spring ai 도 ReAct 를 지원한다.
  - 단, llm model 이 tool calling 을 지원하지 않는 경우 (Ollama..) 프롬프트 기반 ReAct 통한 처리 별도 구현후 작업 필요

### spring ai tool calling loop 관련 개선사항

- @Tool 로써 충분히 ReAct 를 지원하지만 Advisor 를 통해서도 loop 를 제공한다.
- ToolCallAdvisor (https://docs.spring.io/spring-ai/reference/api/tools.html#_advisor_controlled_tool_execution_with_toolcalladvisor)
  - recursive advisor 1.1.0 도입, (2.0.0-M7 default)
  - advisor chain 수준에서 tool loop 동작
  - 툴 호출 사이사이 로깅 및 결과변환 등 가능

```
기존에도 chatModel 내부에서 tool call loop 는 chatModel 내부에서 돌았었음.
다만, ToolCallAdvisor 도입 이후 advisor chain 내에서 tool call loop 를 제어하는 방식으로 바뀐것 뿐.

각 tool call loop 단계마다 로깅, chatMemory 관리, tool 호출동작 커스터마이징 등이 가능해진 것.
```

- tool call loop 는 advisor chain, chatModel 내부 각각 중복으로 도는게 아닌가?
  - 아니다. ToolCallAdvisor 를 사용하는 경우 chatModel 내부에서 tool 실행을 수행하지 않는다.
  - `setInternalToolExecutionEnabled(false)`
  - https://docs.spring.io/spring-ai/reference/api/advisors-recursive.html
    