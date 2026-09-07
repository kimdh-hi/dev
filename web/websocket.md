# websocket

## websocket

- websocket 은 TCP 연결 위 client/server 가 양방향으로 데이터를 송수신하는 통신 프로토콜
- websocket 이전 양방향 통신을 위해 http 폴링을 주로 사용
- 양방향 통신시 http 폴링의 오버헤드 해법으로 제안

## polling 시 websocket 대비 비용

- 요청당 헤더 오버헤드
    - 요청마다 cookie, Authorization, User-Agent 등등 헤더값이 중복으로 왕복
    - 단, http/2 의 경우 헤더압축을 통해 오버헤드 최소화됨
- 지연시간 (short polling 의 경우)
    - 폴링 주기가 곧 지연 시간 (1초 주기 폴링이라면 평균 500ms, 최악 1초까지 지연)
- 불필요한 트래픽
    - 폴링시 대상 데이터가 없는 시점에도 폴링 트래픽은 지속됨

## opening handshake

- websocket 연결수립
- websocket 연결은 http 요청으로 시작

```
//client 요청
GET /chat HTTP/1.1
Host: server.example.com
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Key: dGhlIHNhbXBsZSBub25jZQ==
Origin: http://example.com
Sec-WebSocket-Protocol: chat, superchat
Sec-WebSocket-Version: 13

//server 응답
HTTP/1.1 101 Switching Protocols
Upgrade: websocket
Connection: Upgrade
Sec-WebSocket-Accept: s3pPLMBiTxaQ9kYGzzhZRbK+xOo=
Sec-WebSocket-Protocol: chat
```

- Sec-WebSocket-Key
    - 연결시 마다 생성하는 random key
    - 16바이트 랜덤값을 base64 인코딩한 값
- Sec-WebSocket-Accept
    - 인증 등의 보안장치 아님
    - 상대가 websocket 을 이해 가능한 상태인지 확인하는 용도
    - `Sec-WebSocket-Accept = base64( SHA1( Sec-WebSocket-Key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" ) )`
    - `258EAFA5-E914-47DA-95CA-C5AB0DC85B11` 는 websocket 스펙이 정의하는 고정 GUID

## Frame

- opening handshake 통해 websocket 연결 수립 이후부터는 client/server 모두 header/payload 구조의 http 메세지가 아닌 byte frame을 주고 받음
- frame 내에는 http 메서드/경로/상태코드/헤더/쿠키 등이 없음. websocket 스펙이 정의하는 byte 형태의 frame 만
- 하나의 논리적인 메세지는 여러 개 frame 으로 쪼개질 수 있다.
    - 수신측에서 byte framing 통해 조립

```
프레임1: FIN=0, opcode=0x1 (text), "Hel"
프레임2: FIN=0, opcode=0x0 (continuation), "lo "
프레임3: FIN=1, opcode=0x0 (continuation), "World"
```

- 하나의 커넥션에서, 한 방향으로 진행중인 조각난 메세지는 최대 1개로 규정한다.
    - 각 frame 은 어떤 메세지에 대한 조각인지를 나타내는 필드가 없음.
    - 송신측에서 메세지 연속해서 발송시 선행된 메세지에 대한 조각 모두 전송 이후 다음 메세지에 대한 조각화 및 데이터 송신 진행
- websocket 커넥션은 단일 송신 파이프로 각 메세지에 대한 식별자, 우선순위 등이 없으므로 대량의 데이터를 통째로 밀어넣는 경우 해당 커넥션으로 가는 모든 데이터가 대기한다.
    - websocket 통해 대량 데이터 전송시 애플리케이션 수준에서 청킹 필요
    - 먼저 대량의 데이터를 청킹해서 websocket 으로 보낼 수밖에 없는지를 점검할 것
- 단, ping, pong, close 등 제어 프레임은 끼어들 수 있음

```
ws.send(fileA); // 즉시 리턴
ws.send(fileB); // 즉시 리턴
```

- fileA 의 조각 순차 전송
- fileA 의 FIN=1 프레임 전송 완료 이후 fileB 조각 전송 시작

## 리버스 프록시 구조에서 websocket

- 일반적으로 client-server 구조에서 중간에 nginx, haproxy 와 같은 리버스 프록시가 있음
    - client <—TCP—> reverse proxy(nginx, haproxy..) <—TCP—> was(websocket server)
- 리버스 프록시는 websocket upgrade 이후 websocket frame 을 이해하지 못하고 101 switching protocol 이후 tunnel 모드로 전환
    - websocket frame 을 단순히 전달
- 리버스 프록시는 스스로 제어 프레임을 생성하지 못하므로 만약 리버스 프록시의 설정에 의해 tcp 연결을 끊는 경우 close 와 같은 제어 프레임은 발행되지 않음
    - close 프레임 없이 tcp 연결 자체를 close
- 주의
    - haproxy 기준
    - defaults에 `timeout tunnel` 설정이 없는 경우 `client/server timeout` 이 그대로 적용되어 websocket 이 `client/server timeout` 주기로 계속 재연결
    - `client/server timeout`  은 짧게두되 `timeout tunnel` 값은 별도로 길게 설정 필요 (`timeout tunnel  1h`)
    - https://docs.haproxy.org/3.1/configuration.html#4.2-timeout%20tunnel
    - https://www.haproxy.com/documentation/haproxy-configuration-tutorials/protocol-support/websocket/

## 제어 프레임

- 제어 프레임은 opcode 의 최상위 비트가 1인 것들
- close, ping/pong
- 모든 제어 프레임 페이로드는 125byte 이하여야 하며, 조각화될 수 없음

### ping/pong

- ping 프레임 수신시 반드시 가능한 한 빨리 pong 프레임을 응답해야 한다.
- pong 응답은 ping 메세지 본문과 동일한 payload 를 담아야 함
- 중복 ping 처리
    - 이전 ping에 대한 pong 응답을 아직 하지 못한 상태에서 ping 수신시 가장 최근 ping에 대해서만 pong 보내면 됨
- pong 프레임은 요청없이 보낼 수 있으며, 단방향 하트비트 역할을 수헹
- 사용 목적1: keepalive
    - 중간 네트워크 장비가 연결을 끊지 못하도록 트래픽을 흘린다.
- 사용 목적2: 상대가 살아있는지 확인
    - TCP 특성상 상대가 죽어도 즉시 알려주지 않는다.
    - 서버가 SIGKILL 등으로 죽어 FIN 프레임을 보내지 못한 경우 소켓은 열려있는 것처럼 보이고 send 도 성공됨 (좀비 커넥션)
    - 일정 주기로 ping/pong 을 반복하며 임계값 이상 pong을 받지 못한 경우 해당 커넥션이 끊어진 것으로 판단 가능

### close

- https://www.rfc-editor.org/rfc/rfc6455.html#section-5.5.1
- websokcet closing handshake 를 위한 프레임
- client/server 어느쪽이든 close 제어 프레임을 보낼 수 있음
- close 프레임을 보내고 close 응답을 받기 전까지 수신한 websocket 메세지는 정상 수신 처리. close 응답 이후 수신된 프레임은 폐기 처리
- close 프레임의 opcode는 `0x8` 이고, payload 125byte 에는 status code 2바이트 제외 123바이트의 reason을 포함할 수 있음
    - status code
        - 1000: 작업 정상적으로 끝남 (재연결 x)
        - 1001: 서버 셧다운/배포 등 (백오프 후 재연결)
        - 1006: close 프레임 없이 TCP 연결이 끊어진 경우
            - 중간 리버스 프록시를 두는 구조에서 리버스 프록시에 의해 tcp 연결이 끊어지는 등의 케이스에서 발생
        - 1008: 인증 실패, 권한없음, 정책위반.. (재연결 전 토큰 갱신)
        - 1009: 메세지 크기초과 (클라이언트 버그)
        - 1011: 서버 내부 오류 (재연결)
        - 4000 ~ 4999: 커스텀 코드
- close 프레임 수신시 응답에는 status code를 그대로 echo
- client또는 server 가 close 프레임을 보내고 상대방이 응답하는 경우 server 가 tcp close 를 위한 FIN 플래그 발행 (이후 4way handshake 통해 tcp 종료)
    - close 프레임을 client 에서 발행했어도 server 의 TCP FIN 플래그를 기다리는 것을 권장

```json
1. client 또는 server close 프레임 발행
2. close 프레임 수신한 쪽은 close 프레임 응답
3. server 측에서 TCP close 위해 FIN 플래그 발행
```

## Reference

- https://developer.mozilla.org/ko/docs/Web/API/WebSockets_API
- https://www.rfc-editor.org/info/rfc6455/
- https://sendbird.com/ko/developer/tutorials/websocket-vs-http-communication-protocols