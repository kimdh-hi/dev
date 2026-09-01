# cache stampede

## cache stampede

- 캐싱된 항목이 사용 불가능해진 순간, 그 값을 원하는 요청들이 모두 캐시 미스를 겪고 전부 DB로 몰려가는 현상
    - stampede: 동물 떼나 군중이 한 쪽 방향으로 일제히 우르르 몰리는 상황 의미
- hotkey 가 만료된 경우 해당 캐시키 데이터에 의존하는 수많은 요청으로 인해 DB 과부하 발생
- 캐시를 사용하는 전형적인 패턴인 Cache-Aside(Look-Aside) 패턴에서 명확
    - 캐시 조회
    - miss? => db 조회
    - 캐시 저장

## stampede 해결 방안

### TTL 랜덤화 (TTL jitter)

- redis 는 ttl 만료시 key 를 바로 삭제하지 않는다.
- redis key 는 두 가지 방식으로 만료된다.
    - passive: 클라리언트가 접근했을 때 시간이 지났으면 그 때 만료처리
    - active: 주기적으로 만료 ttl 설정이 있는 키 중 몇 개를 무작위 검사해 삭제
- 두 방식이 같이 동작
- 트래픽이 몰리면서 여러 redis key에 대한 요청이 집중되는 경우 캐시 미스가 몰릴 수 있음
- key 그룹에 stampede 방지를 위해 대해 TTL 랜덤화 검토 (TTL jitter)
    - key 그룹에 대해 stampede 이슈 발생시 캐시 미스를 조금이라도 방지하는 효과 위함
- 단, hotkey 에 대한 stampede 이슈는 방지 불가

```kotlin
class JitteredTtlFunction(
    private val baseTtl: Duration,
    private val jitterRatio: Double = 0.1,     // ±10%
) : TtlFunction {

    override fun getTimeToLive(key: Any, value: Any?): Duration {
        val baseSeconds = baseTtl.seconds
        val jitter = (baseSeconds * jitterRatio).toLong().coerceAtLeast(1)
        val offset = ThreadLocalRandom.current().nextLong(-jitter, jitter + 1)
        return Duration.ofSeconds(baseSeconds + offset)
    }
}

@Configuration
@EnableCaching
class CacheConfig {

    @Bean
    fun cacheManager(
        connectionFactory: RedisConnectionFactory,
        objectMapper: ObjectMapper,
    ): RedisCacheManager {

        fun config(ttl: Duration, jitter: Double = 0.1) =
            RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(JitteredTtlFunction(ttl, jitter)) // 지터
                .disableCachingNullValues()
                .serializeKeysWith(
                    SerializationPair.fromSerializer(StringRedisSerializer())
                )
                .serializeValuesWith(
                    SerializationPair.fromSerializer(
                        GenericJackson2JsonRedisSerializer(objectMapper)
                    )
                )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config(Duration.ofMinutes(10)))
            .withInitialCacheConfigurations(
                mapOf(
                    "products"   to config(Duration.ofMinutes(30)),
                    "categories" to config(Duration.ofHours(6), jitter = 0.2),
                    "rankings"   to config(Duration.ofMinutes(5), jitter = 0.3),
                )
            )
            .build()
    }
}
```

### PER (Probabilistic Early Recomputation, 확률적 조기 만료)

- 캐시 히트시마다 남은 TTL 을 보고 확률적으로 TTL을 재갱신
- TTL이 적게 남을수록 높은 확률로 캐시 갱신 (XFetch)
- 확률적으로 key 만료 자체를 안되게 하여 stampede 이슈 회피
- spring 진영에서 직접적으로 지원하지는 않고 직접 구현 필요
- 로컬 캐시의 경우 Caffeine 에서 refreshAfterWrite 가 유사한 목적으로 기능 제공
    - https://github.com/ben-manes/caffeine/wiki/Refresh

### 분산락

- 어떤 기법을 사용해도 이미 없는 키에 대해 요청이 몰리는 경우 DB 로 부하가 그대로 전달될 수 있음 (Cache-Aside 인 경우)
- redis key 조회시 분산락 적용하여 redis key 조회 -> (miss 시) db 조회 -> redis key put 을 임계영역으로 설정한다.
- 다음번 요청은 전부 캐시 hit 되어 db 로 부하가 몰리지 않는다.
- 요청이 몰리는 경우 stampede 방지 목적이므로 spin lock 보다는 pub/sub 기반 락 적용 검토할 것. (redisson RLock)
- 단, 1000개 요청중 999개 요청은 최소 첫번째 요청이 임계영역에서 db I/O를 하는 시간만큼은 대기가 걸림

## reference

- https://en.wikipedia.org/wiki/Cache_stampede
- https://toss.tech/article/cache-traffic-tip
- claude