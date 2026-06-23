# Kafka

## Bootstrap

- producer/consumer 가 topic 으로 메세지 발행 위해 리더 브로커 정보를 얻는 과정
- 한 개 broker 로부터 클러스터 전체 메타데이터 수신 (리더 브로커 목록, 토픽, 파티션, 각 파티션 리더 브로커 등)
- bootstrap 은 최초 연결 시 수행되고, 이후에는 캐싱된 메타데이터 사용
  - `metadata.max.age.ms` default 300,000ms (5분)

```yaml
spring:
  kafka:
    bootstrap-servers:
      - [broker1 info]
      - [broker2 info]
```

- 고가용성을 위해 n개 브로커 모두 명시
- 어떤 것이 리더 브로커인지는 판단할 필요 없음

---

## Consumer Group

- 각 consumer group 은 다른 consumer group 으로부터 격리된 환경에서 운영됨
- consumer group 내 consumer 는 한 topic 의 여러 partition 이 할당될 수 있음
- consumer group 은 같은 목적/기능의 consumer 집합
- 동일 consumer group 내에서는 1개 메세지는 1번만 처리됨 (메세지 중복처리 방지)

**why consumer group**

- a, b, c 각 다른 서버가 운영되는 경우, a 에서 b, c 로 데이터 동기화를 위해 특정 topic 에 메세지 발행
- b, c 에 각각 다른 consumer group 을 할당하는 경우 topic 의 메세지를 격리되어 처리 가능
- 예: b 서버 장애로 처리 못한 경우 c 는 독립적으로 처리 가능, 이후 b 서버가 복구되면 마지막 소비한 메세지로부터 이후 메세지들을 처리 가능 (결과적으로 데이터 동기화 가능)

### MSA 관점에서

- 각 마이크로서비스는 한 개 consumer group
- k8s 환경에서 각 마이크로서비스의 pod(=consumer)는 늘어났다가 줄었다가를 반복할 수 있음
- partition 수는 모든 consumer group 중 가장 많은 consumer 가 필요한 시점을 기준으로 함
  - k8s 기준 HPA maxReplicas 값을 기준으로 partition 수 산정

---

## Partition 수

- topic 내 각 partition 은 consumer group 내 consumer 와 1:1 매핑
- partition 수와 consumer group 내 consumer 수를 동일하게 하면 최대 병렬성을 낼 수 있음
- partition 수는 런타임에 줄일 수 없으므로 오토스케일링에 대비해서 산정해야 함

| 관계 | 결과 | 권장 여부 |
| --- | --- | --- |
| Consumer = Partition | 1:1 매칭, 최대 병렬성 | 이상적 |
| Consumer < Partition | 일부 Consumer가 여러 Partition 담당 | 확장 여지 있음 |
| Consumer > Partition | 일부 Consumer는 idle | 비용 낭비 |

consumer 수와 partition 수는 동일하게 하거나 배수로 설정하는게 가장 이상적 (균등분배, 병렬성)

**partition 수가 지나치게 많은 경우의 문제**

- partition 은 메모리 수준에서 버퍼/메타데이터 등을 유지하기 때문에 메모리 낭비 발생
- rebalance 시 partition 재분배 시간 증가
- 디스크 i/o 효율 저하

### Rebalancing

- topic 내 partition 와 consumer group 내 consumer 간 할당 상태가 변경되는 것
- 리밸런싱이 발생하는 케이스 (consumer 추가/제거)
  - 새로운 consumer 가 추가/제거되는 경우, 최대한 1:1 매칭이 되는 최대 성능을 위해 할당 관계를 변경
  - 특정 consumer 장애 시 해당 consumer 에 매칭된 partition 을 해당 consumer group 내 다른 consumer 로 할당 (consumer 에 대한 failover)
- partition 수가 적은 경우 (< 10?) 에는 리밸런싱 자체가 큰 문제가 되지 않을 수 있지만, partition 수가 많아지는 경우 (< 100?) 리밸런싱 과정 자체를 장애로 볼 수 있을 정도의 문제가 될 수 있음

---

## Consumer Lag

- producer 와 consumer 의 데이터 처리량의 차이
- producer 가 데이터를 저장한 offset(`log-end-offset`) 과 consumer 가 가리키고 있는 offset(`consumer-current-offset`) 의 차이
- consumer application 이 정상동작하는지를 모니터링 할 수 있는 지표

```
producer offset 5에 데이터 저장 (log-end-offset=5)
consumer offset 2 데이터의 데이터 처리 (current-offset=2)

consumer lag = 3 (5-2)  →  record 3개 지연 발생
```

**모니터링 활용 예시**

- 평상시에는 consumer lag 이 1~10 수준으로 적은 지연
- 트래픽이 몰리는 시점에 consumer lag 이 늘어나는 것이 관측되는 경우 partition 을 늘리고, consumer 를 늘려서 producer 의 처리량을 따라갈 수 있도록 조치

---

## Consumer Options

| 옵션 | 설명 | 기본값 |
| --- | --- | --- |
| `max.poll.records` | consumer polling 시 최대로 가져가는 메세지 수 | 500 |
| `max.poll.interval.ms` | consumer 가 polling 후 commit 까지의 최대 대기시간 | 300,000ms (5분) |
| `session.timeout.ms` | consumer, broker 간 session timeout | 10,000ms |
| `heartbeat.interval.ms` | consumer → broker heartbeat 주기 | 3,000ms |

**max.poll.records / max.poll.interval.ms 주의**

```
1. max.poll.records 설정에 의해 500개 메세지 가져옴
2. 한 개 메세지 처리당 1초 소요
3. max.poll.interval.ms default 5분 초과
4. 5분 내 poll() 이 재호출되지 않았으므로 예외 발생
```

**session.timeout.ms / heartbeat.interval.ms**

- consumer, broker 간 heartbeat 를 주고받지 않고 연결을 유지하는 최대 시간
- `heartbeat.interval.ms` 는 `session.timeout.ms` 보다 작아야 함
