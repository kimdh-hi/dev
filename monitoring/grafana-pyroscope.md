# Grafana Pyroscope

## 프로파일링
- https://grafana.com/docs/pyroscope/latest/introduction/what-is-profiling/
- 매트릭을 수집하면 현재 CPU 사용률을 알 수 있다.
- 수집된 매트릭을 통해 현재 CPU 사용률이 90%라는 것을 알 수 있을 때, 프로파일링은 90% cpu 사용률이 코드의 어떤 함수에서 쓰고 있는지를 답한다.
- 프로파일링을 통해 코드의 어떤 부분에서 cpu, 메모리 등의 리소스가 많이 사용되는지를 파악하여 리소스 사용량을 최적화할 수 있다.

### 데이터 수집 방식
- https://grafana.com/docs/pyroscope/latest/introduction/what-is-profiling/#traditional-profiling-non-continuous
- Pyroscope 가 공식적으로 제공하는 수집도구(SDK, Alloy) 는 모두 샘플링 기반 프로파일러

#### 샘플링 방식
- 프로파일러가 일정 간격으로 프로그램을 중단시키고 프로그램의 상태를 기록, 이 기록들을 분석해 코드의 각 부분이 얼마나 자주 실행되는지를 추론
  - 10ms 마다 현재 어떤 함수가 실행중인가를 기록
  - 100ms 동안 10ms 간격으로 남긴 기록 확인시 어떤 함수가 전체 실행시간 중 얼마나 시간을 사용했는지 알 수 있음

```
10ms    handle_request → parse_json
20ms    handle_request → parse_json
30ms    handle_request → parse_json
40ms    handle_request → parse_json
50ms    handle_request → parse_json
60ms    handle_request → parse_json
70ms    handle_request → process_data
80ms    handle_request → process_data
90ms    handle_request → process_data
100ms   handle_request → process_data
```
- parse_json 6회
- process_data 4회
- parse_json 이 전체 시간의 60% 를 쓴 것으로 추정

#### 계측 방식
- 함수마다 실행시간을 측정하는 코드를 직접 추가
- 정확한 호출 횟수와 시간 측정 가능
- 개발 단계에서 주로 사용
- 계측 방식은 상세한 정보를 누락없이 알 수 있지만 추가 코드의 오버헤드로 인해 측정 자체가 애플리케이션에 영향을 끼칠 수 있음

### 비연속/연속 프로파일링
- 언제, 얼마나 오래 프로파일링하는가에 대한 구분
- Pyroscope 는 비연속, 연속 프로파일링 모두 지원
- 두 개 방식으로 혼합하여 사용하는 것 유용
  - 운영환경에서는 연속 프로파일링
  - 개발, 스테이징에서는 비연속 프로파일링 통해 운영환경에서 발견한 이슈 지표를 비연속 프로파일링 통해 더 상세히 확인

#### 비연속 프로파일링 (Traditional profiling)
- https://grafana.com/docs/pyroscope/latest/introduction/what-is-profiling/#traditional-profiling-non-continuous
- 프로파일링을 직접 시작하고 끝내는 방식
- 필요한 시점에 프로파일러를 실행하고, 결과를 확인한 뒤 종료하는 방식
- 예를 들어 지연을 발견했을 때 프로파일러를 켠 상태로 해당 api 호출 테스트나 부하테스트를 실행 후 프로파일러 종료 후 그 결과를 확인
- 장점: 실행 시간은 짧으므로 샘플링 간격을 촘촘하게 하는 것 가능 (프로그램이 지연이 걸려도 그 시간이 짧음)
  - 비연속 프로파일링은 계측 방식을 사용해서 측정하는 것도 고려 가능. 연속 프로파일링에서 계측 방식 자체가 애플리케이션에 부담이 될 수 있으므로 샘플링 방식을 거의 사용 
- 단점: 실제 문제가 발생했던 그 순간을 놓칠 수 있음

#### 연속 프로파일링
- 프로파일러를 운영 환경에서 항상 켜두는 방식
- 프로파일링 데이터를 백그라운드에서 최소한의 오버헤드로 계속 수집
- 애플리케이션에 붙은 Alloy, SDK가 샘플링을 계속 수행하여 모인 기록을 일정 주기로 집계하여 구간 단위의 프로파일을 Pyroscope 서버로 전송
- 장점: 실제 지연 등 이슈가 발생한 과거 시점의 프로파일링 지표 확인이 가능
- 단점: 운영중인 시스템에 영향을 최소화하는 수준으로 샘플링 주기를 설정해야 하므로 비연속 프로파일링보다는 상세함이 떨어짐

