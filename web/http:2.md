# http/2

## http/2?

- 2015년 발표된 HTTP/1.1 의 후속
    - 2009년 발표된 구글의 SPDY 를 초안으로 한다.
- http/2 는 binary framing, 헤더 압축, 멀티플렉싱 등의 개념을 통해 http/1.1 개선

## http/2 개선점

### Binary framing

- http/1.1 은 http 메세지가 텍스트였지만 http/2 의 경우 binary 로 인코딩된다.
- 헤더/바디 구분시 http/1.1 의 경우 "\r\n" 문자를 찾아서 구분했지만, http/2 는 명시적으로 레이어가 나뉘어 있으므로 파싱 오류 방지
    - http/2 는 header frame, data frame 으로 명시적 구분

### 헤더 압축 (HPACK)

- HTTP/1.1 은 헤더를 평문 전송 (헤더 압축 기능 자체가 없음). 본문 압축만 가능
    - 본문 압축은 http/1.1·http/2 동일: Content-Encoding/Accept-Encoding: gzip
    - Content-Encoding: gzip
    - Accept-Encoding: gzip
- HTTP/2 는 CRIME 에 안전하도록 설계된 HPACK 으로 헤더 압축
    - 중복되는 헤더의 값을 추출하고, 중복된 헤더는 index 값만을 포함하는 등의 HPACK 압축 방식 통해 데이터 전송 효율 향상
    - 정적 헤더 테이블: 자주 쓰는 60+개 헤더 사전 정의
    - 동적 헤더 테이블: 양쪽에서 헤더값을 기억하고 이후 요청부터 인덱스로 교체

### Multiplexing

```
- frame: http/2 최소 통신 단위, header or data
- message: 요청/응답의 단위. 다수의 frame 배열
- stream: 한 개 connection 내 message 송수신 흐름
```

- tcp 는 순서를 보장하는 바이트 스트림이므로 아래처럼 요청시 수신측에서 이 바이트가 어떤 요청에 대한 것인지 알 수 없음

```
TCP 스트림 (1.1):
...GET /api/users HTTP/1.1\r\nHost:...\r\n\r\nGET /api/posts...
```

- 때문에 http/1.1 은 한 개 요청이 완전히 끝난 후 다음 요청을 보낼 수 있었음
- http/2 는 모든 데이터를 frame으로 쪼개고 각 frame 에는 StreamID 가 할당
- StreamID 를 기준으로 수신측에서 재조립

```
HTTP/1.1:
conn1: style.css ──────►
conn2: app.js ──────────►
conn3: logo.png ─────────────►
conn4: banner.jpg ───────────────────►
conn5: thumbnail_1.jpg ──────────────────────►
conn6: thumbnail_2.jpg ──────────────────────►
       thumbnail_3.jpg ⏳

HTTP/2:
conn1: style.css ──►
       app.js ──────►  전부 동시에
       logo.png ────►  우선순위 높은 것부터
       banner.jpg ──►  프레임 단위로 흘러옴
       thumbnail_1 ►
       thumbnail_2 ►
       thumbnail_3 ►
```

### HOL blocking (Head-of-line blocking)

- HOL blocking: FIFO 상태에서 앞 작업으로 인해 뒤에 처리 가능한 항목이 막히게 되어 대기하게 되는 현상
- 애플리케이션 계층 HOL blocking
    - http/1.1 은 한 커넥션에서 한 번에 한 요청만 처리. 파이프라이닝으로 여러 요청을 연속 전송해도 응답은 요청 순서대로 와야 함
    - → 앞 응답(head)이 느리면 뒤의 준비된 응답도 그 뒤에서 대기 = 애플리케이션 계층 HOL
    - http/2 는 multiplexing 으로 frame 단위로 섞어 보내, 먼저 끝난 스트림부터 응답 가능 → 해결

```
요청:  R1 ──► R2 ──► R3        (한 커넥션에 연속 전송)
처리:  R1 = 무거운 쿼리(느림), R2·R3 = 즉시 응답 가능
응답:  [ R1 완료 대기... ] → 그 다음 R2, R3
        └ R1 이 head. 준비된 R2·R3 를 막음 = 애플리케이션 계층 HOL
```

- 전송(TCP) 계층 HOL blocking
    - TCP 특성상 한 커넥션 내 패킷 유실(byte gap) 시 그 뒤 bytes 를 앱에 올리지 못 함
    - 한 커넥션에 n개 스트림을 다중화하는 HTTP/2 는, 한 번의 유실로 gap 뒤 스트림들이 전부 정지 (재전송까지 대기)
    - 반면 HTTP/1.1 은 커넥션당 단일 요청(+커넥션 6개로 손실 격리)이라 이 피해가 최소화됨

## 일반적인 rest api 웹서버에서 HTTP/2 지원이 필요할지?

- api 병렬호출시 multiplexing
    - FE 에서 api 병렬호출시 http/1.1 의 경우 origin 당 6개까지만 동시 요청하고 나머지는 큐잉. (http/2 는 단일 커넥션 다중 스트림 가능)
- 헤더 압축, Binary framing 자체의 데이터 전송 효율 향상 가능
- 브라우저 <-> 서버 간 http/2 통신시 TLS 는 필수임
- 거의 대부분의 api 서버는 앞단에 리버스 프록시를 두고 tls termination 방식을 사용함.
- 즉, 리버스 프록시에서 http/2 를 지원하고 리버스 프록시 <-> api 서버간에는 http/1.1 통신을 하는게 일반적
    - 리버스 프록시 <-> api 서버는 저지연 내부망이고, keep-alive/connection pool 기반 재사용되므로 http/1.1 로 대응
    - 지원하지 않는 것이 아님. nginx, haproxy, aws alb 등 http/2 를 지원하지만 default 는 http/1.1

==> browser -- HTTP/2 --> reverse proxy -- HTTP/1.1 --> WAS
==> 대부분의 리버스 프록시에서는 코드 한 줄로 http/2 설정이 가능함. 손해보단 이득이 더 크므로 웬만하면 설정하는게 좋을 듯 함
==> 단, 전송(TCP) 계층 HOL blocking 가 우려되는 네트워크 상태가 우려되는 망이라면 HTTP/2 적용 재고

## h2c (HTTP/2 clearText)

- 평문으로 TCP 위에서 동작
- 브라우저는 지원하지 않음 (따라서 브라우저는 HTTP/2 사용시 TLS 강제)
- 주로 내부망에서 서버간 통신시 사용 
  - gRPC 는 HTTP/2(h2) 기반이므로 TLS/mTLS 사용하지 않는 경우 h2c 로 통신
- 연결수립 방식 
  - upgrade(switching protocol) 방식: websocket 과 유사하게 101 switching protocol 통해 연결 수립
  - prior knowledge 방식: 서버가 h2c 를 지원하는 것을 알 수 있는 경우 upgrade 없이 h2c 로 연결 수립 (use Connection Preface)

---

- https://www.cloudflare.com/ko-kr/learning/performance/http2-vs-http1.1/
- https://inpa.tistory.com/entry/WEB-🌐-HTTP-20-통신-기술-이제는-확실히-이해하자