# kubernetes ingress

## ingress
- cluster 외부에서 cluster 내부 service 로의 접근을 관리
- uri, hostname, path 등 규칙으로 트래픽을 백엔드 서비스로 라우팅
- L7 라우팅 계층

## ingress 기능
- HTTP/HTTPS 트래픽 노출
  - 기본적으로 pod, service 등은 외부에서 접근 불가한 사설 ip 를 가짐
  - ingress 는 외부 http/https 요청을 내부 pod, service 로 연결
- load balancing
- name-based virtual hosting
  - a.com/b.com --> 동일 ingress ip 로 호스팅
- ssl termination
  - 외부 https 요청을 복호화하여 cluster 내부로는 http 요청으로 전달
  - 각 pod SSL 처리를 위한 인증서 관리 필요 없음

## ingress controller
- ingress resource(ruls) 에 따라 동작하는 프록시 서버 (nginx, haproxy ..)
- ingress 에 대한 ingress resource 을 정의하는 ingress yaml 작성과 별개로 cluster 에 ingress controller 설치 필요

## ingress class
- ingress resource는 ingressClass 를 사용해서 특정 ingress controller 를 지정
- 동일 클러스터 내에서 여러 ingress controller 사용시 유용
- ingress yaml 작성시 ingressClassName 필드에 명시

```
ingress 는 ingress class 를 가리키고, 
ingress class 는 여러 ingress controller 중 해당 클래스를 담당하는 ingress controller 를 실행
```


---

# reference
- https://kubernetes.io/docs/concepts/services-networking/ingress/