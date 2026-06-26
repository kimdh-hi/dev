# Kafka

# Kafka Broker

- kafka 서버 인스턴스를 의미
- 여러 broker 가 cluster 를 구성
- 여러 broker 는 로드밸런싱, HA 제공
- 각 broker 는 고유 id 를 가지며, 클라이언트는 어떤 broker 에 연결하더라도 전체 클러스터의 메타데이터 통해 전체 broker 에 접근(식별) 가능
- replication factor (하나의 파티션을 몇 개 브로커에 복제할지) 의 권장값이 3이므로 broker 의 수도 초기 3개를 권장

## Broker 기능

### partition replica

- partition 마다 하나의 Leader broker 가 읽기 쓰기를 담당
- HA 위해 Follower Broker가 Leader Broker의 메시지를 복제(Fetch)하여 저장

### 메세지 저장

- 메세지 저장단위: segment (.log)
- 하나의 segment 에는 연속된 offset 의 메세지들이 저장된다.
    - segment 의 파일명은 시작 offset 값으로 지정됨
- 각 segment 마다 .index 파일 생성
    - [offset, segment 내 시작 bytes] 구조
    - offset 에 따라 해당 segment 내 n bytes 부터 읽도록 하여 조회 성능 향상
- segment 별 .timeindex 통해 시간 기반 검색도 지원

## Cluter 운영 모드

- ZooKeeper
- KRaft

### ZooKeeper

- ~ 3.x (lagacy)
- Controller Broker 가 클러스터 당 한 개 씩 존재하여 ZooKeeper (ZooKeeper 앙상블) 과 통신
    - 선출된 controller broker 가 kafka cluster 전체 상태를 관리
- controllerbroker 장애시 zookeeper 가 감지하여 후처리
    - controller broker 장애시: 새 controller broker 선출
    - 일반 broker 장애는 controller broker 가 감지하여 rebalancing 수행
- ZooKeeper 도 리더/팔로워로 구성
    - HA 제공
    - 메타데이터 쓰기, controller broker 선출 등에 과반수 동의 필요(Quorum) 하므로 홀수 권장 (짝수여도 quorum 동작은 함. 단, 짝수-1 개와 동일함)
- 메타데이터가 Zookeeper, broker 에 분산되어 있어서 동기화 이슈 발생 가능

### KRaft (Kafka Raft Metadata)

- kafka 2.8 preview 도입, 3.3 부터 권장, 4.0부터 유일 모드
- Raft 합의 알고리즘 내장하여 ZooKeeper 와 같은 별도 시스템 없이 동작
- `__cluster_metadata` 내부 토픽에 모든 메타데이터를 로그 형태로 저장
- controller 의 개념을 broker 로부터 분리
    - combined 모드 사용하면 broker 가 controller 겸용 가능
    - 단, 운영 환경에서는 combined 모드가 아닌 별도 controller (isolated) 권장
- controller 의 역할은 zookeeper 앙상블의 각 역할과 유사
    - 메타데이터 저장
    - Raft 프로토콜 통한 active/standby 선출
- 각 Broker는 쿼럼에 참여하지 않고 메타데이터를 fetch해서 캐싱만 함

---

# Bootstrap

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

# Consumer Group

- 각 consumer group 은 다른 consumer group 으로부터 격리된 환경에서 운영됨
- consumer group 내 consumer 는 한 topic 의 여러 partition 이 할당될 수 있음
  - 단, consumer 수는 partition 수와 1:1 매핑되도록 하는 것이 가장 좋음
    - consumer 수가 초과되는 경우 idle consumer 발행으로 낭비
    - partition 수가 초과되는 경우 특정 consumer 에 부하 발생 가능
- consumer group 은 같은 목적/기능의 consumer 집합
- 동일 consumer group 내에서는 1개 메세지는 1번만 처리됨 (메세지 중복처리 방지)
  - 브로커에는 partition 별 offset 관리 데이터 외에 consumer group 별 offset 데이터도 관리되어 consumer group 별 메세지 중복처리 방지 가능 (__consumer_offsets)

**why consumer group**

- a, b, c 각 다른 서버가 운영되는 경우, a 에서 b, c 로 데이터 동기화를 위해 특정 topic 에 메세지 발행
- b, c 에 각각 다른 consumer group 을 할당하는 경우 topic 의 메세지를 격리되어 처리 가능
- 예: b 서버 장애로 처리 못한 경우 c 는 독립적으로 처리 가능, 이후 b 서버가 복구되면 마지막 소비한 메세지로부터 이후 메세지들을 처리 가능 (결과적으로 데이터 동기화 가능)

## MSA 관점에서

- 각 마이크로서비스는 한 개 consumer group
- k8s 환경에서 각 마이크로서비스의 pod(=consumer)는 늘어났다가 줄었다가를 반복할 수 있음
- partition 수는 모든 consumer group 중 가장 많은 consumer 가 필요한 시점을 기준으로 함
  - k8s 기준 HPA maxReplicas 값을 기준으로 partition 수 산정

---

# Partitioner

- producer 가 topic 내 여러 파티션 중 어떤 파티션으로 메세지를 보내야 할지를 결정
- key 가 있는 경우 기본적으로는 메세지 key 를 해시한 결과로 파티션을 결정한다. 
  - 따라서 동일한 키값을 가진 메세지는 모두 동일한 파티션으로 보내진다.
  -  murmur2 해싱 알고리즘 사용
  - hash(key) % partitions
  - key="aaa-1" --> murmur2 -> 해시값 % 3(파티션 수) --> 파티션 number 결정
- 운영중 파티션 증가시 고려사항
  - 파티션 수 기반으로 파티션이 결정되므로 동일 key 값이어도 파티션 수가 달리지면 결정되는 파티션 number 가 달라질 수 있음
  - 병렬처리가 아닌 특정 파티션의로만 보내서 순서를 보장하고자 한다면 새로운 토픽으로 마이그레이션을 검토
- key 가 없는 경우
  - kafka 2.3 이하: RoundRobbin
  - kafka 2.4 이상: StickyPartitioner
- RoundRobbin 단점
  - 메세지 발송시마다 파티션을 순서대로 바꿔가며 분배
  - kafka 는 기본적으로 프로듀서 측에서 메세지 버퍼를 두고 메세지를 배치 처리
    - 네트워크 비용 절감 위함
  - 버퍼는 파티션별로 나뉘어져 `linger.ms`, `batch.size` 옵션에 의해 트리거 되어 배치 발송처리됨
    - `linger.ms`: 최대 대기시간
      - kafka 4.0 이전 default: 0
      - kafka 4.0 이후 default: 5ms
    - `batch.size`: 최대 버퍼 크기 (bytes)
  - RoundRobbin 은 순서대로 로 파티션이 바뀌므로 배치가 작게 쪼개진다.
- StickyPartitioner 
  - key 가 없는 메세지에 대해 RoundRobbin 전략에서 배치 성능 저하 이슈 해결 위함
  - kafka 2.4 (KIP-480) 도입
  - key 가 없는 메세지는 하나의 파티션에 `달라붙어 (sticky)` 배치가 가득 찰 때까지 보낸다.
  - 배치가 가득찼을 때 (batch.size) 전송되므로 `liner.ms` 대기로 인한 지연 감소, 배치 단위가 거치므로 요청 수 감소

```
UniformStickyPartitioner
- https://cwiki.apache.org/confluence/display/KAFKA/KIP-794:+Strictly+Uniform+Sticky+Partitioner

기존 StickyPartitioner (UniformStickyPartitioner) 이슈
- 기존 StickyPartitioner 는 다음 sticky 파티션으로 넘어가는 기준이 브로커의 성능과 연관됨
  - 파티션 버퍼의 레코드를 브로커에 보내고 ack 를 받은 후 다음 파티션으로 넘어감
  - 느린 브로커에 의해 ack 가 늦어지면 해당 파티션 버퍼에는 계속해서 레코드가 쌓이고 매 번 큰 사이즈의 배치가 느린 브로커에 send, ack 
  - 느린 브로커에 더 많은 레코드가 몰리고 한 번 시작된 불균형을 회복하지 못 함
- Strictly Uniform Sticky Partitioner (KIP-794, Kafka 3.3)
  - sticky 파티션 전환 기준을 batch.size 바이트 만큼 생산만으로 변경, 더이상 브로커의 성능(=ack) 에 의존하지 않음
  - 위 개선만으론 계속해서 느린 브로커는 언젠가 stick 파티션이 될 것이고 batch.size 만큼 레코드가 쌓이면 어쩔수없이 지연이 발생됨.
- adaptive 적용
  - partitioner.adaptive.partitioning.enable (default: true)
  - 의도적으로 불균형
  - 느린 브로커의 파티션은 sticky 파티션이 될 확률을 낮추고, 빠른 브로커가 가져가도록 유도
```

---

# Partition 수

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

## Rebalancing

- topic 내 partition 와 consumer group 내 consumer 간 할당 상태가 변경되는 것
- 리밸런싱이 발생하는 케이스 (consumer 추가/제거)
  - 새로운 consumer 가 추가/제거되는 경우, 최대한 1:1 매칭이 되는 최대 성능을 위해 할당 관계를 변경
  - 특정 consumer 장애 시 해당 consumer 에 매칭된 partition 을 해당 consumer group 내 다른 consumer 로 할당 (consumer 에 대한 failover)
- partition 수가 적은 경우 (< 10?) 에는 리밸런싱 자체가 큰 문제가 되지 않을 수 있지만, partition 수가 많아지는 경우 (< 100?) 리밸런싱 과정 자체를 장애로 볼 수 있을 정도의 문제가 될 수 있음

---

# Consumer Lag

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

# Consumer Options

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
