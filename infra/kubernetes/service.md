# Service

- pod 는 언제든 죽고 재생성 될 수 있고, 재생성시 IP 가 바뀌므로 Pod 의 IP 로 직접 접근하는 방식 사용 불가
- Service 는 Pod 앞 단에서 안정적인 엔드포인트 제공
- Service 는 Label selector 로 Pod 를 찾고, kube-proxy 가 iptables/IPVS 규칙대로 트래픽 라우팅

## Service type

### ClusterIP (default)

- 클러스터 내부에서만 접근 가능한 가상 IP 할당

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  type: ClusterIP       # 생략 가능 (기본값)
  selector:
    app: my-app
  ports:
    - port: 80          # Service 포트
      targetPort: 8080  # Pod 포트
```

- 마이크로서비스 pod 간 통신에 사용 (외부에서 접근 불가)

### NodePort

- 노드의 특정 포트(30000~32767) 을 열어서 외부로부터 접근 허용

```yaml
spec:
  type: NodePort
  selector:
    app: my-app
  ports:
    - port: 80
      targetPort: 8080
      nodePort: 30080   # 생략 시 자동 할당
```

- 테스트/개발 환경 용도
- 운영환경에서 사용하지 않음

### LoadBalancer

- 외부 로드밸런서를 자동 프로비저닝

```yaml
spec:
  type: LoadBalancer
  selector:
    app: my-app
  ports:
    - port: 80
      targetPort: 8080
```

## Service Discovery

### DNS 방식 (권장)

- CoreDNS 가 DNS 레코드 생성

```yaml
<service-name>.<namespace>.svc.cluster.local --> 10.96.218.94
```

| 접근 범위 | 주소 |
| --- | --- |
| 같은 네임스페이스 | `sevice-name` |
| 다른 네임스페이스 | `sevice-name.namespace` |
| 전체 FQDN | `my-service.production.svc.cluster.local` |

## sessionAffinity: ClientIP

- sessionAffinity
    - default: None
- service 는 랜덤/라운드로빈 기반으로 pod 로 요청을 보냄
- sessionAffinity: CLientIP 설정시
    - 같은 클라이언트 IP 요청은 항상 같은 Pod 로 보냄 (sticky)
- timeoutSeconds 통해 sticky 시간 설정 가능

```yaml
apiVersion: v1
kind: Service
metadata:
  name: my-service
spec:
  selector:
    app: my-app
  sessionAffinity: ClientIP
  sessionAffinityConfig:
    clientIP:
      timeoutSeconds: 10800   # 기본 3시간
  ports:
    - port: 80
      targetPort: 8080
```

## externalTrafficPolicy

- Service 설정
- default: `Cluster`
- LB의 노드 선택 기준과 관계없이 각 노드가 다시 전체 Pod 대상으로 재분배
- 장점
    - LB의 노드 부하분산관 무관하게 Pod 간 고르게 부하분산 가능
- 단점
    - 네트워크 홉 수 증가 가능
        - LB → A node
        - A node → B node (b-1 pod)
    - 실제 처리를 담당한 node의 출발지 IP가 가려질 수 있음
        - A node → B node (B-1 pod) 로 재분배 시
        - B node 로 넘길 때 출발지 IP 를 A node 것으로 변경 (SNAT)
        - 실제 응답을 B node 가 아닌 A node 가 받도록

```yaml
externalTrafficPolicy: Cluster

앞 단의 LB 통해 특정 노드로 요청이 들어온 경우 해당 노드의 파드로 요청이 가지 않을 수 있음.
즉, LB 의 노드 선택 기준과 파드 선택 기준은 서로 독립적임.

LB 가 노드로 얼마나 균등하게 부하를 분산하든 각 노드가 다시 전체 Pod 로 재분배하므로 모든 Pod 간 
고르게 부하분산 가능

```

- `externalTrafficPolicy: Local`
    - pod 로 재분배 x
    - LB 가 결정한 노드내 pod 로 분산
    - `externalTrafficPolicy: Cluster` 의 네트워크 홉 수 가 늘어나는 단점 해결 가능
    - 노드 당 pod 가 중첩 배치되는 걸 제외하는 상태에서 앞 단의 LB 가 각 node 적절히 부하분산을 해주는 상태라면 `externalTrafficPolicy: Local` 고려
    - 주의,
        - 노드 당 pod 1:1 배치가 깨지는 경우 pod 단위 부하분산 고르지 않을 수 있음
        - 외부 요청(LB 를 통하는 요청)이 아닌 내부 마이크로서비스 간 요청의 경우 자기 노드에 있는 pod 로만 대상이 좁혀짐
        - 자기 노드에 대상 pod 가 없는 경우 요청 실패
    
    ⇒ 그냥 기본값 사용하자.