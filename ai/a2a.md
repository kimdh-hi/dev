# a2a

## Task

- https://a2a-protocol.org/latest/topics/life-of-a-task/
- A2A 에서 클라이언트가 메세지를 보내면 수신한 원격의 에이전트(서버)는 `Message` 또는 `Task` 를 응답
- `Message` 응답은 무상태 응답으로 추가적인 상태를 가지지 않음. 응답 그대로 끝
- Task 는 고유 id(task.id)와 상태(state)를 통해 정의된 생명주기에 따라 추가 입력을 받거나, 처리중 중간 응답을 주는 등 작업 완료 전 중간단계를 처리 가능

### 응답 sample

```java
// Message
{
  "jsonrpc": "2.0",
  "id": "req-1",
  "result": {
    "message": {
      "messageId": "b4d1a5e6-0c2f-4a77-9c33-1e9f2b6a7d10",
      "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
      "role": "ROLE_AGENT",
      "parts": [{ "text": "안녕하세요. 어떤 문서를 요약할까요?" }]
    }
  }
}

// Task (입력을 요구)
{
  "jsonrpc": "2.0",
  "id": "req-2",
  "result": {
    "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
    "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
    "status": {
      "state": "TASK_STATE_INPUT_REQUIRED",
      "timestamp": "2026-09-17T04:11:02.008Z",
      "message": {
        "messageId": "2f6b8c1d-9e04-4a3f-bb27-5d0c7a1e4f88",
        "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
        "taskId": "de38c76d-d54c-436c-8b9f-4c2703648d64",
        "role": "ROLE_AGENT",
        "parts": [{ "text": "보고서 전체를 요약할까요, 3분기 부분만 요약할까요?" }]
      }
    }
  }
}

// Task (정상 답변 완료)
{
  "id": "de38c76d-d54c-436c-8b9f-4c2703648d64",
  "contextId": "c295ea44-7543-4f78-b524-7a38915ad6e4",
  "status": {
    "state": "TASK_STATE_COMPLETED",
    "timestamp": "2026-09-17T04:12:33.120Z"
  },
  "artifacts": [
    {
      "artifactId": "artifact-1",
      "parts": [{ "text": "3분기 매출 12% 증가 ..........~~" }]
    }
  ]
}
```

### Task 구조

- `id`(required): 새 task 에 대해 수신측이 생성하는 고유 식별자 (taskId)
- `contextId`: 여러 task와 message 를 묶는 식별자 (한 개 contextId는 n개 id를 포함)
- `status`(required): 현재 상태 (`state`, `message` 포함)
- `artifacts`: Task 의 출력 산출물 (Artifact[])
- `history`: Task 진행중 주고받은 이력 (Message[])

### status

- state 와 message 를 포함하며, state 로 현재 수신측 에이전트의 상태 및 요구사항을 표현
- message 는 optional 로 state 에 대한 부가적인 설명이 필요한 경우 작성

#### state

- https://a2a-protocol.org/latest/specification/#413-taskstate

| 값 | 분류 | 설명 |
| --- | --- | --- |
| `TASK_STATE_UNSPECIFIED` | - | unknown |
| `TASK_STATE_SUBMITTED` | 진행 | 성공적으로 제출되어 접수 확인된 상태 |
| `TASK_STATE_WORKING` | 진행 | 에이전트가 처리 중 |
| `TASK_STATE_INPUT_REQUIRED` | 중단 (interrupted) | 진행하려면 추가 사용자 입력이 필요 |
| `TASK_STATE_AUTH_REQUIRED` | 중단 (interrupted) | 진행하려면 인증이 필요 |
| `TASK_STATE_COMPLETED` | 종료 (terminal) | 성공적으로 완료 |
| `TASK_STATE_FAILED` | 종료 (terminal) | 오류로 인한 종료 |
| `TASK_STATE_CANCELED` | 종료 (terminal) | 완료 전에 취소됨 |
| `TASK_STATE_REJECTED` | 종료 (terminal) | 에이전트가 수행하지 않기로 결정 |

### artifacts

- task 작업의 결과물을 포함
    - status.message 에 넣으면?
    - status.message 는 현재 상태(state.status) 에 대한 진행 안내, 입력 요청 등의 목적으로 사용되어여 함 (SHOULD NOT spec)
    - https://a2a-protocol.org/latest/specification/#37-messages-and-artifacts
- 반드시 TASK_STATE_COMPLETED state 에서만 artifacts 에 결과물이 포함되는 것은 아님.
- 예를 들어 task 의 결과 초안을 n개 만들고 client 로부터 선택을 받아야 하는 경우
    - artifacts: 초안 n개 포함
    - status.state: TASK_STATE_INPUT_REQUIRED
    - status.message: "어느 방향으로 진행할까요?"
    - client 는 동일 taskId로 답변
    - 초안중 결정된 안을 artifacts 로 응답 (status.state: TASK_STATE_COMPLETED)