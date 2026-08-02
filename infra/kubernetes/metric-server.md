## Metrics Server

- 클러스터 전체의 cpu, 메모리 사용량을 모아서 kubernetes api 로 노출해주는 애드온
    - 기본 내장이 아니므로 별도 설치해야 하는 클러스터 애드온
- 각 노드의 kubelet 으로부터 메트릭 수집한 뒤 Metrics API 를 통해 kube-apiserver 에 노출하여 HPA, VPA 가 사용할 수 있도록 한다.
- metrics api 는 `kubectl top pods` 으로 접근 가능 (디버깅 용도)
- Metrics Server 는 오토스케일링 목적으로만 설계됨
- 별도 모니터링 솔루션에서 사용할 데이터 소스로 사용x
    - 위 목적이라면 kubelet 의 `/metrics/resource` 엔트포인터로부터 직접 수집
- HPA, VPA 를 위한 데이터 공급기
- HPA, VPA 사용시 cpu, memory 지표를 필요로 하는 Resource, ContainerResource 로 설정된 경우 metrics-server 설치 필요
    - 별도 커스텀 매트릭만을 필요로 하는 경우 metrics-server 는 필수 아님 (`metrics[].type`: `External`/`Pods`/`Object` )
- https://github.com/kubernetes-sigs/metrics-server
- https://kubernetes-sigs.github.io/metrics-server/