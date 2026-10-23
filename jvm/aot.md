# AOT

## AOT (Ahead-of-Time)

- `java -jar` 실행 시점 아래 작업 lazy 하게 수행

```
1. 클래스 로딩/링킹/초기화 (.class 파일 읽고 파싱, 메모리에 로드, 다른 클래스 연결등)
2. 인터프리터 통해 바이트코드 해석/실행
3. 프로파일링 (해당 메서드가 얼마나 자주 호출되는지)
4. JIT 컴파일 (자주 실행되는 메서드는 기계어로 컴파일하여 이후 빠르게 실행)
```

- 1~4 과정을 모두 실행 시점에 lazy 하게 수행
- AOT 는 결과가 같은 작업을 실행 전에 미리 수행하고 결과를 저장한 뒤 필요할 때 저장된 결과를 사용하도록 하는 것.
- 어떤 작업을 미리하는지에 따라 AOT 기술이 구분됨
    - 클래스 로딩/링킹/프로파일 수집 -> JVM AOT Cache
    - spring 초기 컨텍스트 초기화 작업 -> Spring AOT
    - 바이트 코드 전체를 기계어로 컴파일 -> GraalVM Native Image

## JVM AOT Cache

- `java -XX:AOTCacheOutput=app.aot -jar app.jar`
    - 실행 전(배포 전) 미리 한 번 애플리케이션을 실행해서 클래스 로딩/링킹 및 프로파일을 `.aot` 파일에 저장
    - 실제 jar 실행시 해당 `.aot` 캐시 파일을 사용
    - `java -XX:AOTCache=app.aot -jar app.jar`
- springboot 는 java25 부터 AOT Cache 지원 (그 이전 버전에서는 CDS 사용)
    - java24부터 AOT Cache 지원하지만 메서드 프로파일 저장 및 springboot 지원은 java25 부터임
- docker image 빌드 시점에 실제 서버를 한 번 구동해서 `.aot`를 구성하는 트레이닝 런 수행
    - docker image 빌드 시간 늘어날 수 있음
    - 캐시 파일이 이미지에 포함되므로 이미지 크기 커짐
- 트레이닝 런 시 클래스 로딩/링킹 정보들 뿐만 아니라 메서드 프로파일 정보도 `.aot` 에 캐시됨
    - 해당 메서드 호출시 바로 c2 컴파일 대상이라는 것을 .aot 로부터 알 수 있다면 바로 c2 컴파일 진행 가능 (트레이닝 런에서 c1 or c2 까지 갔었다 라는 기록과 프로파일 저장)
    - 즉, 트레이닝 런에서 호출 경로까지 웜업하는 것도 효과있음.
    - 트레이닝 런에서는 `onRefresh` 방식 통해 클래스 로딩/링킹 및 spring 내부 코드 프로파일 캐싱정도만 하고 실제 요청경로는 jvm 구동 이후 별도 applicationRunner 에서 하는게 좋을 듯.
- 효과
    - 시작 시간 단축
    - `Dspring.context.exit=onRefresh` 설정시 트레이닝 런은 서버 시작 전에 종료되므로 실제 요청경로 웜업에는 큰 효과 없음
    - 서버 시작 전?: 빈 생성과 초기화까지 끝낸 뒤, 라이프사이클 시작 전 (lazy가 아닌 싱글톤 빈은 모두 생성 이후 종료)

```
FROM bellsoft/liberica-openjre-debian:25-cds AS builder
WORKDIR /builder
COPY build/libs/*.jar application.jar
RUN java -Djarmode=tools -jar application.jar extract --layers --destination extracted

FROM bellsoft/liberica-openjre-debian:25-cds
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
RUN java -XX:AOTCacheOutput=app.aot -Dspring.context.exit=onRefresh -jar application.jar
ENTRYPOINT ["java", "-XX:AOTCache=app.aot", "-jar", "application.jar"]
```

    
```
# 트레이닝 런 db 엑세스 제거
FROM bellsoft/liberica-openjre-debian:25 AS base

FROM base AS extract
WORKDIR /build
COPY build/libs/app.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --destination extracted

FROM base AS app
WORKDIR /app
COPY --from=extract /build/extracted/dependencies/ ./
COPY --from=extract /build/extracted/spring-boot-loader/ ./
COPY --from=extract /build/extracted/snapshot-dependencies/ ./
COPY --from=extract /build/extracted/application/ ./

FROM app AS aot-train
ARG AOT_TRAINING_OPTS="-Dspring.context.exit=onRefresh -Dspring.profiles.active=aot"
RUN java -XX:AOTCacheOutput=app.aot -Xlog:aot=error $AOT_TRAINING_OPTS -jar app.jar
RUN java -XX:AOTCache=app.aot -XX:AOTMode=on -Xlog:aot=error $AOT_TRAINING_OPTS -jar app.jar

FROM app
COPY --from=aot-train /app/app.aot app.aot
USER 1000:1000
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:AOTCache=app.aot", "-jar", "app.jar"]

# application-aot.yml
spring:
  jpa:
    database-platform: org.hibernate.dialect.MySQLDialect
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        boot:
          allow_jdbc_metadata_access: false

```