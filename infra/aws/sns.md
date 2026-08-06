# AWS SNS

## AWA SNS (Simple Notification Service)

- 토픽 pub-sub 기반 메세지 송수신을 위한 매니지드 서비스

```json
1. sns 토픽 생성
- https://docs.aws.amazon.com/sns/latest/dg/sns-create-topic.html

2. 엔드포인트 구독 (subscribe)

3. 해당 topic 으로 메세지 발행

4. 모든 구독자 메세지 수신
```

- 전달 가능 엔드포인트
    - AWS SQS, Lambda, HTTP 엔드포인트, 이메일, 모바일 푸시, SMS…
    - A2A (Application-to-Application) 시스템 간 통합
        - SQS, Lambda, HTTPS …
        - https://docs.aws.amazon.com/sns/latest/dg/sns-system-to-system-messaging.html
    - A2P (APplication-to-Persion) 사람에게 알림
        - 모바일 푸시, sms, 이메일 …
        - https://docs.aws.amazon.com/sns/latest/dg/sns-user-notifications.html

## SNS 사용 패턴

### Fan-out 패턴

- 토픽에 발행된 하나의 메세지가 복제되어 여러 엔드포인트로 푸시되는 시나리오
- 한 개 sns 토픽을 여러 sqs 큐가 구독
- 한 개 이벤트 메세지가 sns 토픽으로 발행되는 경우 n 개 sqs 큐로 fanout

### 애플리케이션 알림

- ec2 auto scaling, s3, cloud watch 등 이벤트를 sns 통해 sms, 이메일 등올 알림

## 토픽 종류

- Standard 토픽, FIFO 토픽
- SQS 와 동일하게 순서 보장, 중복 제거 기능 차이
- SQS 가 FIFO면 SNS 토픽도 FIFO 여야 한다.
    - SQS FIFO 큐는 MessageGroupId, MessageDeduplicationId 가 필수 파라미터 이지만 Standard 토픽의 publish api에는 MessageGroupId 가 없음

| SNS 토픽 | SQS 큐 | 가능여부 | 결과 |
| --- | --- | --- | --- |
| FIFO | FIFO | ✅ | 순서 보장 + 중복 제거 |
| FIFO | Standard | ✅ | 순서·중복 제거 보장 없음, 비용 절감 |
| Standard | Standard | ✅ | 기본 조합 |
| **Standard** | **FIFO** | ❌ | 구독 등록 시 `InvalidParameter` 오류 |

## Price

- https://aws.amazon.com/ko/sns/pricing/
- 메세지 1건 발행당 sns api 요청은 1건
    - 구독자가 SQS, Lambda 인 경우 구독자 수에 관계없이 api호출 수는 0건
    - 모바알 푸시, 이메일, HTTPS 등의 경우 별도 요금 부과
    - 단, 발행된 메세지 크기에 따라 과금 상이

| 메시지 크기 | SNS API 요청 |
| --- | --- |
| 1KB ~ 64KB | 1건 |
| 65KB ~ 128KB | 2건 |
| 129KB ~ 192KB | 3건 |
| 193KB ~ 256KB | 4건 |

## Reference

- https://docs.aws.amazon.com/sns/latest/dg/welcome.html