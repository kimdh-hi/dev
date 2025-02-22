kafka consumer option

max.poll.records
- consumer polling 시 최대로 가져가는 메세지의 수
- default: 500

max.poll.interval.ms
- consumer 가 polling 후 commit 까지의 최대 대기시간
- default: 30000 (5m)

```
ex)
1. max.poll.records 설정에 의해 500개 매세지 가져옴
2. 한 개 메세지 처리당 1초 소요
3. max.poll.interval.ms default 5분 초과
4. 5분 내 poll() 이 재호출되지 않았으므로 예외 발생
```

session.timeout.ms
- consumer, broker  간 session timeout
- default: 10000ms
- consumer, broker 간 heartbeat 를 주고받지 않고 연결을 유지하는 최대 시간
- heartbear.interval.ms 와 연관된 옵션

heartbeat.interval.ms
- consumer -> heartbeat 주기
- default: 3000ms
- session.timeout.ms 보다 작아야 함
