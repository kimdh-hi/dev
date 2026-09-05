# websocket

## websocket
- websocket 은 TCP 연결 위 client/server 가 양방향으로 데이터를 송수신하는 통신 프로토콜
- websocket 이전 양방향 통신을 위해 http 폴링을 주로 사용
- 양방향 통신시 http 폴링의 오버헤드 해법으로 제안

## polling 시 websocket 대비 비용
- 요청당 헤더 오버헤드
  - 요청마다 cookie, Authorization, User-Agent 등등 헤더값이 중복으로 왕복
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
- Sec-WebSocket-Accept
  - 인증 등의 보안장치 아님
  - 상대가 websocket 을 이해 가능한 상태인지 확인하는 용도
  - `Sec-WebSocket-Accept = base64( SHA1( Sec-WebSocket-Key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11" ) )`
  - `258EAFA5-E914-47DA-95CA-C5AB0DC85B11` 는 websocket 스펙이 정의하는 고정 GUID

## Frame
- opening handshake 통해 websocket 연결 수립 이후부터는 client/server 모두 heeader/payload 구조의 http 메세지가 아니고 byte frame을 주고 받음
- frame 안에는 http 메서드/경로/상태코드/헤더/쿠키 등이 없음. websocket 스펙이 정의하는 byte 형태의 frame 만 
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
- websocket 커넥션은 단일 송신 파이프로 각 메세지에 대한 식별자, 우선순위 등등이 없으므로 대량의 데이터를 통째로 밀어넣는 경우 해당 커넥션으로 가는 모든 데이터가 대기한다.
  - websocket 통해 대량 데이터 전송시 애플리케이션 수준에서 청킹 필요
- 단, ping, pong, close 등 제어 프레임은 끼어들 수 있음

```
ws.send(fileA); // 즉시 리턴
ws.send(fileB); // 즉시 리턴
```
- fileA 의 조각 순차 전송
- fileA 의 FIN=1 프레임 전송 완료 이후 fileB 조각 전송 시작


## Reference
- https://developer.mozilla.org/ko/docs/Web/API/WebSockets_API
- https://www.rfc-editor.org/info/rfc6455/