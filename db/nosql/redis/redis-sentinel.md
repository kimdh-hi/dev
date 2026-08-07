# Redis Sentinel

## Redis Sentinel

- Redis 는 자체적으로 replication 기능을 제공하여 master + replica N대 구성 가능
- 복제만으로는 master 가 죽었을 때 replica 가 자동으로 master 로 승격되지 않음
    - 수동으로 replica -> master 승격 필요
- sentinel 은 redis cluster 를 사용하지 않는 환경에서 replica -> master 승격을 자동으로 수행하여 HA 를 제공
- 별도 redis 서버와 별개로 26379 포트로 sentinel 프로세스를 띄워 관리
    - sentinel 프로세스는 redis 서버의 특별한 실행 모드
    - key-value 저장소가 아니며 get/set 과 같은 redis 명령어 처리x
    - sentinel 명령과 감시로직을 담당
- cluster 와 비교하여 수평 확장, 샤딩 등은 불가하지만 고가용성 확보 가능

## Sentinel 주요 기능

- Monitoring
    - master, replica 정상동작 여부 확인
- Notification
    - 모니터링 대상중 이슈 발생시 API 통해 관리자 또는 타 앱으로 알림
- Automatic failover
    - master 장애시 replica 를 master 로 승격하고 나머지 replica 가 새로운 master 를 바라보도록 설정 후 애플리케이션에게 새로운 master 정보를 알림
- Configuration provider
    - 클라이언트에서 현재 마스터 정보 등을 질의시 답변
    - failover 시 새로운 마스터 정보를 알림

## 주의사항

- 최소 3개 sentinel 인스턴스 필요
    - failover 시 quorum 정족수 관련 이슈
    - 1대 장애시 남은 쪽이 과반수를 만들 수 있는 최소구성이 3개
    - failover 시 sentinel 중 하나가 리더로 선출 필요
    - 이는 과반수 투표로만 가능. 여기서 과반수는 설정값이 아니고 sentinel 전체수로부터 고정적으로 파생
        - sentinel 2 => 과반2
        - sentinel 3 => 과반2
        - sentinel 4 => 과반3
        - sentinel 5 => 과반3
    - 2개 인 경우 과반이 2 이므로 한 대 장애시 남은 쪽에서 과반을 만들 수 없음.
    - 4개는 3개와 동알하게 1대 장애에만 대응 가능하므로 홀수 구성이 효율적
- quorum 과 과반수(majority)의 차이
    - quorum: 마스터를 ODOWN 으로 판정하기 위해 동의 필요한 sentinel 수 (monitor 생성시 지정)
    - 과반: failover 실제 수행 여부 결정 동의 필요한 sentinel 수
    - 과반수는 sentinel 수에 의해 결정되므로 커스텀 불가
    - 일반적으로 quorum을 majority와 동일하게 설정
- 각 sentinel 은 서로 다른 vm, 가용영역에 배치
    - 장애가 독립적으로 발생되어 failover 가능하도록
- 쓰기 손실 가능
    - redis replication 자체의 특성
    - 단, sentinel 은 자동으로 replica 를 master 로 승격시키므로 쓰기 손실된 replica 의 손실이 master 로 승격되는 순간 확정
- 클라이언트 라이브러리 Sentinel 지원여부 확인

## 장애판정 (SDOWN, ODOWN)

### SDOWN (Subjectively Down, 주관적 다운)

- 특정 sentinel 인스턴스 하나에 국한된 다운 상태
- 설정된 시간동안 ping 요청에 유효한 응답하지 못한 경우 SDOWN 상태로 판단
- 각 sentinel 은 다른 sentinel 로 ping 을 보내고 `down-after-milliseconds` 동안 유효 응답이 없는 경우 해당 sentinel을 SDOWN 으로 표시
- SDOWN 만으로는 failover 시작되지 않음

### ODOWN (Objectively Down, 객관적 다운)

- quorum: master 를 ODOWN 으로 만들기 위해 오류 상태를 감지해야 하는 sentinel 수
- 각 sentinel은 다른 sentinel 로 ping 보내면서 SDOWN 체크
- 본인이 마스터가 SDOWN 이라고 판단된 경우 다른 sentinel 로 마스터가 죽었는지 주기적으로 확인
    - `SENTINEL is-master-down-by-addr <A-ip> 6379 <epoch> *`
- 3개 sentinel 인 상태 (a-마스터, b, c)
    - b, c 가 a SDOWN 으로 체크
    - b와 c는 서로 주기적으로 마스터 죽었는지 체크
    - b 가 먼저 is-master-down-by-addr 보낸 경우 c 가 a(마스터)가 죽었다라고 응답
    - b 본인은 a를 SDOWN으로 체크했으므로 1표, c의 1표로 quorum 2 만족되어 마스터 ODOWN 판정
    - c 본인도 위 플로우로 마스터 ODOWN 판정
    - 리더 선출 및 마스터 승격작업 시작

## sentinel 설정

- 기본적으로 master-replica 구조 설정 필요
    - `redis.conf`
    - `replicaof <master-ip> <master-port>`
- sentinel 실행

```
redis-sentinel /path/to/sentinel.conf
# 또는
redis-server /path/to/sentinel.conf --sentinel
```

- sentinel.conf 설정

```
port 26379

sentinel monitor mymaster 192.168.1.10 6379 2 # sentinel monitor <master-name> <ip> <port> <quorum>
sentinel down-after-milliseconds mymaster 5000 # SDOWN 판단을 위한 ping의 유효 응답시간
sentinel failover-timeout mymaster 60000
sentinel parallel-syncs mymaster 1
```

- sentinel 설정시에는 마스터 정보만을 포함
    - redis replication 시 각 replica 에 마스터 정보가 설정되므로  master-replica 모두 서로 정보를 알 수 있음
    - 각 sentinel 프로세스는 마스터 정보를 알고 있으므로 마스터를 통해 replica 와 각 sentinel 정보를 획득
- sentinel 구성시 최소 3개 sentinel 프로세스를 필요로 함
    - redis 서버가 3대일 경우 → 각 서버마다 redis, sentinel 프로세스 배치
    - redis 서버가 2대일 경우 → 각 서버마다 redis, sentinel 프로세스 배치 후 다른 독립 서버에 sentinel 추가 배치
        - 별도 독립 서버일 필요는 없고 was, db, 모니터링서버 등에 배치해도 됨
        - 단, 해당 서버 오토스케일링 여부 확인 필요
        - sentinel 은 한 번 인식된 sentinel 프로세스를 잊지 않음
        - sentinel 설치된 서버가 늘어났다가 줄어드는 경우 과반수 등에서 처리 등에서 이슈될 수 있음
        - 오토스케일시 sentinel 설치가 되지 않도록 조치를 하든, 오토스케일 대상 서버가 아닌 곳에 배치

## springboot 기준 client 설정

- sentinel 정보를 입력하고, 부트스트랩 과정을 통해 sentinel 로 부터 master 정보(6379) 얻어와서 연결
- 순서는 크게 중요하지 않음 (모든 sentinel 프로세스는 마스터 정보를 알고 있으므로)

```yaml
spring:
  data:
    redis:
      sentinel:
        master: mymaster # sentinel.conf 의 monitor 이름
        nodes:
          - 192.168.1.10:26379
          - 192.168.1.11:26379
          - 192.168.1.12:26379

# 읽기 분산 (boot4)
spring:
  data:
    redis:
      lettuce:
        read-from: replica-preferred
```

```kotlin
// 읽기 분산 springboot3.x 설정
@Bean
fun lettuceReadFromCustomizer() = LettuceClientConfigurationBuilderCustomizer { builder ->
    builder.readFrom(ReadFrom.REPLICA_PREFERRED)
}
```

## Reference

- https://redis.io/docs/latest/operate/oss_and_stack/management/sentinel/
- https://redis.io/tutorials/operate/redis-at-scale/high-availability/