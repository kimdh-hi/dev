# subdomain

## subdomain?
- api.test.com
  - api: 서브도메인
  - aaa: second-level domain
  - com: top-level domain
- 내가 소유한 도메인 아래 내가 직접 정의한 계층을 의미
- DNS 표준에서 서브도메인의 수는 제한하지 않음.
  - 한 개 레이블의 길이 제한 (aaa.bbb.com ==> 점으로 구분해서 총 3개 레이블)
  - 전체 도메인 이름 길이 제한
  - 도메인 판매 업체마다 DNS 레코드 수 제한은 있을 수 있음.
  - 흔히 서브 도메인은 아래로 한 개 계층만 사용 가능하다는 것은 TLS 인증서 와일드카드의 특징 때문이고 얼마든지 여러 계층을 . 으로 구분해서 사용 가능
- 서브도메인 생성 방식
  - 서브도메인은 DNS zone file 에 추가되는 레코드이다.
  - A레코드: 서브도메인을 특정 IP (IPv4) 로 직접 연결 
    - AAAA: IPv6 로 직접 연결
  - CNAME레코드: 서브도메인을 다른 도메인 이름의 별칭으로 연결
  - NS레코드: 서브도메인 관리 권한 자체를 다른 네임서버에 위임 
  - 즉, `api.aaa.com` 은 `aaa.com` DNS zone 에 `api` 라는 이름의 레코드를 추가해서 특정 서버가 가리키가 하는 것.

```
aaa.com.       IN A       203.0.113.10
www.aaa.com.   IN CNAME   aaa.com.
api.aaa.com.   IN A       203.0.113.20
```

## subdomain 사용시 주의

### CORS
- 클라이언트(FE) 는 ui.aaa.com 에 위치하고, 서버(BE) 는 api.aaa.com 에 위치한 경우
- 출처(protocol, host, port) 가 다르므로 차단
- 서버에서 Access-Control-Allow-Origin 지정 필요

### Cookie
- Cookie Domain 속성
- 쿠키의 Domain 을 서브도메인과 공유 가능하도록 설정 추가 필요
  - 선행점은 무시됨
  - `.aaa.com` == `aaa.com`
- `aaa.com`: `ui.aaa.com` 과 `api.aaa.com` 에서 Cookie 공유

### TLS 와일드카드 적용 범위
- 와일드카드는 한 개 계층에 대해서만 대응
- `*.aaa.com`
  - `api.aaa.com` 대응됨
  - `v1.api.aaa.com` 대응안됨


## reference
- claude
- https://en.wikipedia.org/wiki/Subdomain
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CORS
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/Cookies
- https://docs.aws.amazon.com/ko_kr/Route53/latest/DeveloperGuide/dns-routing-traffic-for-subdomains.html