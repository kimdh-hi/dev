# Redis Cluster

- Redis Cluster 는 데이터를 여러 redis 노드에 자동으로 샤딩하여 수평 확장 기능을 제공
- 각 노드(master node) 는 16384개 hash slot 의 일부분(부분집합)을 담당
    - 이론적으로는 최대 16383개 노드까지 수평확장 가능
    - 단, 최대 1000개 상한을 권장
- 각 master node 는 이하로 n개 replica node 를 가질 수 있고, 각 replica node 는 master node 의 데이터를 복제하며 고가용상 위한 failover 가능

## HashSlot

- Redis Cluster 의 데이터 샤딩 구조
- 16384개 hash slot 존재
- 특정 키는 `CRC16(key) mod 16384` 연산에 의해 슬롯이 결정
- 각 Redis  Cluster 노드는 슬롯의 부분집합을 담당
- 3개 노드로 구성된 경우
    - node1: 0~5500 slot
    - node2: 5501~11000 slot
    - node3: 11001~16383 slot
- 위 구조로인 해 노드 추가/제거에 용이
    - 4번 노드 추가시 기존 1,2,3 노드의 slot 일부를 D에 할당하도록 조정
    - 제거도 유사한 구조이므로 슬롯 이동은 운영 중단 없이 가능
- 단, 최대 1000개 노드 제한을 권장
    - `High performance and linear scalability up to 1000 nodes.`
    - https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/
- Hash Tag
    - https://redis.io/docs/latest/operate/oss_and_stack/reference/cluster-spec/#hash-tags
    - 여러 키를 같은 슬롯에 강제로 배치하고자 하는 경우 사용
    - `{..}` 패턴이 있는 경우 첫번쩨 `{` 와 오른쪽 첫번쩨 `}` 사이 문자열만 해싱하여 슬롯을 계산
        - 단, 중괄호 사이에는 하나 이상의 문자가 있어야 함.
    - ex) `{user100}.followings` , `{user100}.followers`

## master-replica

- 각 master node 는 16384 개 hash slot 중 일부를 담당
- 각 master node 는 이하로 n개 replica node 를 가질 수 있음
- replica node 도 master node 가 담당하는 동일한 범위의 hash slot 의 데이터를 복제
    - master → replica 데이터 복제틑 비동기이므로 성공을 보장하지 않는다.
- 각 master node 에는 n개 replica 를 설정할 수 있지만 각 master node 마다 replica 설정 수는 다를 수 있다.
- `replica read-only`  (replica 읽기 전략 지정)
    - 특정 hash slot 영역의 key 가 요청되는 경우 기본적으로 해당 요청은 해당 영역을 담당하는 master 로 가고 master 가 읽기 연산에 대한 처리를 수행
    - 읽기 연산자체의 부하를 replica 로 분산하고 싶은 경우 replica 에서 읽기 설정 가능 (아래 설정 참조)
    - 단, redis cluster 특성상 replica 로 복제는 stale 값이 조회될 수 있음
    - 부하 분산 목적이라면 node 추가 투입도 검토해볼 것.

```kotlin
@Configuration
class RedisConfig {

  @Bean
  fun readFromCustomizer(): LettuceClientConfigurationBuilderCustomizer =
      LettuceClientConfigurationBuilderCustomizer { builder ->
          builder.readFrom(ReadFrom.REPLICA_PREFERRED)
      }
}
```

## replica 승격

- master node 가 죽은 경우 즉시 replica 승격이 진행되지 않고, 장애 감지 → replica 선거 → 마스터 투표 순의 절차 이후 replica 가 master 로 승격됨.
- master node 장애 이후 최소 `cluster-node-timeout` 이후 장애를 확정하고 이후 마스터 선출 전 작업 등으로 수 초까지 시간 소요 가능 (`cluster-node-timeout` + 수 초)
- https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/
1. 장애 감지
    1. 각 마스터 노드는 서로 ping/pong 중 특정 노드로부터 `cluster-node-timeout` 동안 pong 받지 못한 경우 장애를 의심 (PFAIL)
    2. 마스터 노드 과반이 동일 노드를 장애로 판정하는 경우 장애로 판정 (PFAIL → FAIL)
2. replica 에 의한 선거
    1. master FAIL 확정시 replica 들 내부에서 master 후보 선출
    2. 마스터로부터 얼마나 많은 복제 데이터를 처리했는지 순위에 따라 선출
3. master 에 의한 선거 및 당선/승격
    1. 다른 마스터 노드의 과반 투표를 받은 replica node 가 master 노드로 당선 및 승격

## 특징

- 강한 일관성을 보장하지 않는다.
    - 마스터A 에 write 시 replicaA 로 복제/전파가 완료되기 전에 OK 응답을 보낸다.
    - 즉, replicaA 로의 복제를 보장하지 않는다.
    - 해당 write 를 받지 못한 replicaA 가 master 로 승격되면 해당 write 는 영구히 손실
    - 성능과 일관성 간의 트레이드 오프 중 성능을 택한 케이스
- 최소 노드 수
    - 동작 가능한 클러스터는 최소 3개 마스터 노드를 포함해야 됨.
    - 마스터 3개 + 레플리카 3개 총 6개 노드 구성을 권장
        - 마스터 1대 당 1개 레플리카
    - https://redis.io/docs/latest/operate/oss_and_stack/management/scaling/#requirements-to-create-a-redis-cluster

## redis cluster 설정

```yaml
# redis.conf
port 7000
cluster-enabled yes # redis cluster 활성화여부 (no: standalone)
cluster-config-file nodes.conf
cluster-node-timeout 5000 # timeout 초과시 replica 에 이해 failover
appendonly yes
```

```bash
redis-cli --cluster create 127.0.0.1:7000 127.0.0.1:7001 \
127.0.0.1:7002 127.0.0.1:7003 127.0.0.1:7004 127.0.0.1:7005 \
--cluster-replicas 1
```

- `--cluster-replicas 1` : master 마다 replica 가 1개 씩 설정

## Spring 기반 설정

```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - 127.0.0.1:7000
          - 127.0.0.1:7001
          - 127.0.0.1:7002
          - 127.0.0.1:7003
          - 127.0.0.1:7004
          - 127.0.0.1:7005
        max-redirects: 3
```

```kotlin
@Configuration
class RedisConfig {

    @Bean
    fun redisConnectionFactory(): LettuceConnectionFactory {
        val clusterConfig = RedisClusterConfiguration(
            listOf(
                "127.0.0.1:7000",
                "127.0.0.1:7001",
                "127.0.0.1:7002",
                "127.0.0.1:7003",
                "127.0.0.1:7004",
                "127.0.0.1:7005",
            ),
        ).apply {
            maxRedirects = 3
        }
        return LettuceConnectionFactory(clusterConfig)
    }
}
```