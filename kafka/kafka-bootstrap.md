# kafka bootstrap

## bootstrap

- producer/consumer 가 topic 으로 메세지 발행 위해 리더 브로커 정보를 얻는 과정
- 한 개 broker 로부터 클러스터 전체 메타데이터 수신
    - 리더 브로커 목록 포함
    - 토픽, 파티션, 각 파티션 리더 브로커 …..
- 리더 브로커 식별

```yaml
spring:
  kafka:
    bootstrap-servers:
      - [broker1 info]
      - [broker2 info]
```

- 고가용성 위해 n개 브로커 모두 명시
- 어떤 것이 리더 브로커인지는 판단할 필요 없음
- bootstrap 은 최초 연결시 수행되고, 이우헤는 캐싱된 메타데이터 사용
    - **`metadata.max.age.ms`**  default 300_000ms (5분)