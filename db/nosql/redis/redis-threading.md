# Redis threading

## redis threading

- redis 는 이벤트 루프 기반 싱글 스레드 아키텍처를 사용
- 위 특성으로 별도 락 처리 없이 효율적으로 원자성 보장 가능
- 단, `명령 실행` 만 싱글 스레드에서 실행되고 디스크 I/O, 요청, 응답 읽기(파싱) 쓰기 등은 redis 버전에 따라 별도 스레드에서 처리
- redis 의 싱글 스레드는 명령을 실행하는 스레드가 하나인 것으로 이해하는 것이 정확함

## Background I/O 스레드

- redis 2.x 도입
- 디스크 write, 메모리 해제 등 무거운 디스크, 메모리 작업을 Background I/O (bio) 에 위임하여 처리
    - 메모리 작업 등은 후에 redis 4.x 에 도입됨
- https://redis.io/docs/latest/operate/oss_and_stack/management/optimization/latency/

## Threaded I/O (fork-join)

- redis 6.0 도입
- `io-threads N`, `io-threads-do-reads yes` 설정 제공
    - `io-threads`: I/O 스레드 수 (default: 1) - 기존 메인 스레드 수를 포함하므로 default 값은 사실상 비활성
    - `io-threads-do-reads`: 읽기/파싱 시 I/O 스레드 사용 여부 (default: no)
    - https://github.com/redis/redis/blob/7.2/redis.conf
- 동작 방식
1. 메인 스레드가 읽기 대기 목록 (clients-pending-read) 의 모든 항목을 꺼냄
2. I/O 스레드들에 라운드로빈으로 분산(fan-out).
- 메인 스레드도 I/O 스레드들과 동일하게 읽기/파싱 처리
- 단, 대기 클라이언트 수가 io_threads * 2 보다 적으면 I/O 스레드 쓰지않고 메인 스레드가 처리
    - 별도 커스텀한 설정값은 없는듯

> 관련 코드 (networking.c)
https://github.com/redis/redis/blob/7.2/src/networking.c
> 

```
int stopThreadedIOIfNeeded(void) {
    int pending = listLength(server.clients_pending_write);

    /* Return ASAP if IO threads are disabled (single threaded mode). */
    if (server.io_threads_num == 1) return 1;

    if (pending < (server.io_threads_num*2)) {
        if (server.io_threads_active) stopThreadedIO();
        return 1;
    } else {
        return 0;
    }
```

1. 각 I/O 스레드가 자기 담당 소켓에서 읽고 파싱.
- 이때 메인 스레드는 모든 읽기/파싱 작업이 끝날 때까지 스핀-웨이트(바쁜 대기)로 대기 (=> 배리어).
1. 배리어가 풀리면 메인 스레드가 파싱된 명령 전부를 순차 실행.
- I/O 스레드들은 명령 순차 실행 종료될 때까지 대기
1. 응답 쓰기도 같은 방식으로 fan-out → 배리어 → 완료.

> 읽기(파싱)/쓰기에 대해 별도 I/O 스레드가 작업을 분담하지만 가장 느린 스레드 기준으로 메인 스레드가 대기하고, 메인스레드 명령 실행시 I/O 스레드도 대기하는 비효율이 있음
> 

## Async I/O 스레드

- redis 8.0 도입
- 메인 스레드와, I/O 스레드 간 베리어(대기) 없이 동시실행

```
읽기 + 파싱 → I/O 스레드 (네트워크 I/O + 바이트→명령 구조 변환)
명령 실행 + 응답 생성 → 메인 스레드 (여전히 단일 스레드, 원자성 유지)
응답 쓰기 → I/O 스레드
```

- 기존 Threaded I/O 와 거의 동일해보이지만 메인 스레드드는 더이상 I/O 에 투입되어 대기하는 구조가 아니다. (베리어 X)
- 메인 스레드는 비동기 이벤트 방식으로 읽기 완료된 요청에 대한 명령을 실행하고 쓰기를 I/O 스레드에 할당하기만 한다.

```
┌────────────────────────────────────────────────────────────┐
   │                       MAIN THREAD                           │
   │   • 클라이언트를 I/O 스레드에 할당 (assign)                    │
   │   • 완료 통지 받은 쿼리 실행(execute) + 응답 생성(reply)        │
   │   • 배리어 없음 → 기다리며 놀지 않음                            │
   └───▲───────────────┬────────────────────────────▲───────────┘
       │ notify(완료)   │ assign                      │ notify
       │               ▼                              │
   ┌───┴──────┐  ┌──────────┐  ┌──────────┐  ┌───────┴──┐
   │IO Thread1│  │IO Thread2│  │IO Thread3│  │IO Thread4│
   │ 자체 루프 │  │ 자체 루프 │  │ 자체 루프 │  │ 자체 루프 │
   │read+parse│  │read+parse│  │read+parse│  │read+parse│
   │  write   │  │  write   │  │  write   │  │  write   │
   └──────────┘  └──────────┘  └──────────┘  └──────────┘
        (각자 비동기로 소켓 read/write, 서로/메인과 lockstep 아님)
```

- `io-threads N` 통해 활성화 (default: 1 사실상 비활성화) - redis 6.x 과 동일
- `io-threads-do-reads` 옵션도 동일 (default: no)
- TLS 지원
    - redis 8.x 이전 TLS 켜면 io-threads 동작X
    - https://oneuptime.com/blog/post/2026-03-31-redis-io-threads-multi-threading/view
- reference
    - release note: https://github.com/redis/redis/blob/8.0/00-RELEASENOTES
    - https://redis.io/blog/redis-8-0-m03-is-out-even-more-performance-new-features/