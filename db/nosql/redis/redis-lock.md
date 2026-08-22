# Redis Lock

## spin lock

- lock 을 얻지 못했을 때 대기하지 않고 짧은 간격으로 계속 재시도 하는 방식으로 락

```kotlin
while (락 획득 실패) {
    잠깐 sleep;
    다시 시도;
}
// 임계 영역 실행
// 락 해제
```

- redis 는 lock 을 대기했다가 순서대로 lock 획득하는 네이티브 기능이 없으므로 보통 스핀락 사용

## redis lock

- redis 에서 lock 획득은 해당 key 생성 했는지 여부로 판단한다.
    - NX 옵션을 통해 해당 키가 존재하지 않는 경우에만 key 를 생성
    - key 생성 == lock 획득
    - lock 획득 이후 임계 영역 실행 이후 key 제거를 통해 lock 을 해제
- 단, 락 해제는 단순히 `DEL` 사용하면 안됨
    - 임계영역 실행이 길어져 락 유효시간을 초과하는 경우 해당 락 해제(키 삭제)되며 다른 클라이언트가 키 생성을 통해 락을 획득함
    - 단순히 `DEL` 시 다른 클라이언트의 락 획득을 위한 키를 잘못 삭제될 수 있음
    - 이를 방지하기 위해 redis 8.4 이후 `DELEX key IFEQ my_random_value` 을 통해 제거하고, 이전에는 lua 스크립트 통해 동작을 구현
- `SET key value NX PX ttl`
    - `NX`: 키가 아직 없는 경우
    - `PX ms` : lock 만료시간
- 이미 생성된 키에 대해 락 점유 위해 `SET key value NX` 를 다른 클라이언트에서 락 획득까지 반복하는 경우를 스핀락이라 한다.

## SET NX 방식 spin lock 이슈 해결

- 기존 redis 통해 락 구현시 락 획득을 위해 SET NX 명령을 주기적으로 반복하는 spin lock 이슈 있음

### pub/sub 기반 락

- 락 점유 대기자는 채널을 구독하고 락 해제시 알림을 받아 락 획득을 시도
- 락 획득을 위한 반복적 SET NX 호출이 줄어든다.
    - `RLock` 은 내부적으로 SET NX 사용하지 않음. hash + Lua 사용
- redisson client 의 `RLock` 이 이 방식을 사용

```java
RLock lock = redisson.getLock("lock:order:42");
if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {   // 최대 5초 대기, 10초 후 자동 해제
    try { doWork(); } finally { lock.unlock(); }
}
```

- Redisson lock 내부 코드

```java
Long ttl = tryAcquire(-1, leaseTime, unit, threadId);

if (ttl == null) {
    return; // 락 획득 성공. 구독 없이 즉시 반환
}
CompletableFuture<RedissonLockEntry> future = subscribe(threadId);  // 실패했을 때만 구독
```

- pub/sub 기반 락은 redisson 뿐만 아니라 Spring Integration RedisLockRegistry 통해서도 지원 가능

## Reference

- https://redis.io/docs/latest/develop/clients/patterns/distributed-locks/
- https://docs.spring.io/spring-integration/reference/redis.html
- https://redis.io/docs/latest/commands/set/
- claude