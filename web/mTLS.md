# mTLS (Mutual TLS, 상호 TLS 인증)

- 일반적인 TLS 는 서버만 자신의 인증서를 클라이언트에게 제시해서 신원을 증명
- 클라이언트는 서버가 누구인지 알고 신뢰할 수 있지만, 서버는 클라이언트가 누구인지 확인하지 않음.
- mTLS 는 위 TLS 과정을 양방향으로 확장
- 클라이언트도 자신의 인증서를 서버에 제시하여 서버가 클라이언트의 신원을 검증

## TLS (Transport Layer Security)

- 브라우저, 서버 간 통신 데이터 암호화 위함
- SSL(Secure Sockets Layer) 개선 프로토콜
- HTTPS 는 HTTP 상에서 TLS 암호화를 구현한 것.

```
TLS 주요 기능

암호화: 제3자로부터 전송되는 데이터를 암호화
인증: 정보를 교환하는 당사자가 요청된 당사자임을 보장
무결성: 데이터 위변조 여부 확인
```

### TLS handshake

- https://www.cloudflare.com/ko-kr/learning/ssl/what-happens-in-a-tls-handshake/
- 클라이언트는 서버의 인증서를 받아 서버의 신원 검증 및 무결성을 확인
    - Certificate 메세지에 인증서 포함 (이 안에는 서버의 공개키 포함)
    - 브라우저의 CA 공개키 통해 인증서 무결성 검증
- 암호화 통신에 사용할 세션키(대칭키) 를 서버와 클라이언트가 나눠가지는 과정
    - 세션키는 RSA(TLS 1.2), ECDHE(TLS 1.3) 키 교환 방식 사용
    - 단, 인증서 서명의 경우 TLS 1.3 도 RSA 사용 (RSA, ECDSA, EdDSA 선택)

```
TLS 1.2 (2RTT)
1. client -> server  SYN
2. server -> client  SYN, ACK
3. client -> server  ACK                              (TCP 연결 완료)

4. client -> server  ClientHello
5. server -> client  ServerHello, Certificate,
                      ServerKeyExchange,                (ECDHE 파라미터 + 서명, RSA 방식이면 생략)
                      ServerHelloDone
6. client -> server  ClientKeyExchange,
                      ChangeCipherSpec, Finished
7. server -> client  ChangeCipherSpec, Finished

TLS 1.3 (1-RTT)
1. client -> server  SYN
2. server -> client  SYN, ACK
3. client -> server  ACK                              (TCP 연결 완료)

4. client -> server  ClientHello,
                      KeyShare                          (ECDHE 공개값 미리 포함)
5. server -> client  ServerHello,
                      KeyShare,                         (ECDHE 공개값)
                      {Certificate, CertificateVerify, Finished}  (5번부터 암호화 시작)
6. client -> server  Finished
```

- ClientHello
    - 지원 가능 TLS 버전 목록
    - ClientRandom
    - 지원 가능 암호 스위트(cipher suite)
    - SNI (접속하려는 도메인명)
- ServerHello
    - 선택된 TLS 버전
    - ServerRandom
    - 선택된 암호 스위트
- Certificate
    - 서버가 자신의 신원 증명을 위해 인증서, 서버 공개키 전송
- ClientKeyExchange
    - 대칭키 생성에 필요한 값(pre-master secret)을 서버의 공개키로 암호화해서 전송 (RSA)
    - ECDHE 의 경우 임시 타원곡선 키쌍을 만들고 자신의 공개값만 교환
- ChangeCipherSpec
    - 지금부터 암호화된 메세지를 보내겠다는 신호
    - 해당 메세지 이후 메세지부터 암호화 적용

## mTLS handshake

- 일반 TLS 와 비교
    - 5단계: CertificateRequest 추가
        - 서버가 클라이언트에게 인증서 제출 요구
    - 6단계: Certificate, CertificateVerify
        - Certificate: 클라이언트가 자신의 인증서를 제출
        - CertificateVerify: 클라이언트 측 개인키로 서명한 값 전송 (서버측에서 공개키로 인증)

```
1. client -> server  SYN
2. server -> client  SYN, ACK
3. client -> server  ACK                              (TCP 연결 완료)
4. client -> server  ClientHello                      (TLS 핸드셰이크 시작)
5. server -> client  ServerHello, Certificate,
                      CertificateRequest               (클라이언트 인증서 요구)
6. client -> server  Certificate,                      (클라이언트 인증서 제출)
                      ClientKeyExchange,
                      CertificateVerify,                (개인키로 서명, 본인 증명)
                      ChangeCipherSpec, Finished
7. server -> client  ChangeCipherSpec, Finished
```

## why mTLS

- mTLS 는 주로 서버간 통신시 상호간 신원확인 목적으로 사용됨
- 방화벽/VPC/사설망 등 보통 서버는 외부와 격리되어 있으므로 신뢰할 수 있었고, 외부 LB 등에서 TLS termination 처리 이후 내부에서는 HTTP 통신했었음.
    - TLS 의 인증서 관리, TLS 위한 추가 네트워크 비용 등을 굳이 치룰 필요가 없다는 판단.
    - 내부망은 안전하다 라는 전제 위에서 비용을 고려한 합리적인 판단
- 내부망도 안전하지 않다.
    - Zero-trust 내부망에 있든 아니든, 신원을 증명해라.
    - 어떤 방식으로라도 내부망에 접근하여 감청 가능하다는 가정으로 내부망 통신에도 평문 통신을 허용하지 않는다.

## mTLS 사용처

- 클라이언트가 브라우저인 경우는 일반적으로는 TLS 를 사용 (mTLS X)
    - 인증서 및 개인키 관리 어려움
- 주로 server to server 환경에서 사용
    - k8s service mesh (Istio..), gRPC
- k8s 의 경우 사이드카 방식으로 리버스 프록시를 올려서 mTLS 를 리버스 프록시 수준에서 처리
- gRPC 의 경우 라이브러리 차원에서 mTLS 를 기본 지원하므로 인증서 경로만 지정하면 됨.

## mTLS 인증서

- 일반 TLS 의 경우 공인 CA 가 발급한 인증서가 사용됨 (Let's Encrypt, DigiCert..)
    - 공인 CA 의 경우 브라우저, OS 에 루트 인증서로 내장되어 있기 때문에 클라이언트 측 별도 설정없이 인증서 처리 가능
- mTLS 는 보통 사설CA/내부 PKI 통해 인증서를 발급
    - 사설CA: 외부 공인 기관없이 조직 낸부에서 인증서를 직접 서명, 발급. 사설CA가 서명한 인증서는 해당 CA 를 신뢰하는 시스템 간에만 유효
    - 내부PKI (Public Key Infrastructure)
        - 사설 CA 를 포함, 인증서 생명주기 전체를 관리
        - Vault, step-ca, OpenSSL...
        - 내부 PKI 통해 CA 를 생성
- mTLS 의 클라이언트(서버간 통신에서 다른 서버) 특성상 수시로 생성되고 사라지고, 공인 CA의 도메인 검증 개념이 서버간 신원에는 맞지 않기 때문에 별도 사설 CA 통해 사용
    - 인증서 내에 도메인 대신 내부 식별자(서비스 식별자)를 포함하는 등.