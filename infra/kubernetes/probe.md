# kubernetes probe

## Liveness Probe
- 컨테이너 상태에 따라 컨테이너 재시작 여부를 판단 
- 초기 delay, delay 주기 등 설정 가능 (initialDelaySeconds, periodSeconds)

## Readiness Probe
- 컨테이너가 트래픽을 받을 수 있는지를 판단
- Readiness 여부에 따라 해당 Service 에서 제외
- 초기 delay, delay 주기 등 설정 가능 (initialDelaySeconds, periodSeconds)

## Startup Probe
- 컨테이너 시작에 소요되는 시간을 기다리기 위함
- 컨테이너가 시작되는 동안 불필요한 Liveness, Readiness 판단을 최소화하기 위함
- Startup 조건이 만족 된 이후 Liveness, Readiness 를 판단
- failureThreshold * periodSeconds 동안 컨테이너 실행 실행됨 여부를 판단
- failureThreshold * periodSeconds 이전 successThreshold 만큼 검증에 성공하는 경우 나머지 probe 실행됨


## sample

```yaml
apiVersion: v1
kind: Pod
metadata:
  #metadata....
spec:
  containers:
  - name: test
    image: testImage
    readinessProbe:
      failureThreshold: 3
      initialDelaySeconds: 5
      periodSeconds: 5
      httpGet:
        path: /health-check
        port: 8080
        scheme: HTTP
    startupProbe:
      failureThreshold: 20
      periodSeconds: 5
      successThreshold: 1
      httpGet:
        path: /health-check
        port: 8080
        scheme: HTTP

```