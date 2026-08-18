# DNS

## DNS (Domain Name System)

- 도메인 이름을 IP 주소로 변환해주는 분산 데이터베이스 시스템이자 질의, 응답 프로토콜
- 사람은 숫자 형태의 IP 주소를 외우고 접근하는데 어려움이 있기 때문에 IP 에 의미있는 이름을 부여하고 이를 매핑해서 접근하기 위함
- DNS 는 분산, 계층구조라는 특징을 갖는다.

## DNS 이름(도메인 이름) 계층 구조

- www.example.com.
    - www: 3단계 도메인 (관례적으로 호스트명)
    - example: SLD (Second-Level-Domain)
    - com: TLD (Top-Level-Domain)
    - "": 루트

### TLD DNS

- https://www.iana.org/domains/root/db
- TLD 종류
    - gTLD(일반): .com, .net, .org
    - ccTLD(국가): .kr, .jp, .uk
    - new gTLD: .app, .dev, .shop

### 루트 DNS

- https://한국인터넷정보센터.한국/jsp/statboard/dns/dnsRoot/currentWorld.jsp
- https://root-servers.org/
- 전세계 13개 루트 DNS 주소가 있음 (실제 수백 대의 서버 네트워크로 구성)
- 루트 DNS 는 개별 도메인 정보를 저장하지 않고 TLD 위임 정보를 관리하므로 TLD DNS 주소를 응답 (리퍼럴 반환)
    - Local DNS로 TLD DNS 주소를 응답

## DNS 캐싱

- 모든 Local DNS(**recursive resolver)** 질의가 루트→TLD→권한서버 요청을 반복해야 하는 것 방지 위함
- DNS 관련 캐싱은 애플리케이션 수준부터 Local DNS 까지 여러 단계로 구성
- 루트 DNS, TLD DNS, 권한서버 DNS 등 질의시 응답에 포함된 TTL 기반으로 LocalDNS 측에서 캐시 설정

### 브라우저에 www.example.com 입력시 동작

#### 애플리케이션 수준 캐시로부터 IP 확인

- 브라우저 자체 DNS 캐시, OS 캐시, /etc/hosts 등으로부터 DNS 질의없이 ip 획득 시도
- 획득시 이후 DNS 질의 없음

#### Local DNS (재귀 리졸버)로 질의 요청

- Local DNS 질의 후 클라이언트는 이후 과정에 관여하지 않음
- Local DNS: ISP DNS, 사내 DNS, 8.8.8.8(google), 1.1.1.1(cloudflare)...
- Local DNS DNS 캐시 히트시 추가 질의 없이 client 로 IP 응답

#### 루트 DNS 질의

- `.com` 존을 담당하는 TLD DNS 주소 목록을 LocalDNS 로 응답

#### TLD DNS

- TLD: `.com` 존을 담당하는 권한 서버
- `example.com` 존을 담당하는 DNS 주소 목록을 LocalDNS 로 응답

#### `example.com` DNS 질의

- `example.com` 존의 권한서버이므로 자기 존 데이터(ip) 직접 응답
- LocalDNS 로 A레코드(IPv4) 응답 응답

#### LocalDNS 캐싱

- TLD NS, 도메인 NS, A레코드 등 각각 캐싱