## kubelet

- 노드에서 실행되는 기본 노드 에이전트
    - 노드마다 실행되는 것은 DaemonSet 과 동일
    - 노드마다 한 개씩 파드로써 존재하는 DaemonSet 과는 다른 개념
    - DaemonSet 도 결국 파드이므로 누군가 노드에 띄워줘야하는데 이 역할을 하는게 kubelet
- kubelet 역할
    - 파드 관리
    - 노드 관리
- kube-apiserver 를 통해 pod 정보(PodSpec) 제공받고 PodSpec 에 기술된 대로 Pod 를 실행하고 주기적으로 모니터링
    - 노드에 직접 docker run 으로 띄운 컨테이너는 kubelet 의 관리 대상이 아님
    - kubelet 이 직접 PodSpec 에 따라 컨테이너를 시작/중지 등을 하진 않고 Container Runtime 을 통한다. (containerd)
- 노드 등록
- 노드 상태 보고 (heartbeat)
    - heartbeat 방식
        - Lease
        - NodeSatus
    - Lease 방식으로 해당 Node 가 살아있는지 확인하고 Node 장애로 판정하는 트리거 또한 Lease 방식 통해서만 가능
        - NodeStatus 는 모니터링 결과를 저장하고 Lease 보다는 긴 주기로 갱신되며 scheduler 의 배치 결정에 사용
    - Lease
        - `kube-node-lease` 라는 네임스페이스를 가진 노드 내 객체
        - node 가 살아있음에 대한 시각(renewTime)만 짧은 주기로 갱신 (default: 10s)
        - leaseDurationSeconds 초과되는 경우에 대한 후처리는 크게 없음.
        - Control Plane 구성요소 중 `kube-controller-manager` 의 `node controller` 가 각 lease 객체의 renewTime 이 `node-monitor-grace-period` 만큼 시간동안 갱신되지 않은 경우 node 상태를 ready → unknown 으로 갱신
            - 해당 노드에는 pod 배치 x
    
    ```bash
    kubectl get lease -n kube-node-lease
    kubectl get lease <노드이름> -n kube-node-lease -o yaml
    
    spec:
      holderIdentity: <노드이름>
      renewTime: "2026-07-15T..."   # ← kubelet이 10초마다 갱신하는 값
      leaseDurationSeconds: 40
    ```
    
    - NodeStatus
        - kubelet 은 apiserver 를 통해 자신의 Node 객체의 status(NodeStatus) 를 갱신
            - NodeStatus
                - Node Conditions (Ready, MemoryPressure, DiskPressure, PIDPressure)
                - capacity
                - allocatable
                - addresses
                - nodeInfo
                - …
        - NodeStatus 갱신은 고비용 작업

## on-premise k8s  kubelet 통한 node 투입 절차

- 물리 서버 리소스를 준비
- 해당 서버에 kubelet 설치 및 systemd 등록
- kubelet 설치시 어떤 k8s 클러스터에 붙어야 하는지 명시 필요
- kubelet 실행시 지정된 클러스터의 apiserver 로 본인 등록 요청 (self-register)
- 매니지드 서비스의 경우

## 노드 장애시 후처리

1. 노드 장애 판정
2. 해당 노드 내 파드 재배치 위해 생성
3. 배치 가능한 노드가 없는 경우 Pending Pod 발생
4. Pending Pod 발생하는 경우 Autoscaler 에 의해 클라우드 api 통해 서버 인스턴스(VM) 생성 요청
    1. 생성되는 VM 내에는 kubelet 설치 및 현재 클러스터 정보 등 설정됨
5. 생성완료되어 kubelet 실행되는 경우 apiserver 로 본인 노드 등록 요청
6. 요청 수락되어 클러스터 내 새로운 노드 배치
7. 해당 노드 내 Pending Pod 배치

## Reference

- https://tech.osci.kr/kubelet/
- https://kubernetes.io/ko/docs/concepts/architecture/#kubelet