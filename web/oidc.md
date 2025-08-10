## OIDC (OpenId Connect)

### OAuth2, OIDC
- OAuth2.0 프로토콜은 인증된 사용자에게 특정 리소스에 대한 권한을 인가를 목적으로 한다.
- OIDC 는 OAuth2.0 프로토콜을 확장하여 사용자의 신원 확인(인증)을 보다 효율적으로 한다.

### OIDC
- 기본적인 flow 는 OAuth2 의 Authorization Code Grant 기준 거의 동일하다.
- 차이점으로는 Authorization Code 통해 AccessToken 요청시 응답으로 AccessToken 과 IdToken 이 함께 응답된다.
- IdToken 은 JSON 포맷이며 OIDC 표준에 의해 정의된다.
  - https://openid.net/specs/openid-connect-core-1_0.html#IDToken


### IdToken 예시
```
{
  "iss": "https://auth.service.com",
  "sub": "sub123123213",
  "aud": "https://myapp.com",
  "exp": 1516670124,
  "iat": 1516670124,            // 토큰 발급 시간
  "auth_time": 1516660124,      // 사용자 인증 시간
  "nonce": "abcdef",            
  "at_hash": "gusdni123",       // AccessToken hash, accessToken 검증시 사용
  "email": "test@example.com",
  "name": "name",         
  "picture": "https://service.com/profile.jpg"
}
```

### OIDC 장점

#### 사용자 정보에 접근이 목적인 경우 OIDC 사용시 요청을 절반으로 줄일 수 있다. 

OIDC 이전 OAuth2.0 flow 를 통해 100명의 사용자 정보라는 리소스에 접근하려면 해당 Resource Server 가 제공하는 endpoint 를 총 200회 호출 필요 
1. accessToken 획득
2. endpoint (/userinfo) 호출


#### 표준화된 IdToken 
- OIDC 스펙에서 IdToken claim에 대한 표준을 정의한다.
- 기존 resource server 가 제공하는 endpoint 통해 사용자 정보 조회시 응답이 표준화되지 않은 경우 추가 작업이 있을 수 있다.


---

reference
- https://openid.net/specs/openid-connect-core-1_0.html#IDToken
- https://www.samsungsds.com/kr/insights/oidc.html
- https://blog.logto.io/ko/what-is-oidc