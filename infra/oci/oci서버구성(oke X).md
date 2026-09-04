# oci 서버 구성 (oke X)

```
aaa.com     ──┐
              ├─► NLB(L4) :443 ──► HAProxy
aaa-api.com ──┘                      (TLS 종료, Host/경로 기반 라우팅)
                                       │
                                       ▼
                                     L7 LB
                                  ├─ :8080 → Apache Pool
                                  │          (Instance Pool)
                                  └─ :8081 → WAS Pool
                                             (Instance Pool)
```

## L4 LB (NLB)

- 고정 public IP 할당 (Reserved Public IP 할당 가능)
- HAProxy 이중화 용도(HAProxy Active-Active 운영 가능)
- Source IP를 HAProxy 까지 보존 (Backend Set의 Preserve Source IP 활성화 시)
    - HAProxy TLS 종료 후 WAS로 client ip 전달 필요한 경우 X-Forwarded-For 통해 전달
- https://docs.oracle.com/en-us/iaas/Content/NetworkLoadBalancer/introduction.htm

## HAProxy

- TLS 종료
- 경로 or 호스트 기반 라우팅
- 경로/호스트 기반 분기 이후 L7 LB의 해당 리스너 포트로 전달

## L7 LB

- was 서버 로드밸런싱
- Instance Pool (aws: Auto scaling Group) 에 L7 LB를 붙인다.
- was 서버 증설시 HAProxy 설정변경 필요없이 증설된 was 서버로 트래픽 전달
- instance pool 에 LB 연결시 해당 instance pool 에 인스턴스 추가시 해당 인스턴스는 LB의 backend set 에 자동 추가
    - 오토스케일링 필요시 따로 설정 (Autoscaling Configuration, Instance Configuration)
- https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/updatinginstancepool_topic-To_attach_a_load_balancer_to_an_instance_pool.htm

## 다른 구조

- `client -> L7 LB -> apache/was`
    - O
    - 가장 단순, 표준
    - L7 LB 만으로 요구사항 충족이 가능한 경우 선택
    - host/path 라우팅, reserved public ip, instance pool 모두 사용 가능
- `client -> L7 LB -> haproxy -> apache/was`
    - X
    - haproxy 가 was 를 선택하므로 instance pool 사용시 수동으로 haproxy 에 등록 필요 (LB backend set 자동 등록 사용 불가)
    - haproxy 의 기능 사용 위함이라면 instance pool 사용 가능한 다른 구조 검토 필요
- `client -> L4 LB -> L7 LB -> apache/was`
    - X
    - L7 LB 도 이미 매니지드 서비스이므로 이중화 등 불필요 (L4가 의미 없어짐)
    - L7 LB 도 reserved public ip 생성해 할당 가능. 즉, 고정 IP 때문에 NLB 를 앞에 둘 이유 없음
    - https://docs.oracle.com/en-us/iaas/Content/Network/Tasks/managingpublicIPs.htm
- `client -> L4 LB -> haproxy -> L7 LB -> apache/was`
    - O
    - haproxy 이중화 (L4 LB 담당)
    - instance pool 사용 가능 (L7 LB 담당)
    - haproxy 사용이 반드시 필요한 경우 client -> L7 LB -> apache/was 구조 대신 고려