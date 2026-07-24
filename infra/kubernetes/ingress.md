# Ingress

- k8s 클러스터 외부에서 내부 서비스로 향하는 http/https 경로를 노출하는 api 객체
- 트래픽 라우팅은 ingress 리소스 내에 정의된 규칙에 의해 제어
- 클러스터 외부에서 서비스에 접근 가능한 URL를 부여하고, 부하분산, TLS 종료처리 등을 구성 가능

```
pod, service 등은 클러스터 내에서만 유효한 가상 ip를 가진다.
즉, 외부에서는 http/https 로 직접 접근 가능한 방법이 없음.

service.type 중 NodePort와 LoadBalancer 를 통해 외부에 서비스를 노출시킬 수 있지만 http/https 기반이 아님
- NodePort, LoadBalancer 모두 L4 계층에서 포트 기반으로 트래픽 전달
```

- IngressClass 에 대상을 지정하면 해당하는 IngressController 가 Ingress 규칙을 읽어서 실제 L7 로드밸런서를 프로비저닝

```yaml
https://kubernetes.io/docs/concepts/services-networking/gateway/

Ingress API 는 k8s v1.19부터 stable(GA)
Ingress API 는 frozen 되었고, Ingress 대신 Gateway API 사용을 권장
```

- Gateway API 권장
    
    ```yaml
    Note:
    The Kubernetes project recommends using Gateway instead of Ingress. The Ingress API has been frozen.
    
    This means that:
    
    The Ingress API is generally available, and is subject to the stability guarantees for generally available APIs. The Kubernetes project has no plans to remove Ingress from Kubernetes.
    The Ingress API is no longer being developed, and will have no further changes or updates made to it.
    ```
    

### ALB Ingress 샘플

- Ingress resource 만 만들면 의미 없음.
    - 클러스터 내 Ingress Controller 설치 필요 (use Helm)
    - 보통 Ingress Controller 설치시 IngressClass 자동 생성됨

```yaml
# IngressClass 생성
apiVersion: networking.k8s.io/v1
kind: IngressClass
metadata:
  name: alb
spec:
  controller: ingress.k8s.aws/alb
```

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: app-ingress
  annotations:
    alb.ingress.kubernetes.io/scheme: internet-facing
    alb.ingress.kubernetes.io/target-type: ip
    alb.ingress.kubernetes.io/listen-ports: '[{"HTTP": 80}]'
spec:
  ingressClassName: alb
  rules:
  - http:
      paths:
      - path: /core
        pathType: Prefix
        backend:
          service:
            name: core-backend
            port:
              number: 80
      - path: /chat
        pathType: Prefix
        backend:
          service:
            name: chat-backend
            port:
              number: 80
```

- /core → core-backend service
- /chat → chat-backend service

## Reference

- https://kubernetes.io/docs/concepts/services-networking/ingress/
- claude