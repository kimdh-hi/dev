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

## reference

- https://datatracker.ietf.org/doc/html/rfc6749
- https://guide.ncloud-docs.com/docs/b2bpls-oauth2
- https://www.sktenterprise.com/bizInsight/blogDetail/dev/11146