## jvm volatile

- JMM (Java Memory Model) 구조에서 스레드 간 `가시성 문제`를 해결
    - `가시성 문제`
        - JMM 구조상 각 thread 는 `메인 메모리`의 변수를 본인의 `작업 메모리에 복사`해서 사용함
        - 작업 메모리에서 변수에 대한 변경사항은 `그 즉시 메인 메모리에 반영되지 않음`
        - cpu/레지스터/캐시, ram 구조에서 변경사항을 그 즉시 ram 에 반영하고 ram 으로부터 읽기를 보장하지 않는 것과 유사
        - 즉, 다른 thread 에서 동일한 변수에 접근시 `낡은 값을 볼 가능성이 있음`
        - 이것을 가시성 문제라 함.
- `재정렬 문제 해결`
    - JIT 컴파일러는 결과가 같다면 더 효율적인 구조로 `머신 코드의 순서를 바꿔 최적화`함
    - 이 때 바뀐 순서로 인해 의도치 않은 결과가 생길 수 있음

```java
// case1
int data;                 // non-volatile
boolean ready = false;    // non-volatile

// Thread A
data = 42;        // (1)
ready = true;     // (2)

// Thread B
if (ready) {      // (3)
    use(data);    // (4) → 0을 볼 수 있다
}

// case2
while (running) {
    doWork();
}

// 최적화 전 (volatile 적용시 r1에 대해 최전화 전 상태를 강제할 수 있음)
반복:
    r1 // 메모리에서 running 가져오기(매 반복마다 가져옴)
    r1이 false면 종료
    doWork() 호출
    반복으로 점프

// 최적화 후
    r1 // 메모리에서 running 가져오기
반복:
    r1이 false면 종료
    doWork() 호출
    반복으로 점프
```

- 변수의 키워드로 `volatile` 사용시 가시성 문제와 재정렬 문제를 해결 가능
- java 는 변수 선언시 키워드로 제공하고, kotlin 은 프로퍼티 중 필드에 붙는 @Volatile 어노테이션 제공

### 대표적인 사용 예시 (Double-Checked Locking, DCL)

- 객체 생성 지연 초기화 시 DCL 적용 가능
- 객체 생성 전 해당 객체 null 여부를 먼저 체크하고 초기화되지 않은 경우 임계영역 내에서 한 번 더 객체 초기화 여부에 따라 초기화를 진행한다.
    - 객체 초기화 여부 (null 여부) 임계영역 밖, 안 두 번 체크
- DCL 통해 안전하게 객체를 지연 초기화하고 매 객체 생성 요청마다 lock 을 잡지 않아도 됨

```java
class Holder {
    private Config config = null;

    public Config get() {
        if (config == null) {              // ① 락 밖에서 검사
            synchronized (this) {
                if (config == null) {      // ② 락 안에서 다시 검사
                    config = new Config();
                }
            }
        }
        return config;
    }
}
```

- 위 DCL 패턴 객체 지연 초기화는 추가적인 명시적 동기화 없이 java 에서 사용시 깨질 수 있음
- `new Config()` 는 기계어로 컴파일 시 여러 단계로 나뉨
    1. 객체를 위한 메모리 할당
    2. 생성자 실행 (필드 초기화)
    3. config 필드에 그 주소를 대입
    - …
- 컴파일러에 의해 2, 3이 뒤바뀔 수 있음

| 순서 | 스레드 A | 스레드 B |
| --- | --- | --- |
| 1 | 락 획득 |  |
| 2 | (a) 메모리 확보 |  |
| 3 | (c) helper 에 주소 대입 |  |
| 4 |  | ① `config != null` → **통과** |
| 5 |  | 반환 → `config.timeout` 읽음 → **0** |
| 6 | (b) `timeout = 3000` 실행 |  |
- 스레드 B는 timeout이 0인 Config 객체를 받게됨 (partially constructed object)
    - 예외발생 없이 조용히 잘못된 값으로 설정됨
- 해결: config 를 volatile 로 지정

## volatile 주의

- 동시성 이슈를 완전히 해결하지 못함
    - `원자성 보장 불가`
- t1, t2 가 동시에 volatile 로 선언된 변수(a=10)을 읽음
- t1, t2 각각 a 를 5씩 증가
    - 결과 a 는 20이 아닌 15가 될 가능성이 있음
- 위와 같은 동시성, 상호배제 목적이라면 단일 인스턴스라면 synchronized, local cache lock, 다중 인스턴스라면 분산락 검토
- 이런 특성으로 volatile 변수는 boolean 이거나 값을 특정 값으로 완전히 바꾸는 연산이 사용되는 것이 일반적

## spring/java 사용시

- https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html#beans-factory-thread-safety
- spring container 는 싱글톤 인스턴스 생성시 락으로 보호하며 발행
- 컨테이너가 빈을 생성하고, 프로퍼티 주입, @PostConstruct 등을 통해 초기화하는 과정이 모두 락 임계영역 안에서 수행
- 해당 빈을 사용하는 모든 thread 에게 volatile 없이 가시성을 보장
- 싱글톤 인스턴스 내에서 `volatile`  이 필요한 경우
    - 빈 생성 및 발행 이후 바뀌는 값인 경우 `volatile`  선언
    - `volatile` 만으로 충족되지 않는 경우 AtomicXxx, synchronized, ReentrantLock, 분산락 등 검토
- 일반적인 crud api 서버의 경우 `volatile` 사용하는 경우가 흔치 않음.
    - controller, service, repository 등은 대부분 빈 초기화 시점에 모든 필드가 결정되고 후에 변하지 않으며 주로 db로부터 entity 를 조회해서 entity 의 상태를 바꾸는 작업인데, entity 는 db로 부터 조회된 결과로 thread 마다 safe 하므로 이슈 없음
- `비 싱글턴 인스턴스`인 경우 해당 인스턴스에 접근하는 스레드가 2개 이상인 경우를 관찰
    - 요청당 1스레드이므로 요청 스레드 외에 @Async, ExecutorService, CompletableFuture 등등 별도 워커 스레드 만들어 처리하는 부분중 동일 인스턴에 접근해서 값을 읽고 쓰는 경우 `volatile` 적용 검토
    - 단, `volatile` 만으로 안될 수 있으니 상황에 따라 AtomicXxx, synchronized, ReentrantLock, 분산락 등 검토

## spring volatile 사용 선택 기준

- 공통
    - 기존 값과 무관한 단일 대입일 것 (원자성을 보장하지 않으므로 `++` 과 같은 기존 값으로부터의 변경은 보장x)
    - 원자성 보장이 필요한 경우 synchronized, reetrantLock, 분산락 등 다른 솔루션 검토
- 싱글턴 인스턴스인 경우
    - 초기화 시 spring 컨테이너가 다른 스레드로부터 가시성 보장하므로 신경쓸 필요 없음.
    - 싱글턴 인스턴스는 스레드 간 공유하므로 초기화 이후 변경되는 값이 있는 경우 volatile 사용 검토
- 비싱글턴 인스턴스인 경우
    - 요청당 1스레드 모델인 경우 일반적으로 각 스레드마다 공유하게 인스턴스를 가지므로 이슈없음
    - 단, @Async, @Scheduled, ExecutorService, CompletableFuture 등 다른 스레드로 부터 동일 인스턴스의 값을 읽고 쓰는(단일 대입) 경우 volatile 사용 검토
- 가시성을 보장하는 가벼운 연산이므로 volatile 로 해결 가능하다면 volatile 사용해도 괜찮

## Reference

- https://docs.oracle.com/javase/specs/jls/se17/html/jls-17.html#jls-17.4
- https://docs.spring.io/spring-framework/reference/core/beans/factory-nature.html#beans-factory-thread-safety
- https://docs.oracle.com/javase/specs/jls/se17/html/jls-8.html#jls-8.3.1.4
- https://www.baeldung.com/java-volatile
- claude