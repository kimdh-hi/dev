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