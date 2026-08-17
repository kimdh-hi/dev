# CDN

## CDN (Content Delivery Network)
- 여러 리전에 분산 배치된 서버 그룹으로, 같은 데이터의 복제본을 각 서버가 저장하고 사용자의 요청정보 기반으로 최적의 서버가 요청을 처리
- html, js, image 등 정적 리소스를 사용자 근처에 캐싱해 전송 속도 향상을 목적으로 함.
- 핵심 목표
  - 물리적 거리 단축을 통한 네트워크 지연 최소화
    - 네트워크 지연은 물리적 거리에 비례
  - 원본 서버 부하 감소
    - 동인한 파일 요청을 엣지에서 처리하므로 원본 서버로 가는 요청 수 감소

## CDN 구성요소
- Origin (원본 서버)
  - 리소스 원본이 저장된 곳 (S3 버킷, ec2 웹서버, 온프레미스 서버 등)
- PoP(Point-of-Presence)/Edge Location
  - 전 세계 배치된 CDN 데이터센터, 실제 캐시가 저장되는 지점
- Cache Key
  - 요청을 같은 요청으로 볼지 판단하는 식별자
  - cloudfront 의 경우 요청 객체의 URL
  - cloudflare 의 경우 전체 URL, cors header 등 더 만흔 것으로 키로 사용
  - 별도 cache policy 로 커스텀 가능
- TTL

## 요청 처리 흐름 

### 콘텐츠 배치 방식 (pull)
- pull CDN
- 대부분은 pull 방식으로 동작
- 원본에 콘텐츠를 두고, 각 엣지는 요청을 받았을 때 원본 서버로부터 리소스를 가져와서 캐시
- 첫 요청자는 느리고, 이후 요청자는 빠름

### 라우팅 방식 (사용자를 어떤 엣지로 보낼것인지)
- 기존에는 Akamai, CloudFront 는 DNS 기반 라우팅을, CloudFlare 는 Anycast 기반 라우팅을 선호했지만 현재는 두 방식을 혼합하는 경우가 많음

#### DNS 기반 라우팅 (unicast 방식)
- 해당 CDN 도메인을 IP로 변환하는 DNS 질의 시점에 대상 엣지 로케이션를 결졍하여 반환
- 각 엣지 로케이션는 서로 다른 IP를 가지며, CDN의 권한 DNS 서버가 요청자의 위치를 기반으로 가까운 엣지 로케이션의 IP 를 반환
- 단, 8.8.8.8 등 퍼블릭 리졸버 사용하는 경우 실제 DNS 질의를 수행하는 재귀 리볼버의 위치가 사용자의 위치와 다를 수 있으므로 지리적 특성으로 판단시 오판 가능성 있음 (`resolver mis-localization`)

#### Anycast 라우팅
- 사용자가 `example.com` 조회시 사용자 측 DNS 는 CDN의 anycast ip 를 응답
- 사용자는 cdn anycast ip 로 패킷 전송
- 사용자의 측 라우터는 자기 BGP 라우팅 테이블을 조회해서 해당 anycast ip (prefix, 주소블럭)에 대해 최적의 경로를 선택
- 적은 홉수, 적은 비용 등의 조건에 의해 특정 엣지 로케이션에 요청이 도착한 경우 해당 CDN 에 리소스가 있는 경우 히트, 없는 경우 원본 fetch
  - 무조건 빠른, 적은 홉수를 가진 경로가 아니고 정책상 선호되는 경로를 선택
- dns 기반 라우팅과 달리 패킷 단위로 라우팅


```
Anycast
- 여러 서버가 같은 IP 주소 또는 IP 주소 집합을 공유
  - 여러 개 서버가 동일 IP를 공유
  - "인터넷에 직접 연결된 장비는 고유한 IP주소를 가진다" 라는 전제 X
- 인터넷 라우터는 실제 목적지는 알지 못하고 목적지가 `104.16.1.1/13` 는 내 이웃 라우터 중 A에게 넘겨라 만 알고 있음
  - 특정 대역의 목적지 IP를 어디로 넘길지 규칙은 BGP 프로토콜을 통해 수립

BGP (Border Gateway Protocol)
- 서로 다른 네트워크 사업자(AS) 사이에서 특정 IP주소 대역으로 가려면 어디로 가야하는지에 대한 정보를 서로 교환하는 프로토콜
- AS(Autonomous System): 하나의 정책으로 운영되는 네트워크 단위 (AWS, CloudFlare, KT...) 
- 각 네트워크 사업자는 나는 이 IP 대역을 가지고 있다고 이웃에게 알리고 (=광고) 그 정보가 인터넷 전체로 퍼져나가면서 라우터의 규칙표가 만들어짐
- https://www.cloudflare.com/learning/security/glossary/what-is-bgp/
```


## CDN 서비스
- AWS CloudFront
- Cloudflare
- Azure FrontDoor
- Google Cloud CDN
- Google Media CDN
- Akamai
- Fastly
- NHN Cloud CDN, Naver Cloud CDN+ ...
- ...

## reference
- https://docs.aws.amazon.com/AmazonCloudFront/latest/DeveloperGuide/HowCloudFrontWorks.html
- https://www.cloudflare.com/learning/security/glossary/what-is-bgp/
- https://developers.cloudflare.com/reference-architecture/architectures/cdn/?_gl=1*18la0g5*_gcl_au*NTU5MTQyNDcwLjE3ODY5NjY5MDA.*_ga*ODNkOTJkNjAtOGVkNi00OWI2LWE1ZDktMzY1ZGZkNmJkOTdm*_ga_SQCRB0TXZW*czE3ODY5NjY5MDckbzEkZzAkdDE3ODY5NjY5MDckajYwJGwwJGgwJGQ1V1J2aTVBRWFUZFV3VG16OXEzN25FWEVreUNWZlF4Nzhn
