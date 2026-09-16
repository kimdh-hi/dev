# LGTM

## LGTM 스택에서 Prometheus 의 역할

- https://grafana.com/docs/alloy/latest/collect/opentelemetry-to-lgtm-stack/
- 프로메테우스는 메트릭 저장소이자, 수집기, 질의 엔진 등의 역할을 수행
- LGTM 스택에서 Mimir(메트릭 저장소)와 직접적인 연관이 있음.
- Mimir 는 프로메테우스 용 장기 저장소
- 프로메테우스는 애플리케이션이 공개하는 엔드포인트를 통해 매트릭을 스크랩해서 mimir 로 remote write
- Mimir 는 외부 스토리지(S3, GCS 등)에 시계열 매트릭 데이터를 저장하고 집계 질의하는 기능을 제공

### Prometheus vs Mimir

- mimir 도 파일시스템에 매트릭 데이터를 저장 가능한 모드를 지원하지만 실환경이 아닌 테스트 목적으로 사용할 것을 권장
    - 기본값: filesystem
    - 외부 저장소로 s3, gcs, azure, swift 제공
    - https://grafana.com/docs/mimir/latest/configure/configure-object-storage-backend/
    - https://archive.grafana.com/docs/mimir/v2.11.x/configure/configure-object-storage-backend/
- Prometheus 에서 Mimir 전환시 비용 낮음
- 초기 도입시 Prometheus 를 우선검토하고 서비스가 커질 때 Mimir 로 전환 검토할 것.
- grafana 공식 lgtm 올인원 이미지도 prometheus 를 사용하고 있음.
    - https://github.com/grafana/docker-otel-lgtm
    - 위 이미지는 개발, 데모 용도임

### springboot 환경에서 LGTM (Prometheus) 구조

```
1. springboot 는 micrometer 를 통해 매트릭 수집
2. prometheus 의존성이 있는 경우 스크랩용 actuator 엔드포인트 자동 구성 (`/actuator/prometheus`)
3. prometheus 인스턴스는 각 서버 인스턴스의 `/actuator/prometheus` 를 스크랩(폴링)
4. prometheus 인스턴스는 로컬 TSDB 에 저장하고 promQL 통한 질의 수행
```

### LGTM (Alloy + Prometheus)

```
WAS <--/actuator/prometheus-- prometheus (매트릭 스크랩, 질의, 저장)
 ㅣ
 ㅣ- stdout/로그 <--tail-- Alloy --push--> Loki
 ㅣ- OTLP 트레이스 --push--> Alloy --push--> Tempo
```

### Prometheus scrap, Alloy remote write

- application 입장에서는 prometheus 매트릭을 /actuator/prometheus 를 통해 공개하는 것은 동일
- prometheus 가 직접 매트릭을 scrap(인바운드) 할지, alloy 가 매트릭을 수집하고 remote write(아웃바운드) 통해 prometheus 에 적재할지 차이

## exporter

- prometheus 포맷 매트릭을 공개하는 HTTP 엔드포인트 공개 불가능한 경우 별도 exporter 프로세스 플요
- mysql, postgreSQL 등 RDBMS 는 내부 통계를 시스템 뷰 또는 테이블로 노출
- exporter 는 통계 데이터가 쌓이는 뷰 또는 테이블을 조회하고 결과를 가공하여 HTTP 로 노출
    - sql 질의 결과를 prometheus 매트릭으로 변환
- mysql/mariadb db exporter: mysqld_exporter
    - prometheus 에서 제공하는 mysql 용 exporter (mysqld_exporter)
    - prometheus 매트릭을 HTTP 엔드포인트로 노출 (default: `:9104/metrics`)
    - https://github.com/prometheus/mysqld_exporter
- exporter 의 기능은 collector 단위로 on/off
    - mysqld_exporter 지원 collector 목록
    - https://grafana.com/docs/alloy/latest/reference/components/prometheus/prometheus.exporter.mysql/#supported-collectors

### alloy 내장 exporter

- https://grafana.com/docs/alloy/latest/reference/components/prometheus/prometheus.exporter.mysql/
- alloy 는 별도 exporter 프로세스를 띄우지 않아도 되도록 exporter를 내장한 컴포넌트를 제공
    - prometheus.exporter.mysql 컴포넌트는 mysql 통계 수집을 위해 mysqld_exporter를 내장
- alloy 프로세스 내 내장된 exporter 가 mysql sql 질의를 통해 스크래핑 및 prometheus 매트릭으로 가공 후 alloy remote write 통해 백엔드(mimir, prometheus..) 로 push
- sql 질의 주기
    - exporter 자체는 주기를 갖지 않는다.
    - 스크래핑 요청이 들어온 시점에 sql 질의하므로 alloy scrape 주기(scrape_interval) 로 sql 질의 주기가 결정

```
DB/Redis <--scraping-- alloy(exporter) --remote write(push)--> prometheus
```

```
prometheus.exporter.mysql "label" {
  data_source_name = "mysql-username:mysql-password@tcp(127.0.0.1:3306)/"
}

prometheus.exporter.redis "label" {
  redis_addr = "127.0.0.1:6379"
}

prometheus.scrape "mysql" {
  targets = discovery.relabel.mysql.output
  job_name = "mysql"
  scrape_interval = "30s"
  forward_to = [prometheus.remote_write.monitoring.receiver]
}

prometheus.scrape "redis" {
  targets = discovery.relabel.redis.output
  job_name = "redis"
  scrape_interval = "30s"
  forward_to = [prometheus.remote_write.monitoring.receiver]
}

discovery.relabel "redis" {
  targets = prometheus.exporter.redis.beta.targets

  rule {
    target_label = "instance"
    replacement = "[redis-server-ip]:6379"
  }

  rule {
    target_label = "job"
    replacement = "redis"
  }
}

discovery.relabel "mysql" {
  targets = prometheus.exporter.mysql.beta.targets

  rule {
    target_label = "instance"
    replacement = "[db-server-ip]:3306"
  }

  rule {
    target_label = "job"
    replacement = "mysql"
  }
}

prometheus.remote_write "monitoring" {
  external_labels = {
    env = "label",
    host = "db",
  }

  endpoint {
    url = "http://[prometheus-ip]:9090/api/v1/write"
  }
}

//...
```