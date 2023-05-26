## Redis 키 스페이스 이벤트

수신 가능 이벤트
- key 에 영향을 끼치는 모든 명령시
- key에 대해 LPUSH 작업시
- key 가 만료되즌 경우 (ttl)

기본적으로 키 스페이스 이벤트는 cpu 를 사용하기 때문에 성능상 이유로 비활성화 되어있음. <br/>
`redis.conf` 에서 활성화 및 설정 가능 <br/>

```
K     Keyspace events, published with __keyspace@<db>__ prefix.
E     Keyevent events, published with __keyevent@<db>__ prefix.
g     Generic commands (non-type specific) like DEL, EXPIRE, RENAME, ...
$     String commands
l     List commands
s     Set commands
h     Hash commands
z     Sorted set commands
t     Stream commands
d     Module key type events
x     Expired events (events generated every time a key expires)
e     Evicted events (events generated when a key is evicted for maxmemory)
m     Key miss events (events generated when a key that doesn't exist is accessed)
n     New key events (Note: not included in the 'A' class)
A     Alias for "g$lshztxed", so that the "AKE" string means all the events except "m" and "n".

//K 또는 E 는 반드시 포함되어야 함 
```

### 만료 이벤트 타이밍

ttl 이 설정된 Key 는 두 가지 방식으로 만료됨
- key 에 대해 특정 명령에 의해 접근되었을 때 만료된 것을 확인한 경우
- 백그라운드에서 만료된 키를 찾음

즉, ttl 이 0이 된 바로 그 시점의 이벤트를 보장할 수 없다. <br/> 
위 두 방식에 의해 만료가 확인된 경우에만 만료 이벤트를 발생시킨다. <br/>


---

### 참고
https://redis.io/docs/manual/keyspace-notifications/