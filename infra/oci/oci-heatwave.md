# oci heatwave

## heatwave maintenance 
- 보안, 성능, 안정성을 유지하기 위한 월1회 구동되는 정기적 스케쥴링
- 주기는 월 1회로 고정
- 시작시간(maintenance window start time) 지정 가능 
  - 시작 시간으로부터 ±2 이내 시작
  - 02:00 지정시 00:00~04:00 이내 시작
- 시작 시간 지정하지 않는 경우 자동 지정
  - If you do not define the Maintenance window start time, Oracle defines one for you
  - 시작 시간 지정하는 것을 권장.
- minor 버전 변경 자동으로 일어나지 않음.
  - 8.0.38 -> 8.0.39 수동 업그레이드 필요.
  - 단, 해당 버전 지원 종료되는 경우 minor 자동 업그레이드
- maintenance 시 downtime 최소화하려면 cluster 를 통한 HA 구성을 권장

## reference

### heatwave maintenance 
- https://blogs.oracle.com/mysql/understanding-maintenance-in-heatwave-mysql-service
- https://docs.oracle.com/en-us/iaas/mysql-database/doc/overview-maintenance.html
- https://docs.oracle.com/en-us/iaas/mysql-database/doc/maintenance-high-availability-db-system.html