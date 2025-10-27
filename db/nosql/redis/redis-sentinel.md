## redis sentinel
- redis 고가용성 전략 중 하나 (sentinel, cluster)
- sentinel 노도의 모니터링을 통해 master 노드 장애시 slave 노드를 master 로 승격하여 고가용성 확보

### 구성요소
- sentinel: master/replica node 모니터링 및 자동 장애 복구
- master(primary)
- slave(replica)

### 장애판별

#### SDOWN (Subjectively Down, 주관적 장애)
- sentinel -> master 로 ping/info 응답이 3초(down-after-millisecondes) 간 오지 않는 경우 SDOWN 으로 판단.
- SDOWN 만으로 장애조치는 진행되지 않는다.

#### ODOWN (Objectively Down, 객관적 장애)
  - quorum 이상 수의 sentinel 이 해당 마스터가 장애라고 판단하는 경우 ODOWN으로 판단되어 장애조치 진행

```
sentinel 수와 quorum
- quorum 이 2인 경우 최소 2대 이상의 sentinel 이 master 장애로 인지해야 장애복구 진행

최소 권장사항
- sentinel 3대
- quorum 2
```

### master 선출
- ODOWN 시 새로운 master 선출
- SDOWN/ODOWN/DISCONECT 상태 slave 는 선출 대상에서 제외
- 남은 slave 중 `slave_prority` 가 높은 순으로 선택 (default: 100)
  - `slave_prority` 를 0을 설정한 node 는 master 승격 대상에서 제외

### spring data redis sentinel connection

#### code base

```
@Bean
public RedisConnectionFactory lettuceConnectionFactory() {
  RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration()
  .master("mymaster")
  .sentinel("127.0.0.1", 26379)
  .sentinel("127.0.0.1", 26380);
  return new LettuceConnectionFactory(sentinelConfig);
}

```

#### property base

```
RedisSentinelConfiguration can also be defined through RedisSentinelConfiguration.of(PropertySource), which lets you pick up the following properties:

Configuration Properties
- spring.redis.sentinel.master: name of the master node.
- spring.redis.sentinel.nodes: Comma delimited list of host:port pairs.
- spring.redis.sentinel.username: The username to apply when authenticating with Redis Sentinel (requires Redis 6)
- spring.redis.sentinel.password: The password to apply when authenticating with Redis Sentinel
- spring.redis.sentinel.dataNode.username: The username to apply when authenticating with Redis Data Node
- spring.redis.sentinel.dataNode.password: The password to apply when authenticating with Redis Data Node
- spring.redis.sentinel.dataNode.database: The database index to apply when authenticating with Redis Data Node
```

----

issue
- sentinel monitoring > redis-master not found
  - https://stackoverflow.com/questions/57464443/redis-sentinel-throws-error-cant-resolve-master-instance-hostname
  - sentinel resolve-hostnames yes

---

reference
- https://redisgate.kr/redis/clients/spring_session_redis_sentinel.php