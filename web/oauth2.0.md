OAuth 2.0 (Open Authorization)

## OAuth 2.0 (Open Authorization 2.0) 등장 배경
- oauth 등장 이전 우리 서비스 사용자의 계정으로 google api(calender, drive...) 사용이 필요한 경우 google 인증정보를 받아서 우리 서비스에 저장하고 필요할 때 google api 호출의 인증정보로 사용
- 문제
  - google 계정 id/pw 를 우리 서비스에 등록하는 사용자는 우리 서비스를 신뢰할 수 있는가?
  - 우리 서비스는 google 계정 정보를 저장하는 책임을 질 수 있는가?
  - google 은 우리 서비스를 신뢰할 수 있는가?
- OAuth 는 third-party 앱이 사용자의 리소스에 접근할 수 있는 절차를 정의한다.


## OAuth 2.0 구성 요소

Resource Owner
- 리소스에 대한 엑세스 권한을 부여하는 사용자
- third-party 애플리케이션 에서 google api 사용시 google 인증을 하는 사용자

Client
- 리소스에 접근하는 third-party 애플리케이션

Authorization Server / Resource Server
- Authorization Server: 사용자의 로그인을 통해 권한을 위임받아 Client 의 인증 요청을 처리하여 Access Token 발급
- Resource Server: 실제 사용자의 Resource 를 갖고 있는 서버

```
(Authorization Code Grant 방식에서 Authorization Server / Resource Server 예시)

1. Client 가 사용자를 Authorization Server 로 redirect
2. 사용자 인증 (id/pw 사용자 로그인 및 scope 등 동의 여부 결정)
3. Authorization Code 발급 (-> Client)
4. Client 는 Authorization Code 로 Access Token 발급
5. Resource Server 로 Access Token 포함하여 리소스 요청
```


## OAuth 2.0 권한 부여 방식

Authorization Code Grant
- Resource Owner 로그인 완료시 Client 로 Resource Server 에서 인증이 가능한 AccessToken 을 직접 발급하지 않고, Authorization Code 를 발급하는 방식
- AccessToken 을 직접 노출시키지 않으므로 보안상 유리
- Authorization Code 통해 AccessToken 요청시 client_id, secret 등을 포함해야 하며, Authorization Code 도 1회성 혹은 짧은 만료기한 갖기 때문에 보안상 유리.

Implicit Grant
- 보안상 취약하므로 권장되지 않음.
- 서버가 없는 경우 권한 부여를 받을 수 있기 위함.
- Client 는 Authorization Server 로 AccessToken 요청시 redirect-uri 를 포함하고, 사용자 인증 완료시 redirect-uri 로 redirect 시 accessToken 을 포함

Client Credentials Grant
- 사용자 개입 없이, Client 의 자격(clientId, secret) 만으로 Access Token 을 획득하는 방식
- Client 가 Resource Owner 가 되는 개념
- msa 간 내부 api 호출시 인증에 사용될 수 있음.
- 참고: https://docs.spring.io/spring-authorization-server/reference/getting-started.html


---

reference
- https://datatracker.ietf.org/doc/html/rfc6749
- https://guide.ncloud-docs.com/docs/b2bpls-oauth2
- https://developers.google.com/identity/protocols/oauth2
