# AWS SQS

## aws sqs (simple queue service)

- managed queue service
- `producer -HTTPS-> SQS <-HTTPS(polling)- consumer`
    - consumer 측에서 polling 통해 메세지 수신 후 명시적으로 message delete https 호출 필요 (ack)
    - 단, lambda 함수가 SQS 큐 구독시 lambda 가 consumer 로써 동작하는 경우 DeleteMessage 자동 처리
- 프로듀서, 컨슈머는 별도 브로커 등을 세팅할 필요없이 awa sqs 통해 api 호출만으로 메세지 송수신 가능 (결합도 낮춤)

### consumer polling

- sqs 컨슈머는 기본적으로 polling 기반으로 메세지를 수신 (ReceiveMessage)
- short polling, long polling
    - short polling (default): 특정 주기없이 지속적으로 sqs 측으로 ReceiveMessage 요청
        - sqs 는 api 호출당 과금 모델이므로 비쌈
    - long polling: ReceiveMessage 호출 시 메세지가 없는 경우 지정된 주기만큼 대기 후 메세지가 있는 경우 응답
- long polling 설정방법
    - 큐 속성: ReceiveMessageWaitTimeSeconds
    - 컨슈머 호출 파라미터: WaitTimeSeconds
    - 컨슈머 측 설정이 우선순위를 가진다.
- **long polling 권장.**
    - tcp 연결을 유지할 수 없는 경우, 컨슈머 측 요청 스레드가 점유되는 것이 부담스러운 경우 예외적으로 short polling 선택 가능

### Queue

- Standard 큐
- FIFO 큐

#### Standard 큐

- SQS 기본 큐 타입
- at-least-once 보장
    - aws sqs는 분산 아키텍처이므로 메세지 복사본이 두 번 이상 전달될 수 있음
    - 순서 보장X
    - 단, best-effort..
- `SendMessage` 시 SQS 는 응답반환 전 여러 AZ 에 메세지 중복 저장
    - AZ 단위 이중화로 인해 고가용성 확보
    - 중복전달, 순서보장x 의 이유
    - 삭제 요청이 일부 서버에 도달 전에 다시 조회될 수 있는 등..
- 중복 메세지 발생 가능하므로 컨슈머 측은 멱등성 고려해서 설계 필요
- 순서가 중요한 경우 FIFO 큐 사용

#### FIFO 큐

- Standard 큐의 모든 기능을 지원하면서 메세지 순서와 중복 제거 등의 추가 기능을 지원
- 메세지 송수신 간 순서 유지
- 메세지 송신간 중복 메세지 발송 방지
- 큐의 이름은 `.fifo` 접미사로 끝나야 함
- 
- `MessageDeduplicationId`
    - 중복 메세지 전달 방지를 위한 토큰 (프로듀서 측에서 sendMessage 시 설정)
    - 프로듀서는 fifo 큐 메세지 발송시 MessageDeduplicationId 지정 가능 (조건부 필수)
        - 지정하지 않는 경우 SQS 내부적으로 본문 해시(SHA-256) 해서 MessageDeduplicationId 로 지정 (ContentBasedDeduplication 활성화 필요)
    - sqs 내부적으로 동일 MessageDeduplicationId 에 대해서는 5분간 중복 전송 무시
    - 프로듀서 측에서 중복 메세지 발송을 방지하는 것이고, 컨슈머 측에서 중복 수신을 방지하는 장치는 아님

### 큐 단위 TTL 지정 가능

- MessageRetentionPeriod
    - default: 4일
    - min: 60s
    - max: 14d
- TTL 경과시 메세지 자동 삭제

### visibility timeout

- polling 에 의해 컨슈머가 메세지를 획득한 경우 DeleteMessage 전까지 다른 컨슈머에게는 일시적으로 보이지 않게됨
- visibility timeout default: 30s (0s ~ 12h 커스텀 가능)
- 주의: 컨슈머 측 처리 시간이 visibility timeout 를 초과하는 경우 다른 컨슈머에서 중복처리 될 수 있음
    - 컨슈머 처리에 따라 visibility timeout 적절히 설정 필요

### QoS

- pull(polling) 기반이므로 브로커 측에서 흐름제어가 필요한 QoS 개념 자체가 없음

### 메세지 batch pull

- 컨슈머 별 한 번에 가져올 메세지 수 지정 가능 (MaxNumberOfMessages)
    - default:1, max: 10
- 주의: 10 으로 설정시 visibility timeout 이 10s 인 경우 첫번째 메세지 처리에 10s 가 소요된 경우 이후 모든 메세지는 모두 중복처리될 수 있음

### DLQ

- 처리 실패 메세지를 격리하는 별도 큐
    - SQS 내부적으로 자동 처리
- 무한 재시도 차단, 실패 매세지 보존/분석, 정상 처리 방해 방지 위함
- 큐 단위로 dlq 설정 가능 (`RedrivePolicy` 속성)
    - dlq 는 자동생성되지 않음

```json
{
  "deadLetterTargetArn": "arn:aws:sqs:ap-northeast-2:1234:orders-dlq",
  "maxReceiveCount": "5"
}
```

- maxReceiveCount
    - 원본큐 설정값 (DLQ 에 설정하는 값 아님)
    - maxReceiveCount 만큼 메세지가 pull 되고도 삭제되지 않은 경우 DLQ 로 이동
    - 실제 재시도 원하는 횟수 + 1 만큼 maxReceiveCount 설정
    - 권장: 3~5
    - 주의: aws콘솔에서 메세지를 조회하는 것도 maxReceiveCount 증가됨
- RedriveAllowPolicy
    - DLQ 측에 설정
    - 어떤 원본큐가 이 DLQ 를 사용할 수 있는지
    - options: allowAll, byQueue, denyAll
    - 여러 원본큐가 DLQ 의도치 않게 공유하는 것 방지 위함
- 보존 기간
    - Standard
        - 만료에 대한 시점은 원본 큐에 들어온 시점 기준
        - 만료기간(MessageRetentionPeriod) 는 원본큐, dlq 각각 다르게 설정 가능
        - 원본큐: 5일, dlq: 10일 설정한 경우
        - 원본큐에 3일 보관 후 dlq 로 보내진 경우 dlq 에서는 10일이 아닌 원본큐 3일 체류기간을 뺀 7일만 보존
    - fifo 큐
        - fifo 큐의 경우 dlq 에 enqueue 시 enqueue timestamp 리셋
        - 원본큐가 fifo 큐이면, dlq 도 fifo 큐 필수

## Price

- https://aws.amazon.com/ko/sqs/pricing/
- 요청수 기반 과금
    - 1요청 == api 1회 호출
    - 한 개 메세지 처리시 최소 3회 이상 api 호출 필요
    - sendMessage, receiveMessage(polling), deleteMessage
- polling 의 경우 메세지가 없는 경우에도 호출되므로 요청수는 누적됨.
- 매월 100만 건 sqs 요청 무료 제공
- 100만 건 당 평균 USD 0.40

## reference

- claude
- Amazon Simple Queue Service Amazon SQS visibility timeout - Amazon Simple Queue Service
- https://aws.amazon.com/ko/sqs/pricing/