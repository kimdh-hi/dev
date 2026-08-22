# redis pub-sub

## redis pub-sub

- redis pub-sub은 `subscribe`, `unsubscribe`, `publish` 명령으로 구현되는 발행/구독 메세징 방식
    - redis 코어 내장 기능
    - shareded pub/sub(`SPUBLISH`, `SSUBSCRIBE`) 정도만 redis 7.0 부터 사용 가능
- 발행자는 `특정 수신자를 지정해서 메세지를 보내지 않고`, 메세지는 채널로 분류되어 발행되며 발행자는 `구독자가 존재여부를 알 필요없음`.
- publish 시 채널 대상으로 메세지를 발행시 해당 메세지는 `특정 공간에 저장되지 않고` 그 순간 `구독중인 subscriber 쪽으로 전달`만 한다.
    - `구독자가 없는 경우` 그냥 버려지고 publish 는 0을 반환
    - 저장되지 않으므로 TTL 개념 없음
- redis pub/sub 은 브로드캐스트
    - 한 채널에 대해 10개 구독자가 있는 경우 구독자 전원 메세지 수신

### 사용법

```
# SUBSCRIBE [channel] [channel ...]
SUBSCRIBE ch1 ch2

# PUBLISH [channel] [message]
PUBLISH ch1 Hello
```

- publish 시 응답은 메세지가 전달된 구독자 수

## 구독 상태 제약

- 구독중인 커넥션은 구독 전용 상태가 된다.
- 하나 이상의 채널을 구독한 클라이언트는 `SUBSCRIBE`/`UNSUBSCRIBE` 류만 가능
    - `PING`, `PSUBSCRIBE`, `PUNSUBSCRIBE`, `QUIT`, `RESET`, `SSUBSCRIBE`, `SUBSCRIBE`, `SUNSUBSCRIBE`, `UNSUBSCRIBE`
- 구독용 커넥션과 일반 명령용 커넥션 분리 필요
    - 대부분의 클라이언트 라이브러리는 별도 커넥션을 만듬
- redis-cli 구독모드에서는 `UNSUBSCRIBE` 명령 불가
    - ctrl-c 로만 벗어날 수 있음

## 패턴 구독 (PSUBSCRIBE)

- redis pub-sub 은 패턴 매칭 지원
- 클라이언트는 glob 스타일 패턴을 구독해서 패턴 매칭되는 모든 채널로 보낸 메세지 수신 가능

```kotlin
PSUBSCRIBE channel.*
```

## 전달 보장

- `at-most-once`
- 최대 한 번 전달
- 이력 저장, 재처리, ACK 등 개념 없음
- 강한 전달 보장이 필요한 경우 `redis-streams` 참고
    - https://redis.io/docs/latest/develop/data-types/streams/

## Sharded Pub/Sub

- redis cluster 에서 일반 pub/sub 문제
    - 채널은 키가 아니기 때문에 특정 slot 에 할당되지 않음
    - 특정 노드에 publish 된 메세지는 cluster 내 모든 노드로 메세지를 전파한다.
    - 구독자 입장에서 어떤 노드에 커넥션을 열고 구독을 하든 다 수신할 수 있다는 편의가 있지만 클러스터 버스 통해 메세지가 전파되는 것 자체가 비효율
- redis 7.0 에 도입된 sharded pub/sub 은 채널을 슬롯에 배정한다.
    - 특정 노드로 `SPUBLISH` 시 해당 샤드 내에서만 전파
    - 마스터의 수가 많을수록 메세지 발행시 전파량 감소
    - 단, `PSUBSCRIBE` 시 특정 샤드 내 노드 중 하나를 대상으로 커넥션을 열어야 메세지 수신 가능
    - 각 샤드의 한 개 노드 대상으로 커넥션을 열고 구독해야 누락없이 메세지 수신 가능
    - 일반 pub/sub 의 편의성을 포기하고, 메세지 전파 량을 줄이는 트레이드오프

## reference

- https://redis.io/docs/latest/develop/pubsub/
- claude