# loopback address

## loopback address?

- 호스트가 자기 자신에게 ip 패킷을 보낼 때 사용하는 예약된 주소
- 이 주소로 향하는 패킷은 ip 레이어 이하 NIC, 스위치 등 물리 계층으로 내려가지 않고 호스트의 ip 레이어 내부로 그대로 되돌아 온다.
    - 가상 네트워크 장치 (lo, loopback device) 까지 내려갔다가 돌아옴
- IPv4 내부 호스트 루프백 주소 표준: 127.0.0.0/8
    - 표준은 127 로 시작하는 모든 127.x.x.x 대역을 루프백 주소로 정의(예약)
    - 단, OS 에서 127.0.0.1 만 루프백 주소로 할당
    - linux의 경우 127.0.0.1 ~ 127.255.255.254 루프백 ip로 사용 가능
- 일반적으로 127.0.0.1 == loopback ip
- 일반적으로 `/etc/hosts` 에 localhost 가 127.0.0.1 로 매핑되어 있으므로 localhost == 127.0.0.1 == loopback ip
- IPv6 루프백 ip: ::1/128
    - IPv4와 다르게 표준 자체가 한 개 IP 만을 예약

/etc/hosts

```
127.0.0.1   localhost
::1         localhost
```

## localhost AAAA, A 레코드 이슈

- localhost 는 A레코드(127.0.0.1), AAAA레코드(::1) 모두 가진다.
- getaddrinfo() 가 가진 기본 정책 테이블에 따라 정렬해서 반환
- 서버가 127.0.0.1:8080 에만 바인딩 된 경우 localhost:8080 으로 접속시 ::1 로 먼저 시도하는 경우 ECONNREFUSED 에러 발생 가능
    - 과거 node.js 에 관련 이슈 있었음
    - https://github.com/nodejs/node/pull/39987
    - https://github.com/nodejs/node/issues/40537

## reference

- https://www.rfc-editor.org/info/rfc1122/
- claude