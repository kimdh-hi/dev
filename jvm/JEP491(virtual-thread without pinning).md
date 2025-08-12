## virtual thread synchronized (JEP 491)

### virtual thread
- JDK 21 release (JEP 444)
  - https://openjdk.org/jeps/444
- JVM 수준의 thread (platform thread) 가 OS thread 와 1:1로 매핑되는 되어 platform thread 생성 비용이 커질 수 있는 이슈 해결 위함
- virtual thread 는 OS thread 와는 독립적이고, virtual thread 생성시 OS thread 를 생성하지 않는다.
- virtual thread 에서 1초 소요되는 I/O 작업시 해당 virtual thread 는 1초간 blocking 되지만, platform thread 는 다른 virtual thread 에게 할당되어 자원 효율을 높인다.

### virtual thread pinning
- 대표적으로 synchronized 키워드가 붙은 곳에서 I/O 작업시 virtual thread pinning 이슈가 발생한다.
  - pinning: platform thread 에 virtual thread 가 고정되는 이슈 (다른 virtual thread 에 할당되지 못함)
- 해결방안
  - synchronized 제거
  - ReentrantLock 으로 대체
- virtual thread 가 소개되고 위 이슈로 인해 여러 라이브러리들이 수정되었다.

```
https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-0-0.html

Synchronized blocks in the Connector/J code were replaced with ReentrantLocks. This allows carrier threads to unmount virtual threads when they are waiting on IO operations, making Connector/J virtual-thread friendly.

https://github.com/spring-projects/spring-data-jpa/issues/3505
```

### JEP 491: Synchronize Virtual Threads without Pinning
- JDK 24 released
- Synchronize 내 virtual thread 가 platform thread에 고정되는 이슈 수정
- Synchronize 에 사용되는 monitor 내부에서도 mount/unmount 지원
- virtual thread 적용시 관련 라이브러리 수정없이 pinning issue 해결 위함 


---
참고
- https://openjdk.org/jeps/491
- https://openjdk.org/jeps/444
- https://dev.mysql.com/doc/relnotes/connector-j/en/news-9-0-0.html