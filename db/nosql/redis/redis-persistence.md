# redis persistence

## redis persistence

- redis persistence 는 redis 재시작, 크래시 이후에도 메모리 상태를 복구할 수 있는 기능을 제공
    - redis persistence 는 데이터를 어디에 저장하느냐 가 아닌 프로세스가 죽은 뒤 메모리 상태를 어떻게 복원할 것인가에 대한 답
    - 상태 자체(데이터 자체)를 디스크에 저장해서 메모리에 복원할 것인지, 상태에 도달한 연산을 기록해서 재생을 통해 복원할 것인지에 따른 방식 결정 필요
- redis 는 영속성 관련 옵션 4가지 제공
    - RDB (Redis Database): 지정한 간격으로 데이터셋의 특정 시점의 스냅샷을 생성
    - AOF (Append Only File): 서버가 받은 모든 쓰기 연산을 기록, 서버 시작시 이를 재생해 데이터셋 복원
    - 영속성 없음
    - RDB + AOF

## RDB (Redis Database)

- redis는 `dump.rdb` 라는 바이너리 파일에 스냅샷을 저장
    - N초 동안 M건 이상의 변경이 있을 때 저장 or save/bgsave 통해 직접 저장
- 스냅샷은 자식 프로세스가 fork 되어 자식이 임시 RDB 파일에 데이터셋을 기록하고 완료시 기존 파일을 교체하는 순서
    - `dump.rdb` 는 인스턴스 당 한 개만 존재
    - 스냅샷 저장시 자식 프로세스에서 `temp-<자식PID>.rdb` 에 임시파일 작성 완료 이후 rename 통해 기존 `dump.rdb` 대체
- 수동 실행시 이미 백그라운드에서 스냅샷 저장이 실행중인 경우 에러를 반환
- 기본적으로 RDB 는 활성화 상태
- 기본값: `save 3600 1 300 100 60 10000`
    - 3600초(1시간) 동안 1건 이상 변경시 스냅샷 저장
    - 300초(5분) 동안 100건 이상 변경시 스냅샷 저장
    - 60초 동안 10000건 이상 변경시 스냅샷 저장
- 변경 건수의 기준은 명령어 수가 아닌 변경된 키, 요수 수
    - SET k v => 1회
    - SADD key a b c => 3회
- N초 경과시 M건 미달시 카운터는 리셋되지 않음
- 스냅샷 저장 완료 이후 저장된 건수만큼 건수 차감

```
127.0.0.1:6379> config get save
1) "save"
2) "3600 1 300 100 60 10000"

# dump.rdb 위치
# dir + dbfilename = /data/dump.rdb
127.0.0.1:6379> config get dir dbfilename
1) "dbfilename"
2) "dump.rdb"
3) "dir"
4) "/data"
```

```
RDB 주의사항
- 비정상종료 시 데이터 손실 가능 (dump.rdb 에 쓰이지 못한 데이터셋은 손실 가능)
  - 정상종료 시에는 메인 프로세스가 직접 스냅샷 저장 이후 종료
- fork 기반이므로 부모 redis 프로세스의 데이터셋이 큰 경우 지연 발생 가능
```

## AOF (Append Only File)

- 서버는 모든 쓰기 연산을 로그에 기록하고, 서버 시작시 이를 재생해서 원래 데이터셋을 복원하는 방식

```
# 활성화 방법 (default: 비활성화)
# redis.conf
appendonly yes
```

```
# set user:1 alice 시 AOF 파일에 추가되는 내용
# *3 : 인자 3개짜리 배열
# $3: 뒤에 3바이트 문자열

*3\r\n
$3\r\nSET\r\n
$6\r\nuser:1\r\n
$5\r\nalice\r\n
```

- 실수로 `flushall` 했을 때 마지막 명령만 aof 파일에서 지우고 재시작하는 등의 수동 복구 가능
    - 사람이 읽을 수 있는 포맷이기 때문.
- AOF에는 클라이언트가 보낸 명령을 그대로 받아 적지 않는다.
    - 재생시 반드시 같은 결과가 나오도록 확정된 명령만 적재된다.
- Write-After log
    - 관계형 DB의 WAL (Write-Ahead log) 와 다른 것에 주의
        - 관계형 DB WAL 은 디스크에 쓰기 전 메모리(버퍼 풀)에 먼저 쓰는 것을 의미
    - Redis AOF의 WAL 은 명령을 먼저 실행하고 기록하는 것을 의미 (Ahead X, After O)

### AOF 파일 구조

- redis 7.0.0 부터 멀티파트 AOF 방식 사용
- 기존 단일 AOF 파일이 base 파일 한 개과 incremental 파일 여러 개로 분리
    - base: .rdb 포맷으로 스냅샷 저장
    - 여러개 .aof 파일들은 .rdb 스냅샷 이후부터의 확정 명령어
- RDB 의 dump.rdb 와는 별개 파일이며 관리주기도 다르다.
- aof 의 .rdb 는 별개 주기로 rewrite 된다.
    - 수동 rewrite: `redis-cli bgrewriteaof`

## reference

- https://redis.io/docs/latest/operate/oss_and_stack/management/persistence/
- claude