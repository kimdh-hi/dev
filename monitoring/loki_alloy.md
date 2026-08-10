# grafana loki & alloy

## grafana loki

- grafana loki 는 수평확장, 고가용성, 멀티테넌트 등의 기능을 제공하는 로그 집계 시스템
- http 엔드포인트를 열어두고 push 방식으로 수집
- Loki 는 로그 내용 자체를 인덱싱하지 않고, 각 로그 스트림에 붙은 레이블(Label)만 인덱싱
    - 로그 본문은 압축된 chunk로 오브젝트 스토리지에저장되고, 인덱스는 해당 label set의 로그가 어떤 chunk에 있는지를 찾는 역할
    - S3/MinIO 등 object storage 에 index 와 chunk를 저장하는 구성이 일반적
    - 레이블은 Loki가 데이터 저장소에서 스트림을 찾는데 사용되므로 좋은 레이블 설계가 쿼리 성능의 핵심
- 레이블 예시: `{app="chat-server", env="dev"}`
    - 로그 검색시 레이블로 대상 스트림을 좁히는 것이 핵심
- loki 설정 (loki.yml)에 s3, minio 등 스토리지 설정 가능
    - 별도 스토리지가 아닌 파일시스템도 지정가능하지만 개발/테스트 목적으로 사용 권장
    - https://grafana.com/docs/loki/latest/operations/storage/filesystem/
- loki 설정 이후 grafana dashboard 에서 loki 서버를 데이터소스로 지정

## grafana loki 구성 스택

- 에이전트: Granafa Alloy (promtail)
    - 로그를 수집하고 레이블을 붙여 스트림으로 많든 뒤 http api 통해 loki 로 push
    - promtail 이 기본 에이전트였으나 EOL 됐고 alloy 로 마이그레이션 필요
    - https://grafana.com/docs/loki/latest/send-data/alloy/
- loki 서버
    - 로그 수집, 저장, 쿼리 처리
- Grafana
    - 로그 조회, 시각화
    - https://grafana.com/docs/grafana/latest/datasources/loki/

## Alloy

- loki 는 로그를 직접 가져가지 않음
    - prometheus 와 달리 pull 방식이 아닌 push 방식으로 로그를 수집
    - default 주기: 1분
    - https://grafana.com/docs/alloy/latest/reference/components/discovery/discovery.docker/
- loki 가 로그를 수집하는 http 엔드포인트를 열어두면 alloy와 같은 에이전트가 로그를 읽고, 레이블을 붙인 스트림을 만들어서 loki 로 push
- diecovery alloy는 도커 소켓을 감시해서 실행중인 컨테이너 목록을 주기적으로 가져온다.
    - 컨테이너가 새로 뜨거나 죽게되면 대상에서 자동으로 추가, 제거
- read 찾아낸 대상 컨테이너로부터 실제 로그를 읽는다.
    - docker engine api, 로그파일 tail, OTLP 수신 등 여러 방법 제공
- relabel 가공, 라벨링
    - 컨테이너 이름, k8s 네임스페이스 등 메타데이터를 레이블로 변환
- write 전송
    - 완성된 스트림을 loki push 엔트포인트 통해 전송
    - 배치 처리, 재시도, 버퍼링 등 기능 제공
        - 버퍼링(WAL, Write ahead log) 는 기본적 제공안됨. 필요시 WAL 활성화 필요
    - https://grafana.com/docs/alloy/latest/reference/components/loki/loki.write/?plcmt=products-nav
- alloy 는 loki 사용시 필수는 아님
    - loki push api 를 직접 호출해도 되고, loki 지원 docker 로깅 드라이버, OTel collector 를 사용하는 등 다른 방안 적용 가능
    - alloy 는 로그 전용, 즉 loki 전용이 아님
    - 로그 외에 메트릭, 트레이스 등도 각각 mimir/prometheus, tempo 로 동일한 프로세스로 다룸

## label 설계

- 낮은 카디널리티 레이블만 쓴다. (중복도가 높은)
    - 레이블 값의 조합마다 별도 스트림이 만들어지고, 스트림 수는 인덱스 크기와 ingester 메모리에 직접적인 영향을 줌
- 적합한 레이블: cluster, namespace, env, app, service_name...
- 부적합 레이블: user_id, trace_id, request_id, pod, ip, timestamp...
- Structured Metadata
    - https://grafana.com/docs/loki/latest/get-started/labels/structured-metadata/?utm_source=chatgpt.com
    - 인덱싱 되지 않고 메타데이터를 붙이는 방법
    - 카디널리티가 높아서 레이블 대상으로는 할 수 없지만 검색 조건으로 사용해야 하는 경우 Structured Metadata 지정
    - LogQL: `{service="order-api"} | trace_id="ABC123"`

## loki 설정파일 샘플 (s3 bucket 사용)

```
auth_enabled: false # 멀티테넌시 비활성화

server:
  http_listen_port: 3100 # Loki HTTP API 포트

common:
  path_prefix: /loki # 로컬 작업/캐시 디렉터리

  # 단일 Loki 인스턴스용 ring 설정
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

  replication_factor: 1

# 로그 Chunk / TSDB Index 저장소
storage_config:

  # TSDB Index의 로컬 작업 및 캐시 경로
  # 최종 Index는 S3 Object Storage에 저장됨
  tsdb_shipper:
    active_index_directory: /loki/index
    cache_location: /loki/index_cache

  # AWS S3 Object Storage 설정
  aws:
    bucketnames: my-loki-bucket
    region: ap-northeast-2

# 저장 포맷 및 Object Storage 지정
schema_config:
  configs:
    - from: 2026-01-01
      store: tsdb # Loki 권장 Index 방식
      object_store: s3 # Index와 Chunk의 영구 저장소
      schema: v13 # 현재 권장 Loki schema
      index:
        prefix: index_
        period: 24h
```

## alloy 설정파일 샘플

- docker 호스트에 alloy 같이 띄우고 `/var/run/docker.sock` 통해 컨테이너 정보에 접근

```
// 1. Docker 컨테이너 검색
discovery.docker "containers" {
  host = "unix:///var/run/docker.sock"
}

// 2. Docker metadata -> Loki label 변환
discovery.relabel "containers" {
  targets = discovery.docker.containers.targets

  // container name -> container
  rule {
    source_labels = ["__meta_docker_container_name"]
    target_label  = "container"
  }

  // docker-compose service label -> service_name
  rule {
    source_labels = ["__meta_docker_container_label_com_docker_compose_service"]
    target_label  = "service_name"
  }

  // 고정 label
  rule {
    target_label = "env"
    replacement  = "dev"
  }
}

// 3. Docker 로그 읽기
loki.source.docker "containers" {
  host    = "unix:///var/run/docker.sock"
  targets = discovery.relabel.containers.output

  forward_to = [loki.process.logs.receiver]
}

// 4. 로그 가공
loki.process "logs" {

  stage.json {
    expressions = {
      trace_id = "trace_id",
    }
  }

  stage.structured_metadata {
    values = {
      trace_id = "trace_id",
    }
  }

  forward_to = [loki.write.local.receiver]
}

// 5. Loki 전송
loki.write "local" {
  endpoint {
    url = "<http://loki:3100/loki/api/v1/push>"
  }
}
```

## reference

- 모범 사례: https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/
- 공식 문서: https://grafana.com/docs/loki/latest/get-started/labels/
- claude, codex