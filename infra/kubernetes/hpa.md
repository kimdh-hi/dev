# HPA

## HPA (Horizontal-Pod-Autoscaler)

- 파드의 cpu, 메모리 등 사용률이 따라 파드의 수를 늘리고 줄이는 기능

## 설정

### deployment.yaml

- `resoruces.requests` 설정
- 해당 파드는 최소 선언한만큼의 자원을 필요로 하다는 것을 의미
- hpa 사용시 replicas 설정은 제외하는 것을 권장
    - 최초 replicas 만큼 pod 가 구동
    - 이후 부하가 늘어서 hpa 설정에 의해 pod 오토스케일링
    - 이후 새롭게 kubectl apply -f deployment.yml 시 오토스케일링 된 pod 의 수는 무시되고 replicas 만큼의 파드만 남기고 즉시 종료
    - 재배포시마다 불필요한 축소 -> 재확장이 반복될 수 있으므로 HPA 에 의해서 파드 수가 결정되도록 하는 것을 권장
    - https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/#migrating-deployments-and-statefulsets-to-horizontal-autoscaling
- spec.replicas 를 설정하지 않으면 기본값 1이 지정되는데 재배포시 1만큼 줄어들고 다시 hpa 설정만큼 늘어나는거 아닌가?
    - replicas 를 생략하면 기본값은 1이 맞지만 기본값 1은 오브젝트에 값이 설정되지 않은 경우에만 사용됨
    - 즉, hpa 에 의해 이전 오브젝트 내 replicas 가 이미 늘어나있다면 replicas 생략시 1로 다시 설정하지 않음
    - spec.replicas 의 default 값이 1이라는 것은 생략이 deployment.yml 의 spec.replicas 를 1로 설정하는 것이 아니고, 오브젝트 설정시 replicas 가 없다면 1로 설정한다는 의미

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: web
spec:
  # replicas: 3 # HPA 사용시 설정 제외 (권장)
  selector:
    matchLabels:
      app: web
  template:
    metadata:
      labels:
        app: web
    spec:
      containers:
        - name: web
          image: myapp:1.0
          resources:
            requests: # HPA 시 필수 설정
              cpu: 200m # 0.2코어
              memory: 256Mi # 256메비마이트
```

### hpa.yaml

- averageUtilization: 60 의미
    - deployment 의 requests 가 200m 이므로 60%에 해당하는 120m이 파드의 평균 cpu 지표가 되도록 파드수를 minReplicas~maxReplicas 사이에서 조정
- minReplicas 를 지정하므로 deployment.yml 에서 별도 replicas 를 지정하지 않아도 최소 pod 수는 유지됨
    - 단, hpa 가 일정 주기(15s) 마다 루프를 돌며넛 minReplicas 만큼 deployment 객체의 spec.replicas 를 갱신하는 것이므로 15초 경과 전까지는 space.replicas 의 기본값 대로 파드가 1개만 구동될 수 있음

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: web-hpa
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: web          # Deployment 이름과 일치
  minReplicas: 2
  maxReplicas: 10
  metrics:
    - type: Resource
      resource:
        name: cpu
        target:
          type: Utilization
          averageUtilization: 60
```

#### metrics 설정

metrics[].type — 메트릭 소스

| type | 대상 | 필요 컴포넌트 |
| --- | --- | --- |
| Resource | 파드 전체의 CPU/메모리 (컨테이너 합산) | metrics-server |
| ContainerResource | 특정 컨테이너의 CPU/메모리 | metrics-server |
| Pods | 파드별 커스텀 메트릭의 평균 | custom metrics 어댑터 |
| Object | 다른 쿠버네티스 오브젝트의 값 (describedObject 필수) | custom metrics 어댑터 |
| External | 클러스터 외부 지표 (큐 길이 등) | external metrics 어댑터 |
- Resource는 파드 내 모든 컨테이너를 합산하므로,
사이드카가 있으면 앱 컨테이너의 부하가 희석됨 → ContainerResource 사용

### 계산식

```
필요한 파드 수 = 올림[ 현재 파드 수 × (현재값/목표값) ]
```

- 현재 파드 3개, 목표 100m, 현재 평균 200m → 3 × 2 = 6개

## 동작

- https://kubernetes.io/docs/concepts/workloads/autoscaling/horizontal-pod-autoscale/#how-does-a-horizontalpodautoscaler-work
- HPA 는 일정 주기마다 도는 루프
- 기본 주기는 15초이고, kube-controller-manager의 --horizontal-pod-autoscaler-sync-period 에서 커스텀 가능

```
1. scaleTargetRef 로 Deployment를 찾는다
2. 그 Deployment의 selector 라벨로 파드들을 고른다
3. metrics API 에서 각 파드의 CPU 사용량을 가져온다
4. 평균 내서 목표값과 비교 → 필요한 파드 수 계산
5. Deployment의 replicas 를 그 값으로 바꾼다
```

- 단, HPA 는 직접 파드를 만들지 않는다.
- deployment 객체의 spec.replicas 를 갱신하고 실제 파드 생성은 deployment 가 수행

### 확장/축소 속도 (기본값)

- **확장**: 조건 충족 시 즉시
- **축소**: 최근 5분간 추천값 중 최댓값 채택 → 5분 내내 낮아야 축소