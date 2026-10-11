# Pinpoint

## Pinpoint?

- https://github.com/pinpoint-apm/pinpoint
- naver 에서 개발한 오픈소스 APM (Application Performance Management)
- google 의 dapper에서 영감을 받았으며, 분산 애플리케이션 간 트랜잭션을 추적해 시스템 전체 구조 분석

## 계측 방식

- Pinpoint 는 기존 코드 수정없이 동작 가능
- Pinpoint Agent 는 클래스 로딩 시점에 대상 라이브러리 (JDBC, Tomcat, http client 등)의 바이트코드를 변경해 대상 메서드의 바이트코드에 실행 전후로 Pinpoint의 인터셉터 객체 삽입
- 실행 전후로 Pinpoint 인터셉터의 before(), after() 호출
- 샘플링 비율을 낮춰도 before, after 는 호출되며 각 메서드 내에서 샘플링 여부에 따라 early return

```
// 개념적으로

//원본
public String hello(String name) {
    return "hello " + name;
}

//인터셉터 삽입
public String hello(String name) {
    Object[] args = new Object[]{name};
    interceptor.before(this, args);
    try {
        String result = "hello " + name;
        interceptor.after(this, args, result, null);
        return result;
    } catch (Throwable t) {
        interceptor.after(this, args, null, t);
        throw t;
    }
}
```

## Pinpoint Agent

- 모니터링 대상 애플리케이션(was..)의 JVM 에 `javaagent` 옵션으로 붙어서 실행
- 클래스 로딩 시점에 대상 라이브러리의 바이트코드 변경해 Pinpoint 인터셉터 객체 삽입하여 해당 메서드 호출시 계측 가능하도록 하는 역할 수행
- 트랜잭션 추적 데이터(span)과 JVM 통계(cpu, memory, GC, thread..) 를 수집해 Collector 로 전송하는 역할 수행

## Pinpoint Collector

- pinpoint agent 가 gRPC 통해 보내는 데이터를 가공하여 외부 저장소(HBase ..)에 쓰는 역할

## 샘플링 설정

```
//pinpoint.config
profiler.sampling.type=PERCENT # COUNTING | PERCENT
profiler.sampling.percent.sampling-rate=100

// jar -javaagent
java -javaagent:/pinpoint-agent/pinpoint-bootstrap.jar \
     -Dpinpoint.agentId=test-api-01 \
     -Dpinpoint.applicationName=test-api \
     -Dprofiler.sampling.type=PERCENT \
     -Dprofiler.sampling.percent.sampling-rate=100 \
     -jar app.jar
```