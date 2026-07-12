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

## AWS ELB (ALB, NLB)
- AWS ELB (Elastic LoadBalancer)
- target group, listener, Rules 등의 구성요소를 가짐
- managed 서비스로써 트래픽 기준으로 LB 노드 오토스케일링
- ELB 는 여러 AZ(Availability Zone, 가용역역)에 걸쳐 배포됨
  - AZ: 리전 내 독립된 물리적 데이터센터 그룹
  - 여러 AZ 에 거쳐 분산되므로 LB 단일장애지점 이슈 최소화
- NLB(L4), ALB(L7), GWLB(L3) 등이 대표적인 ELB 타입

### AWS NLB (L4, Network Load Balancer)
- AWS L4 LB
- connection 단위 처리, IP 기반 세션 고정 등 일반 L4 LB 와 동일한 기능 제공
- TLS 처리
  - NLB 는 TLS 통과/종료 모두 지원
  - TLS 종료를 지원해도 직접 내용을 읽어 내용 기반 라우팅은 불가
  - L7 LB 가 따로 없는 경우 백엔드 서버에 직접 TLS 관련 부하를 가하지 않기 위함
  - 인증서 관리 중앙화 위함 => ACM (AWS Certificate Manager)

### AWS ALB (L7, Application Load Balancer)
- AWS L7 LB
- L7 LB 기본 기능 동일하게 제공.
  - 요청 단위 처리, 내용 기반 라우팅, TLS 처리, 쿠키 기반 세션 고정

## L4/L7 선택
- L7 LB 는  L4 LB 의 완전한 상위집합이 아니다.
- 각각 특징이 있으므로 요구사항에 맞게 선택해야 함.
- L4 선택 기준
  - HTTP/HTTPS/gRPC 프로토콜이 아닌 경우
  - 디테일한 라우팅룰 적용이 필요없는 반면 극한의 처리량을 요구하는 경우
    - L4 LB 는 메세지 파싱/복호화 등이 없으므로 처리량 측면에서 우위
  - 원본 클라이언트 IP 를 X-Forwarded-For 가 아닌 TCP 커넥션 자체에서 획득해야 하는 경우
  - 고정 IP 가 필요한 경우
    - L7 ALB 의 경우 고정 IP 가 아님.
    - L4 NLB 는 서브넷(AZ) 마다 고정 IP 부여 (필요시 Elastic IP 직접 설정 가능)
      - 우리 서버로 인바운드 요청을 보내는 타서버에서 우리 서버 IP 를 화이트리스트로 관리하고 싶은 경우 고정된 IP 를 필요로 함. 
  - TLS 를 BE 까지 그대로 넘겨야 하는 경우 (tls passthrough)
    - L7 은 콘텐츠 기반 라우팅이므로 tls 복호화 수행하므로 mTLS 등 클라이언트의 신원 파악 위해 BE 로 tls 그대로 넘기지 못함.
    - 단, SNI 기반 1라우팅만 필요로 하는 경우 L7 에서도 passthrough 가능 (HAProxy mode tcp-SNI 검사, Nginx stream-sslpreread)
- 일반적으로 api 등 백엔드 서버 앞 단에는 L7 LB 를 배치
- 만약 고정 IP/tls passthrough, 경로/헤더 등 콘텐츠 기반 라우팅 가 동시에 필요한 경우 L4/L7 모두 사용도 가능


## reference

- https://aws-hyoh.tistory.com/149
- https://docs.aws.amazon.com/elasticloadbalancing/latest/network/load-balancer-listeners.html
- https://docs.nginx.com/nginx/admin-guide/load-balancer/http-load-balancer/
- https://www.haproxy.com/blog/layer-4-and-layer-7-proxy-mode
- https://docs.aws.amazon.com/elasticloadbalancing/latest/application/listener-rules.html
