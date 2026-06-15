## observability?

- metric, log, trace 세 가지 신호를 관찰
- metric
    - 시계열 데이터
    - 초당 요청수, 에러률, cpu 사용률, 응답시간…
    - prometheus, mimir, thanos…
- log
    - 로그
    - loki, elasticsearch
- trace
    - 하나의 요청이 여러 서비스를 거칠 때의 경로를 기록
    - 요청의 end-to-end 경로, 특정 경로에서 지연 등 확인 가능
    - tempo, jaeger

## OpenTelemetry (OTel)

- https://opentelemetry.io/docs/
- metric, log, trace 데이터를 어떻게 만들고 어떤 형태로 내보낼지에 대한 벤더 중립 표준
- OTel 이전
    - metric 은 prometheus, trace 는 Jaeger, 로그는 … 제가각이었음.
    - OTel 은 하나의 표준 프로토콜로 통일
        - SDK + Protocol(OTLP) + Semantic Conventions
- OTel API
    - 계측 데이터를 생성 할 수 있는 표준 인터페이스
- OTel SDK
    - API 를 구현한 도구 모음
    - 코드 레벨에 sdk 도입시 metric, log, trace 를 OTel 표준 형식으로 생성
    - OTel API 의 실제 다양한 구현체들
- OTel Collector
    - https://opentelemetry.io/docs/collector/architecture/
    - application ↔ 저장소 사이 파이프라인
    - receive → process(가공/필터링) → export
        - receive: OTLP, prometheus scrape 등 통해 데이터 수신
        - processor: 필터링, 속성 추가/삭제, 샘플링 비율 결정 등 수행
        - exporter: 백엔드로 전송 (Tempo, Prometheus, Loki..)
    - OTel collector or Grafana Alloy
    - OTLP 형식이라면 OTel collector, Grafana Alloy 모두 호환
- Protocol - OTLP (OpenTelemetry Protocol)
    - https://opentelemetry.io/docs/specs/otlp/
    - application(SDK) → collector(OTel Collector/Grafana Alloy) → backend(Tempo, Prometheus, Loki..)
    - 대부분의 구간에서 OTLP 전송 규약 사용
        - app SDK → collector: 대부분 OTLP 사용
        - collector → backend: OTLP 사용 or backend native protocol 사용
            - Loki exporter, prometheus remote write
            - Tempo, Prometheus, Loki 모두 OTLP 수신 지원함
                - 단, trace 외 log, metric 의 경우 상대적으로 OTLP 지원이 늦었으므로 collector → backend 구간을 native protocol 를 사용하는 경우가 많음
    - 중간에 provider 가 바뀌어도 (OTel Collector → Grafana Alloy or Prometheus → mimir…) 애플리케이션은 OTLP 로만 내보내니 애플리케이션 코드에 영향 없음
- Semantic Conventions
    - 다양한 operation과 데이터에 대한 공통 이름을 명시
        - 이런 종류의 데이터는 다음과 같은 이름, 형식으로 기록한다.
    
    ```yaml
    http.request.method = "GET"
    http.response.status_code = 200
    db.system.name, service.name, service.version
    ```
    

## LGTM (Loki / Grafana / Tempo / Mimir)

- https://github.com/grafana/intro-to-mltp
- OTel 은 application 이 observability 지표들을 어떻게 생성하고 가공해서 LGTM 스택의 backend 영역으로 넘기는 것까지를 담당
- LGTM 은 OTel 지표들을 넘겨받아 저장, 조회, 인덱싱 등을 담당
- L: Loki
    - log 저장소
- G: Grafana
    - 시각화, 대시보드
- T: Tempo
    - trace 저장소
- M: Mimir
    - metric 저장소
- LGTM 으로 묶었을 때 grafana labs 의 설계에 의해 trace↔log↔metic 간 점프 등을 기본적으로 제공
    - 물론 Grafana 위에서 Jaeger + Prometheus + ES 를 택해도 점프는 만들수 있지만 수동설정이 많이 들어감 (trace_id, service.name 기반으로 찾는 것이기 때문에 불가능한 것은 아님)
    - LTM 스택이 바뀌어도 점프 등이 설정 통해 가능한 것은 Semantic Conventions 로 통일된 키 이름 덕분

### LGTMP (LGTM + Profile)

- Pyroscope
- Profile?
    - code 의 어느 부분에서 CPU, memory, 응답시간 등이 소요되는지를 함수 단위로 보여주는 신호
        - `sendMessage()` 함수가 CPU의 70%를 쓰고 있었음

## spring OpenTelemetry 적용 변천사

#### Springboot 2.x - micrometer, prometheus pull

- springboot 2.0 micrometer 도입
- metric 수집 목적
- slf4j 퍼사드 구조와 동일한 구조로 여러 구현체 코드 수정없이 사요 위함

```java
meterRegistry.counter("orders.created").increment();

//micrometer-registry-prometheus
//micrometer-registry-datadog
//...
```

### Micrometer OTLP registry

- springboot 3.x
- 코드레벨에서는 이전 micrometer 방식과 동일
- 실제 전송 포맷만 OTLP 포맷으로 개선
- 데이터 흐름 방향(Pull → Push) 변경
- 이전에는 `/actuator/prometheus` 를 노출하면 prometheus 서버가 주기적으로 가져가는 구조
- Micrometer 의 OtlpMeterRegistry 가 수집한 지표를 OTLP 프로토콜로 OTel 호환 backend 로 push
    - 우리 서버는 지표를 어디로 보낼지 능동적으로 설정 가능
    - OtlpMeterRegistry 는 Micrometer api 통해 수집된 meter 데이터를 OTLP 데이터로 변환

```java
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    runtimeOnly   'io.micrometer:micrometer-registry-otlp'   // prometheus registry 대체
}

management:
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics // target backend
        step: 30s // 주기
        resource-attributes:
          service.name: payment-service
```

### Otel Java Agent / Otel starter

- 이전까지 계측을 하는 주체는 Micrometer 였음. OLTP 는 단순히 backend 로 내보내는 포맷으로써만 사용
- 계측 자체를 Micrometer 를 거치지 않고 OpenTelemetry SDK 를 사용하도록 개선
- 애플리케이션은 OTel api 로 관측 데이터를 내보내고, 해당 OTel api 를 구현한 SDK 가 수집, 필터링, 샘플링 등을 거쳐 내보내기까지 담당

option 1: OpenTelemetry Java Agent

```java
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=service-name \
     -Dotel.exporter.otlp.endpoint=http://localhost:4318 \
     -jar myapp.jar
```

- code, 의존성 등 조작 x

option2: OpenTelemetry Spring Boot Starter

```java
implementation 'io.opentelemetry.instrumentation:opentelemetry-spring-boot-starter'

otel:
  service:
    name: payment-service
  exporter:
    otlp:
      endpoint: http://localhost:4318
```

### Springboot 4 OTel starter

- https://spring.io/blog/2025/11/18/opentelemetry-with-spring-boot
- `spring-boot-starter-opentelemetry` 제공
    - 다시 micrometer 통해 trace, metric 을 OTLP 로 내보냄

```java
management:
  **opentelemetry:**
    resource-attributes:
      service.name: spring-app
  otlp:
    metrics:
      export:
        url: http://localhost:4318/v1/metrics
        enabled: true
  tracing:
    sampling:
      probability: 0.1
```

- springboot 4 는 위 방식을 권장
    - java agent 간 충돌없음, native image 친화적, spring 의 micrometer 그대로 사용