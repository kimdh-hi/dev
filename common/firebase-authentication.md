# Firebase Authentication

## Firebase Authentication

- Identity Broker
    - Keycloak의 identity broerking
    - AWS Cognito UserPool 의 federated Idp 와 같은 범주
- 여러 IDP 를 하나로 묶어주고 인증 결과 idToken 을 firebase idToken 으로 표준화

## Firebase IdToken (표준화된 ID token)

- https://firebase.google.com/docs/auth/admin/verify-id-tokens?hl=ko
- IDP 가 무엇이든 IdToken 은 언제나 firebase 가 표준으로 정의한 firebase ID token 하나
    - IDP 가 google, apple 등등 늘어나도 서버측 검증 로직은 변함 없음
- 즉, IdToken 검증시 firebase admin sdk 만 있으면 됨
- IdToken 자체에 대한 스펙은 OIDC 가 표준으로 규정하지만 IdToken 에 대한 검증은 IDP 마다 상이
    - google: Google JWKS, google_client_id 기반 검증
    - apple: Apple JWKS, apple_service_id 기반 검증
    - ...

```
// iss: <https://securetoken.google.com/><projectId>
{
  "iss": "<https://securetoken.google.com/my-project>",
  "aud": "my-project",
  "sub": "abc123FirebaseUid",
  "email": "user@example.com",
  "email_verified": true,
  "firebase": {
    "identities": {
      "google.com": ["117xxxxxxxxxxxxxxxxxx"],   // 구글의 sub
      "email": ["user@example.com"]
    },
    "sign_in_provider": "google.com"
  }
}
```

- 수명
    - idToken: 1시간
    - refresh token: 무기한
- 서버측에서 idToken 검증시 주의
    - `verifyIdToken()` 은 기본적으로 취소 여부를 확인하지 않음.
    - 서명, 만료여부, aud, iss 만 검증
    - 즉, firebase 관리자가 해당 계정을 정지해도 이미 발급된 idToken은 1시간 동안 검증 통과
    - `checkRevoked` 플래그 설정 필요 (https://firebase.google.com/docs/auth/admin/manage-sessions?hl=ko#detect_id_token_revocation_in_the_sdk)
- firebase idToken email 주의
    - firebase idToken 의 `email` 은 최초 로그인시 google idToken 의 `email` 클레임 값
    - firebase 사용자 레코드에 `email` 값을 복사하여 저장
    - 이후 매 번 firebase idToken 발급시 사용자 레코드의 email을 idToken 의 email 클레임 값으로 사용
    - firebase 사용자 레코드의 `email` 값은 수동으로 변경 가능
    - 즉, firebase idToken 의 `email` 은 실제 email 과 다를 수 ㅣㅇㅆ음