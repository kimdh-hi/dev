# JOSE

## JOSE (Javascript Object Signing and Encryption)

- JSON 기반 데이터 서명, 암호화 방법에 대한 표준 묶음
- JOSE 이전 XML 기반 표준의 복잡도 및 취약점 개선 위해 2015/05 JOSE 등장
- JOSE 핵심 표준 문서
    - JWS, JWE, JWK, JWT, JWA

## JWS (Json Web Signature)

- 데이터에 대한 서명을 붙여 위변조 감지 위함
    - 페이로드에 대한 암호화와 관계없음
- `base64url(헤더).base64url(페이로드).base64url(서명)` 구조
    - 서명 입력: 헤더 + "." + 페이로드
- 헤더에 명시한 alg 서명 알고리즘을 사용하여 서명입력을 개인키로 서명
    - 서명 = sign(서명 입력, 개인키)
- 공개키 기반 서명인 경우 JWS 를 받은 수신자 측은 공개키를 통해 서명입력 위변조 여부를 검증 가능
- HMAC(HS)와 같은 서명 알고리즘 사용한 경우 대칭키로 서명하고 수신측에서 대칭키로 서명 검증

## JWT (Json Web Token)

- 페이로드에 대한 규칙을 정한 JWS
- 기본적인 구조는 JWS 와 완전히 동일
    - `base64url(헤더).base64url(페이로드).base64url(서명)` 구조
- 단, 페이로드 영역에 대한 규칙이 추가된다.
    - 페이로드는 Json 이어야 한다.
    - 페이로드 Json 의 각 항목을 claim 이라 명칭한다.
    - 자주 쓰는 Claim 의 이름을 표준으로 정해둔다.
- 표준 Claim
    - iss: 누가 발급했나
    - sub: 누구에 대한 토큰인가
    - aud: 누가 받아야 하는가
    - exp: 만료기한
    - nbf: 언제부터 유효한가
    - iat: 언제 발급되었는가
    - jti: jwtId 토큰 고유번호
- 페이로드 내용을 제외하고 JWS 와 완전히 동일하므로 수신측에서 서명 검증은 동일하게 수행
- 서명 검증 이후 claim 에 따른 부가적인 검증을 통해 유효 토큰 여부를 판단한다.

## JWK

### JWK (Json Web Key)

- JWK는 암호키를 Json 으로 표현하는 포맷
    - 공개키, 개인키, 대칭키 전부 표현 가능
    - 기존 공개키는 PEM 파일이나 X.509 인증서 통해 주고 받았음
- 주로 인증 서버가 공개키를 JWKS 엔트포인트에 JSON 으로 노출하고, 각 클라이언트 서버가 공개키를 받아서 토큰 서명 검증하는데 사용
- 인증서버는 공개키를 api 통해 공개하고, 개인키는 kms 통해 관리
    - msa 구조에서 인증서버는 개인키로 서명한 jwt 를 발행하고 공개키는 api 로 공개 노출한다.
    - 다른 마이크로서비스에서 jwt 서명 검증 필요한 경우 인증서버 jwk 엔드포인트로부터 공개키 받아 서명 검증한다.

### JWK 형태

- Ed25519(EdDSA) 샘플
- `kty`: key type (RSA, EC, OKP...)
- `use`: 공개키 용도 (`sig`, `enc` ..)
- `x`: 공개키
- `d`: 개인키
    - kty

```
# 개인 JWK
{
  "kty": "OKP",
  "crv": "Ed25519",
  "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo",
  "d": "nWGxne_9WmC6hEr0kuwsxERJxWl7MmkZcDusAxyuf2A",
  "use": "sig",
  "alg": "Ed25519",
  "kid": "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k"
}

# 공개 JWK
{
  "kty": "OKP",
  "crv": "Ed25519",
  "x": "11qYAYKxCrfVS_7TyWQHOg7hcvPapiMlrwIaaPcHURo",
  "use": "sig",
  "alg": "Ed25519",
  "kid": "kPrK_qmxVWaYVA9wwBF6Iuo3vVzz7TxHCTwXBygrS4k"
}
```

### JWKS (JWK Set)

- `{"keys":[JWK, JWK, ...]}` 형태
- 서로 다른 kty/alg 의 키가 섞일 수 있음
- 공개 엔드포인트로 노출
- JWK 내에 kid 통해 JWKS 내에서 JWK 선택
- 무중단 키 로테이션 위함

```
키 로테이션
1. 새 키 JWKS 에 추가 (이전 키 유지)
2. 새 키로 서명 시작
3. 이전 키, 새 키 각각 kid 가 다르므로 kid 에 해당하는 JWK 기반 서명 및 서명 검증 가능
4. 이전 키가 만료될만한 충분한 시간 이후 이전 키 제거
```

## JWE (JSON Web Encryption)

- 데이터를 암호화해서 전달하기 위한 표준
- 수신자의 공개키로 암호화하고, 수신자는 개인키로 복호화
- `헤더.암호화 된 CEK.IV.암호문.인증태그` 5개 섹션(점4개) 로 구성
- 하이브리드 암호화 기법 사용
    - RSA(비대칭키 암호화) 는 AES(대칭키 암호화)보다 수백배 이상 느리고 크기 제한도 있음
    - `데이터는 AES로 암호화`하고, `AES 암호화 키를 RSA 로 암호화`해서 함께 보낸다.
    - 수신 측은 개인키를 통해 AES 대칭키를 얻고 대칭키 통해 데이터 복호화 수행
    - AES 키는 32byte로 RSA 200바이트 제한에 걸리지 않고 짧으므로 느린 RSA 암호화에도 대응 가능
    - 실제 데이터를 AES 대칭키 암호화 한 CEK 값을 RSA 비대칭 암호화 한 값(JWE Encrypted Key)을  포함해서 수신측에 전달
- 하이브리드 암호화 사용하므로 헤더에는 `alg` , `enc` 각각 총 두 개 알고리즘이 명시
    - alg: CEK 자체를 암호화 한 알고리즘
    - enc: CEK 통해 실제 데이터를 암호화 한 알고리즘
- IV (Initialization Vector)
    - AES 방식으로 데이터 암호화시 같은 데이터를 같은 키로 암호화하면 매번 같은 암호문이 나온다는 이슈 있음
    - 키가 없다면 복호화는 불가하지만 같은 메세지가 나간다는 것. 즉 패턴을 알 수 있으므로 매번 다른 암호문이 나오도록 임의 난수값 추가
    - `AES(데이터, CEK, IV)`
    - 복호화 시 IV 필요하므로 수신측에 전달 필요
- Authentication Tag
    - 공격자는 내용 복호화는 불가해도 암호문의 비트를 뒤집어 복호화 결과를 다르게 조작 가능
    - `암호화 + 무결성 보장을 위해 Authentication Tag 라는 검증값` 추가
    - 헤더와 암호문 기반 CEK 로 계산한 MAC 값을 Tag 로 하여 포함
        - 수신측에서는 CEK 복호화 이후 Tag 일치여부 확인을 위해 MAC 계산
        - 검증 실패시 이후 복호화 작업 진행하지 않고 복호화 거부

```json
JWS vs JWE 키 방향
- JWS: 발신자의 개인키로 서명 → 발신자의 공개키로 검증
- JWE: 수신자의 공개키로 암호화 → 수신자의 개인키로 복호화
```

## reference

- https://jose.readthedocs.io/en/latest/
- https://datatracker.ietf.org/doc/html/rfc7517
- https://www.rfc-editor.org/rfc/rfc7516
- claude