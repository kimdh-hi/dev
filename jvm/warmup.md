# warmup

## warmup 필요한 이유

```
두 가지를 목적으로 한다
- 클래스 로딩
- JIT 컴파일러

클래스 로딩
- 해당 클래스를 필요로 하는 경우 .class 파일을 JVM 내에서 사용할 수 있도록 메서드 영역에 로딩하고 초기화하는 등의 작업 필요
- 위 작업은 lazy 하게 수행된다.
- 필요로 하는 클래스에 미리 접근하여 로딩 작업을 미리 해놓음으로 구동 초기 안정적인 성능을 갖도록 한다.

JIT 컴파일러
- JVM 구동 초기 C1/C2 컴파일러에 의해 컴파일된 최적화된 기계어 코드가 코드캐시 영역에 없기 때문에 인터프리터가 메서드 바이트 코드를 처리
- 이러한 이유로 JVM은 구동 초기 비교적 나쁜 성능을 가지고, 실행 횟수가 늘어날수록 JIT 컴파일러(c1/c2) 통해 최적화된 코드가 코드 캐시영역에 위치하면서 안정적인 성능을 갖게 된다.
```

## JVM warmup 관련 참고사항

### 빌드

- .java -> .class 컴파일
- .class 패키징 (to jar/war)

### 런타임

- java -jar
- JVM 시작
- 클래스 로딩/링킹/초기화
    - https://docs.oracle.com/javase/specs/jvms/se21/html/jvms-5.html
- 클래스를 JVM 이 사용할 수 있는 상태로 준비하는 작업까지 수행
    - .class 파일 내용을 JVM 이 다룰수 있는 형태로 메모리(메서드 영역)에 올리는 것
- 필요한 클래스를 그때그때 로딩/링킹/초기화

### 실행

- 메서드 호출시 인터프리터가 메서드 영역에서 바이트 코드를 읽어서 실행
    - 인터프리터는 호출 횟수를 카운팅
- 메서드 호출 횟수 및 메서드 내 루프 횟수을 기준으로 기준 횟수를 넘는 경우 메서드 컴파일
    - 호출횟수, 루프 반복 횟수 등을 기준으로 c1 컴파일 큐에 컴파일 요청
    - 메서드 호출 횟수 200회 초과 또는 호출 100회+(호출+루프횟수) 2000회 초과시 인터프리터 -> c1 전환 대상
- c1 컴파일러에 의해 컴파일 된 후 호출 5000회 또는 호출 600회+(호출+루프횟수) 15000 초과시 c1 -> c2 전환 대상
    - https://github.com/openjdk/jdk/blob/master/src/hotspot/share/compiler/compiler_globals.hpp
    - Tier4CompileThreshold 참조

### C1/C2

- c1 (Client Compiler), c2 (Server Compiler)
- JIT 컴파일러의 두 종류
    - c1, c2 모두 기본적으로 컴파일된 결과는 코드 캐시라는 메모리 영역에 저장
- C1 은 인터프리터에 의해 실행중인 메서드 바이드코드를 컴파일
    - 컴파일 시 프로파일링을 위한 코드가 추가됨
    - 인터프리터에 의해 메서드 바이트 코드 실행시마다 호출횟수 등 카운팅 되어 기준 넘어서는 경우 C1 컴파일러 통해 기계어로 번역
- C2 도 메서드 바이트코드를 컴파일
    - 컴파일 시 바이트코드와 프로파일 된 데이터 기반으로 강하게 최적화된 기계어 생성
- 모든 메서드가 호출횟수가 늘어났을 때 c2 컴파일 대상이 되는 것은 아님
    - 코드가 매우 단순하여 최적화 여지 없는 경우 level1 수준에서 머무를 수 있음

### level

- level0: 인터프리터(바이트코드), 호출/반복 횟수, 상세 프로파일 기록
- level1: c1 컴파일, 기록할 거 없음 (c2 컴파일이 될 필요없는 코드)
- level2: c1 컴파일, 호출/반복 횟수 기록
- level3: c1 컴파일, 호출/반복 횟수, 상세 프로파일 기록 (c2 컴파일러에 넘길 프로파일 기록)
- level4: c2 컴파일

## warmup 적용시 주의

- spring에서 warmup 적용시 일반적으로 `ApplicationRunner` 에서 warmup 코드를 실행한다.
    - 모든 `ApplicationRunner` 작업이 완료된 이후 `readinessState` 를 `ACCEPTING_TRAFFIC` 로 갱신
    - 앞 단의 리버스 프록시 또는 LB 등은 readiness 상태를 참조하여 트래픽을 보냄
- `ApplicationRunner` 의 웜업 작업이 종료된 이후 트래픽을 받게 됨.
- 특정 매트릭이 임계값에 도달해 오토스케일링 된 경우 새로운 서버가 신속하게 투입되어야 하지만 warmup 작업에 소요되는 시간만큼 투입이 늦어짐. 단, 투입 직후 안정적인 성능을 가짐 (trade-off)

## Reference

- https://docs.oracle.com/en/java/javase/25/vm/java-hotspot-virtual-machine-performance-enhancements.html
- https://docs.spring.io/spring-boot/reference/features/spring-application.html#features.spring-application.application-availability
- https://speakerdeck.com/kakao/jvm-warm-up
- https://oliveyoung.tech/2024-10-30/application_warmup_algorithm/