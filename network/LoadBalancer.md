# LoadBalancer (LB)

## L4 LoadBalancer
- 전송계층(L4, TCP/UDP) 로드밸런서
- 내용은 읽지 않고 connection 단위로 처리
  - 일반적으로 내용은 HTTP 메세지를 의미
  - 새 connection 이 들어올 때 어느 쪽으로 보낼지 결정하고, 이후 트래픽은 같은 대상으로 보냄
- 새 connection 에 대한 연결을 맺을 서버 결정 기준
  - 라운드 로빈
  - Least Connection (활성 연결 수가 가장 적은 서버를 대상으로)
  - 그 외 ip/port, protocol 만으로 대상 서버를 결정 가능
- TLS 복호화하지 않음.
  - HTTP 메세지 내용을 직접 파싱하지 않음.
  - 별도 인증서 불필요
- 내용 파싱 및 복호화 과정이 없으므로 오버헤드가 적고 처리량이 높음
- 단, 내용을 보지 않으므로 URL, 헤더, 쿠키 등을 기반으로하는 라우팅 불가.
  - L4 이므로 IP 는 볼수있으니 client IP 해시 기반 모듈러 연산 통해 고정된 서버로 연결을 고정하는 것 정도만 가능
  - 단, IP 가 바뀌면 세션이 끊어짐, 동일 IP(NAT) 쓰는 환경이라면 모든 요청이 동일 서버로 집중되어 부하분산x
- 소스 IP 보존됨
- AWS NLB, HAProxy(mode tcp), NGINX(stream)

## L7 LoadBalancer
- 애플리케이션(L7, HTTP) 로드밸런서
- connection 단위가 아닌 각 요청 단위 부하분산
- HTTP 메세지를 파싱하므로 헤더, 쿠키, 메서드 등 파싱해서 분산 결정 요소로 사용 가능
- 내용(HTTP 메세지..) 등을 읽을 수 있으므로 콘텐츠 기반 라우팅 가능
  - `/user` 는 인증 서버로, `/chat` 은 채팅서버로
- 세션 고정
  - L4 와 같이 IP 기반 불완전한 형태로 세션 고정 X
  - 쿠키 기반으로 세션 고정 (JSESSIONID)
- 메세지 내용을 파싱하므로 TLS 인증서 필요
  - TLS termination 지원
  - End-to-End TLS 지원
- 메세지 파싱, 복호화 등 과정이 있으므로 L4 와 비교하여 오버헤드 큼
- X-Forwarded-For 헤더 통해 소스 IP 보존
- AWS ALB, HAProxy, NGINX, Envoy..

## AWS NLB (L4, Network Load Balancer)
- AWS L4 LB
- connection 단위 처리, IP 기반 세션 고정 등 일반 L4 LB 와 동일한 기능 제공
- TLS 처리
  - NLB 는 TLS 통과/종료 모두 지원
  - TLS 종료를 지원해도 직접 내용을 읽어 내용 기반 라우팅은 불가
  - L7 LB 가 따로 없는 경우 백엔드 서버에 직접 TLS 관련 부하를 가하지 않기 위함
  - 인증서 관리 중앙화 위함 => ACM (AWS Certificate Manager)

## AWS ALB (L7, Application Load Balancer)
- AWS L7 LB
- L7 LB 기본 기능 동일하게 제공.
  - 요청 단위 처리, 내용 기반 라우팅, TLS 처리, 쿠키 기반 세션 고정

## reference

- https://aws-hyoh.tistory.com/149
- https://docs.aws.amazon.com/elasticloadbalancing/latest/network/load-balancer-listeners.html
- https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/
- https://www.haproxy.com/blog/layer-4-and-layer-7-proxy-mode
- https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-rules.html
