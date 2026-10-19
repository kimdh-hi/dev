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

### Single Flight

- `product:1` 이라는 key 로 n개 요청이 동시에 몰리는 경우 가장 먼저 들어온 요청 1개만 필요한 조회 로직을 수행
- 나머지 요청은 앞선 조회로직이 반환 될 때 캐시도, db도 조회하지 않고 해당 응답을 그대로 사용
- 캐시 적재는 조회 로직을 수행한 가장 먼저 들어온 요청이 수행
- 명시적으로 락을 잡고 조회 로직에 들어가는 경우 db조회는 최적화하지만 캐시 조회는 최소한 n-1 번 발생됨, single flight 방식의 경우 miss 시 캐시 조회 0건
- spring 진영에서는 `@Cacheable(sync = true)` 통해 single flight 유사 기능 제공
    - Synchronized Caching
    - provider 마다 다르지만 대기 요청은 캐시에 값이 채워지는 것을 기다렸다가 캐시로부터 값을 조회해서 사용 (spring data redis)
    - https://docs.spring.io/spring-framework/reference/integration/cache/annotations.html#cache-annotations-cacheable-synchronized

### SWR (stale-while-revalidate)

- 캐시 만료시 바로 버리지 않고, 특정 시간동안 기존 값을 그대로 응답(stale)하면서, 뒤에서 새 값으로 갱신(revalidate)
- `product:1` ttl 10분인 경우
    - 핫키인 경우 10분마다 db로 큰 부하, 응답지연 발생 가능
- stale 5분 설정
    - ttl 10분이 요구되는 경우 실제 ttl은 stale 시간 포함 15분으로 설정
    - 히트시 ttl 10분 경과된 경우 백그라운드에서 db 통해 캐시 갱신 (10~15분 사이 캐시 히트 요청이 있어야 함)
    - 갱신하는 동안에 요청이 들어온 경우 stale 시간동안은 기존 값 그대로 응답(hit)
    - 갱신된 경우 최신 값으로 새롭게 ttl 10분 stale 5분으로 시작
- ttl 을 15분으로 설정하는 것과 무엇이 다르고 왜 사용해야 하는가?
    - db 조회로직 자체가 매우 느린 경우 miss 이후 캐시 갱신을 위한 조회 자체가 오래 걸리므로 stampede 이슈로 인한 db, 외부 i/o 등 과도한 부하 발생 가능
    - 단순히 ttl 을 15분으로 설정하는 경우 15분 이후 요청 몰릴 때 동기적으로 부하가 걸리는 조회로직 수행 이후 캐시 갱신 및 응답이 되므로 지연 및 부하 발생
    - stale 시간 동안 백그라운드에서 캐시를 갱신하므로 부하 및 client 응답 지연 최소화
- redis 에서 swr 을 직접 제공하지 않으므로 애플리케이션 수준에서 구현 필요
    - ttl + stale 만큼 ttl 설정
    - 값에는 실제 언제부터 stale 로 판단할지에 대한 값을 같이 저장
    - 캐시 히트시마다 stale 여부 확인하여 stale 로 판단되는 겨우 백그라운드에서 캐시 갱신 진행
- 적용 검토
    - 해당 key 로 요청이 몰리는 경우
    - 캐시에 적재할 조회 로직이 무거운 경우

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