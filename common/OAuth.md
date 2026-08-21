# OAuth

## OAuth(Open Authorization) 가 해결하려는 문제

- OAuth 이전 서드파티 앱(내가 운영중인 서버)이 사용자 자원에 접근하려면 사용자의 인증정보를 직접 받아야 했음
- 사용자가 여러 서드파티 앱에 인증정보를 남겨둔 경우 어느 하나라도 침해되는 경우 인증정보 탈취 가능
- OAuth 는 서드파티 앱에 인증정보를 직접 넘기지 않고, 사용자의 리소스에 접근 가능하게하며 가능한 범위를 scope 로 제한한다.

## 인증과 인가에 대한 구분

- 인증: 당신은 누구입니까
- 인가: 당신은 무엇을 할 수 있습니까
- OAuth 는 인증이 아닌 인가 방식에 대한 표준이다.
    - 인증의 경우 OIDC(OpenID Connect) 가 표준
- 흔히 "OAuth 로 로그인" 이라고 한다면 OAuth 위에 OIDC 를 얹은 구조를 의미한다.

## OAuth 구성요소

- Resource Owner: 자원의 소유자 (사용자 본인)
- Client: Resource Owner 대신 자원에 대한 접근을 요청하는 애플리케이션
- Authorization Server: 사용자를 인증하고 동의를 받은 뒤 토큰을 발급하는 서버 (인가 서버)
- Resource Server: 보호된 자원을 관리하고, 엑세스 토큰을 검증하여 자원을 응답해주는 서버

## OAuth 핵심용어

- client_id: 인가 서버에 사전에 등록된 Client 의 식별자 (공개되어 됨)
- client_secret: Client 의 비밀 자격증명 (공개 x)
- redirect_uri: 인가 서버에서 인증 및 동의 이후 사용자를 되돌려 보낼 사전에 등록된 Client 의 주소
- authorization code: 인가 서버가 브라우저를 통해 Client 로 전달하는 일회성 코드 (토큰이 아님)
- access token: 실제 Resource server 로 api 호출시 사용되는 토큰

## Authorization Code Flow

- OAuth 통해 서드파티 앱이 인증정보를 직접 받지 않고 접근 가능한 권장되는 흐름
- 사전에 Client 개발자는 해당하는 인가 서버 측에 client 애플리케이션 등록 및 client_id, secret 획득
    - google cloud console... 등에서 client 등록
    - client_id, secret 획득
    - redirect_uri 등록
- resource owner(사용자)의 리소스 접근 필요시 client 는 사용자를 인가 서버 엔드포인트로 리다이렉트
    - 사용자는 리다이렉트 된 곳에서 인가 서버 통해 인증

```
GET /authorize
  ?response_type=code # Authorization Code Flow 를 사용하겠다는 선언 (response_type=token: Implicit flow)
  &client_id=s6BhdRkqt3
  &redirect_uri=https%3A%2F%2Fclient.example.com%2Fcb
  &scope=photos.read
  &state=xyzABC123 # csrf 방지용 난수
  &code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM # PKCE 공개값 (client의 code_verifier 해시)
  &code_challenge_method=S256
Host: server.example.com
```

- 인증 및 동의 완료시 redirect_uri 로 redirect
    - 응답에는 authorization code 를 포함
- authorization code 를 accessToken 으로 교환 (client -> 인가서버)
- client 는 획득한 accessToken 통해 Resource Server 로 리소스 요청

```
authorization code 로 accessToken 을 획득하는 이유
- 인가서버로부터 응답을 받는 주체는 사용자(브라우저)
- 만약 authorization code 대신 accessToken 가 응답되는 경우 accessToken 브라우저 주소창을 타고 여러 히스토리에 저장되고 탈취 위험이 생김
- authorization code 가 노출되면?
  - 결과적으로 리소스 서버는 accessToken 을 요구함. authorization code 만으로는 리소스 서버로부터 인증 불가

authorization code flow 이전 표준이었떤 Implicit flow 가 authorization code 가 아닌 accessToken 을 노출하는 방식을 사용
```

## PKCE (ProofKey for Code Exchange)

- https://www.rfc-editor.org/info/rfc7636/
- PKCE는 OAuth2.0 확장 기능으로 인가 요청과 accessToken 통한 리소스 접근이 같은 클라이언트로부터 나왔음을 증명
- 별도 백엔드 서버에 cleint_secret 을 관리할 수 없는 공개 클라이언트의 경우 authorization code(인가 코드) 를 가로채면 인가 코드를 그대로 accessToken으로 바꿀 수 있음
- 즉, 공개 클라이언트 환경 또는 client_secret 이 탈취(극단적인 예시)된 경우 인가 코드 탈취만으로 사용자 리소스 접근이 가능해짐
- PKCE 는 인가 코드 가로채기 공격을 막는다.

PKCE 용어

- code_verifier: 클라이언트 메모리에만 있는 난수
    - 클라이언트 측 메모리에만 있는 값이므로 노출 불가
- code_challenge: code_verifier 의 해시값
    - 인가 요청 url 에 노출되며, 노출되어도 무방
- code_challenge_method: code_challenge 해시 방식 (SHA-256 권장)
    - 인가 요청 url 에 노출되며, 노출되어도 무방

```
code_challenge = base64url(SHA256(code_verifier))
```

### 동작 방식

1. client 측에서 code_verifier, code_challenge 생성
- code_verifier 는 인가 요청마다 새로 생성
- 생성된 code_verifier 는 client 메모리에 저장
- 생성된 code_verifier 해시 및 base64url 인코딩하여 code_challenge 생성
1. 인가 요청
- 인가 서버로 인가 요청시 code_challenge, code_challenge_method 만 포함
- code_verifier 는 요청에 포함하지 않는다.
1. 인가 서버 측 authorization code 생성 및 code challenge 값 저장
- 인가 서버는 authorization code 를 생성하고 응답
- 단, 생성한 코드와 매핑하여 code_challenge, code_challenge_method 값을 저장
1. client 측에서 accessToken 요청
- 요청에는 authorization code 와 code_verifier 값을 포함
1. 인가 서버 측 code, verifier 검증 및 accessToken 발급
- authorization code 에 매핑된 code_challenge, code_challenge_method 통해 code_verifier 해시 및 accessToken 발급 요청 내 code_verifier 와 값 비교
- 일칳는 경우 accessToken 발급

## OIDC (OpenID Connect)

- OIDC 1.0 은 OAuth 2.0 위에 올라가는 인증 (Authentication) 계층
    - OAuth2.0 기반 인증기능과 End-User에 대한 정보 전달을 위한 Claim 사용을 정의
- OAuth2.0은 서드파이 앱이 accessToken 을 통해 리소스에 접근할 권한이 있는지를 검증하기 위한 표준
- client는 인가 요청시 scope 에 `openid` 를 포함시켜 확장
- 응답으로 JWT 포맷의 ID token을 응답

### OIDC 스펙이 정의하는 것

1. ID Token : 인증 이벤트에 대한 클레임을 담은, JWS 서명된 JWT
2. UserInfo EP : Access Token으로 조회하는 표준 사용자 정보 엔드포인트
3. 표준 Claim : sub/name/email/picture 등 (Core §5.1)
4. openid scope : 확장을 요청 scope
5. nonce : ID Token replay 방어

### 관련 용어

- OpenId Provider(OP): OpenID Connect를 구현한 OAuth 2.0 인증서버
- Relying Party(RP): OAuth 2.0 클라이언트
- End-User: 실제 인증되는 사용자 (=resource owner)
- ID Token: 인증 결과 claim 을 포함하는 JWT
- Token Endpoint: Authorization Code 를 ID Token, AccessToken/RefreshToken 으로 교환
- UserInfo Endpoint: accessToken 통해 리소스 서버로부터 end-user 의 정보를 응답
- jwks_uri: Id Token 서명 검증용 공개키 배포 (jwks_uri)

### 인증 플로우

#### AuthorizationCodeFlow (response_type=code)

- client가 브라우저를 Authorization endpoint 로 리다이렉트
- 인증서버(OP) 가 End-User 인증 및 동의 획득
- redirect-uri 로 code 반환
- client 가 Token Endpoint 로 code, 클라이언트 인증정보 전송
- OP 가 ID Token, AccessToken(RefreshToken) 응답

```kotlin
TokenEndpoint 요청시

//client_secret_basic
//헤더방식 권장 (https://www.rfc-editor.org/info/rfc6749/#section-2.3.1)
//Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW
//basic => base64(client_id:client_secret)
POST /token HTTP/1.1
Host: server.example.com
Content-Type: application/x-www-form-urlencoded
Authorization: Basic czZCaGRSa3F0MzpnWDFmQmF0M2JW

grant_type=authorization_code
&code=SplxlOBeZQQYbYS6WxSbIA
&redirect_uri=https%3A%2F%2Fclient.example.org%2Fcb

//client_secret_post
//client_secret 본문에 넣는것 허용
grant_type=authorization_code&code=...&redirect_uri=...
&client_id=s6BhdRkqt3&client_secret=7Fjfp0ZBr1KtDRbnfVdmIw
```

### 서명 검증

- 인가 서버는 ID Token 을 자신이 개인키로 서명
- RP(Client)는 OP의 공개키로 서명 검증
    - 공개키는 인가서버가 공개하는 `jwks_uri` 로부터 획득
- code 통해 ID Token 획득 요청시 TLS (HTTPS) 기반 요청인 경우 이미 인가서버가 준 응답임을 보장함으로 굳이 공개키로 다시 검증할 필요 없음
- https://openid.net/specs/openid-connect-core-1_0.html#IDTokenValidation

### ODIC 장점

- 사용자 정보 조회 응답 표준화
    - OAuth 2.0 통해 accessToken 획득 이후 userInfo endpoint로 사용자 정보를 얻어오는 경우 해당 스펙이 다름 (요청 url, 응답 스키마 모두 다름)
    - OIDC 는 OIDC 의 클레임을 표준으로 정의하므로 표준화 가능
    - userinfo endpoint 를 통해 표준화
- 검증 가능
    - aud, nonce, exp, 서명 등을 통해 자체 검증 가능
- accessToken 발행시 id token 을 동봉하는 경우 api 호출횟수 감소
    - id token 동봉 여부는 OP 재량
    - id token 동봉되는 경우 userinfo endpoint 호출 또는 oidc가 아닌 경우 user 정보 조회 api 호출 1회 감소 가능

### ODIC 표준 클레임

- 필수: `iss`, `sub`, `aud`, `exp`, **`iat`**

```kotlin
{
  "iss": "https://accounts.google.com",   
  "sub": "1234567890",                    
  "aud": "my-client-id",                  
  "exp": 1311281970,                      
  "iat": 1311280970,
  
  "email": "user@example.com",            
  "email_verified": true,                 
  "name": "Jane Doe",                     
  "picture": "https://..."                
}
```

## reference

- https://datatracker.ietf.org/doc/html/rfc6749
- https://guide.ncloud-docs.com/docs/b2bpls-oauth2
- https://www.sktenterprise.com/bizInsight/blogDetail/dev/11146