# redis memory 정책 (maxmemory, maxmemory-policy)

## redis memory 정책

- redis 메모리 정책은 두개 설정이 짝을 이룬다.
- maxmemory: 데이터 저장에 쓸 메모리 상한 지정
- maxmemory-policy: 상한에 도달시 동작

## maxmemory

- default: 0
    - 메모리 제한x (32bit 시스템의 경우 3gb으로 설정 덮어씀)
    - 기본값 사용시 사용 가능한 모든 메모리가 소진될 수 있음
    - 상한을 설정하는 것을 권장
- 엄격한 상한이 아님
    - 일시적으로 상한을 큰 폭으로 초과되는 경우 별도 예외처리 없이 허용될 수 있음
    - https://redis.io/faq/doc/1jbxid5qq7/is-maxmemory-the-maximum-value-of-used-memory

## maxmemory-policy

- maxmemory 상한 도달시 evict 정책에 대한 설정
- default: `noeviction`
- 정책은 `대상범위-알고리즘` 으로 네이밍
    - `allkeys-`: 모든 키 대상
    - `volatile-`: TTL 설정된 키만 대상
- `volatile-` 정책들은 ttl 설정된 키가 없는 경우 `noeviction` 처럼 동작

| 정책 | 동작 |
| --- | --- |
| `noeviction` | 키를 축출하지 않고, 새 데이터를 캐싱하는 명령을 실행시 서버가 오류를 반환. 복제를 사용하는 경우 이 조건은 프라이머리에만 적용. 기존 데이터를 읽기만 하는 명령은 정상 동작 |
| `allkeys-lru` | 가장 오래 사용되지 않은(LRU) 키를 축출 |
| `allkeys-lrm` | 가장 오래 수정되지 않은(LRM) 키를 축출 (Redis 8.6+) |
| `allkeys-lfu` | 가장 적게 사용된(LFU) 키를 축출 |
| `allkeys-random` | 무작위로 키를 축출 |
| `volatile-lru` | 만료 시간(TTL)이 설정된 키 중 가장 오래 사용되지 않은 것을 축출 |
| `volatile-lrm` | TTL이 설정된 키 중 가장 오래 수정되지 않은 것을 축출 (Redis 8.6+) |
| `volatile-lfu` | TTL이 설정된 키 중 가장 적게 사용된 것을 축출 |
| `volatile-random` | TTL이 설정된 키 중 무작위로 축출 |
| `volatile-ttl` | TTL이 설정된 키 중 남은 TTL이 가장 짧은 것을 축출 |

## maxmemory, maxmemory-policy 설정 및 설정 확인

### 설정 확인

```
# maxmemory, maxmemory-policy 설정 확인
127.0.0.1:6379> CONFIG GET maxmemory maxmemory-policy
1) "maxmemory"
2) "0"
3) "maxmemory-policy"
4) "noeviction"

# 현재 사용중 memory 확인 (<https://redis.io/docs/latest/commands/info/>)
127.0.0.1:6379> INFO memory
# Memory
used_memory:4911880
used_memory_human:4.68M
used_memory_rss:19759104
used_memory_rss_human:18.84M
used_memory_peak:6883312
used_memory_peak_human:6.56M
used_memory_peak_perc:71.36%
used_memory_overhead:1354328
used_memory_startup:948584
used_memory_dataset:3557552
used_memory_dataset_perc:89.76%
allocator_allocated:5938328
allocator_active:6410240
allocator_resident:10391552
allocator_muzzy:0
total_system_memory:33166307328
```

```
# /etc/redis/redis.conf

################################## MEMORY MANAGEMENT ###################

maxmemory 4gb
maxmemory-policy allkeys-lru

# 런타임 변경 (redis-cli)
> CONFIG SET maxmemory 4gb
> CONFIG SET maxmemory-policy allkeys-lru
```

## reference

- https://redis.io/docs/latest/develop/reference/eviction/
- https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/memory-optimization/
- claude