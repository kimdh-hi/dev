# virtual thread

## thread-per-request

- 내장 tomcat 의 경우 요청 하나를 스레드 하나가 처음부터 끝까지 처리하는 방식으로 동작
- springboot 기본 설정 기준
    - 스레드풀 상한(워커 스레드 수 상한): 200개 (`server.tomcat.threads.max`)
    - 최소 유지 유휴 스레드 + 활성스레드 하한: 10개 (`server.tomcat.threads.min-spare`)
    - 동시 요청수가 스레드풀 개수를 초과하는 경우 작업큐에 대기
- 여기서 사용되는 스레드는 platform thread 로 OS thread 와 1:1로 매핑
    - 사용 가능 개수 또한 OS thread 수로 제한
- 유휴(idle) 스레드
    - 요청이 작업큐에 들어가면, 스레드 풀 안의 스레드들이 큐에서 작업을 꺼내서 실행(poll)
    - 실행이 끝나면 다시 작업큐에 다음 작업이 들어오길 대기
    - 여기서 대기하는 스레드를 유휴 스레드라 한다.
- 유휴 스레드 수를 초과하는 요청 100개가 한 번에 몰린 경우
    - 유휴 스레드 10개가 바로 10개 요청 poll 해서 처리
    - 이후 요청부터는 새로 스레드 생성하여 작업 할당
    - 작업이 완료된 스레드는 60초 간 유휴 스레드로 써 작업 대기하다가 60초 간 작업 없는 경우 스레드 제거 (minSpareThreads 보다 적은 경우 제거되지 않음)
    - 다시 최소 유지 유휴 스레드 수 10개만 유지 (처음 유휴 스레드가 다시 유휴 스레드로 되는 것은 보장하지 않음)
- 스레드 풀
    - 이미 생성된 스레드를 가져다 쓰고 다 쓰면 반납하는 구조가 아님. (db 커넥션 풀 같은 개념이 아님)
    - 유휴 스레드를 초과하는 요청이 동시에 오는 경우 스레드 풀로부터 스레드를 꺼내서 작업을 할당하는 방식이 아니고 새롭게 스레드를 생성해서 작업을 할당
    - 스레드 풀 개수 제한은 워커 스레드 개수 제한과 동일

## virtual thread

- jdk 21 정식 도입
- 기존 java thread(platform thread)는 생성시 OS 스레드가 대응되어 생성
- 가상 스레드는 OS 스레드를 만들지 않고, JVM 내에 객체로써만 존재하면서 실행될 때 잠깐 플래폼 스레드(캐리어 스레드) 위에 올라간다.
- 가상 스레드는 왜 가볍나?
    - 생성 비용
        - 플랫폼 스레드는 JVM이 OS 시스템 콜을 통해 스레드를 생성
        - 가상 스레드는 객체이므로 생성 비용 자체가 거의 없음
    - 전환 비용
        - 플랫폼 스레드는 컨텍스트 스위칭 비용 발생
        - tomcat 워커 스레드 200개가 DB 응답을 대기하는 경우 컨텍스트 스위칭 계속 발생 가능
        - 가상 스레드의 경우 OS가 바라보는 것은 캐리어 스레드 몇 개(CPU 코어수) 뿐
        - 가상 스레드가 DB 응답 등의 이유로 블로킹 되어 마운트/언마운트를 반복해도 이 교체는 JVM 유저모드 수준에서 수행되고 OS는 관여하지 않음.
        - OS 입장에서는 스레드가 바뀌지 않았으므로 스위칭 비용이 OS 수준에서 발생되지 않음
        - 캐리어 스레드 수는 코어수만큼 존재하므로 OS 수준 컨텍스트 스위칭 최소화
- 캐리어 스레드
    - 가상 스레드를 실행하고 있는 플랫폼 스레드
    - 새로운 종류의 스레드가 아닌 플랫폼 스레드의 역할
- 용어
    - mount: 가상 스레드가 플랫폼 스레드에 올라가는 것. 마운트가 되면 해당 플랫폼 스레드는 캐리어 스레드가 된다.
    - unmount: park 또는 가상 스레드 작업 완료시 캐리어 스레드로부터 내려오는 것.
    - park: 캐리어 스레드의 가상 스레드 작업이 블로킹 연산을 만나면 대기. (해당 가상 스레드 작업은 더이상 진행X)
    - unpark: 가상 스레드 블로킹 연산 종료되어 다시 작업큐로 들어가 플랫폼 스레드에 마운트되기를 대기
- park + unmount
    - 일반적인 경우 가상 스레드가 블로킹 작업을 만나면 JDK 수준에서 park 이후 unmount
    - 캐리어 스레드에서 작업중인 가상 스레드가 블로킹 연산을 만나 park 후 unmount 되어 unpark 이후 mount 대기
    - 블로킹 코드를 호출한 가상 스레드가 직접 호출
- park + mount (`pinning`)
    - park 되어 가상 스레드 작업은 진행 불가하지만 캐리어 스레드를 붙잡고 있는 상황
    - unmount 는 가상 스레드의 스택을 heap 에 저장하고 캐리어에서 내려오는 것
    - 가상 스레드의 작업이 캐리어 스레드(플랫폼 스레드)에 종속되어 있다면 가상 스레드만 떼어내서 다른 캐리어 스레드로 옮겨지면 문제가 될 수 있음
    - 이런 상황일 때 jdk 수준에서 unmount 를 하지 않고 캐리어 스레드를 붙잡는 것을 pinning 이라 한다.
    - jdk 21~23 에서 synchronized 가 대표적. JDK 24부터 개선됨 (JEP491, https://openjdk.org/jeps/491)

```
1. client 요청 도착 -> virtual thread 생성 -> 작업큐(ForkJoinPool 큐)에 들어감
2. 캐리어 스레드에 mount (controller -> service -> repositry...) 실행
3. DB 요청/응답 대기 (park -> unmount) 가상 스레드 스택은 heap 에 저장되고, 캐리어 스레드는 다른 가상 스레드 작업 mount해서 진행
4. DB 응답 도착 (unpark) 작업큐에 다시 들어감
5. 아무 캐리어 스레드에 mount 되어 실행 이어감
6. 응답 반환, 작업 종료 이후 unmount
```

## reference

- https://docs.spring.io/spring-boot/appendix/application-properties/index.html
- https://openjdk.org/jeps/444
- https://docs.oracle.com/en/java/javase/21/core/virtual-threads.html
- https://openjdk.org/jeps/491
- https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.virtual-threads