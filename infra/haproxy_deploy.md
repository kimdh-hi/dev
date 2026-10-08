# haproxy deploy

https://www.haproxy.com/documentation/haproxy-configuration-tutorials/proxying-essentials/custom-rules/map-files/

## haproxy blue/green deploy
- blue/green deploy?
  - 동일 환경을 만들어 한쪽은 현재버전(blue), 다른 한 쪽(green)에는 새 버전을 구동
  - 그린 환경에서 테스트가 끝나면 실 트래픽을 그린 환경으로 돌리고 블루 환경을 폐기하는 전략
  - 배포 실패시 롤백을 단순화하고 다운타임과 배포 위험을 줄이기 위한 전략
  - blue/green 색상 자체에 대한 정해진 역할은 없고 green으로 전환 완료 이후 green 이 blue로 개명되는 것이 아니고 이후 버전에는 blue 에 새 버전을 올린다.
  - https://martinfowler.com/bliki/BlueGreenDeployment.html
  - https://docs.aws.amazon.com/whitepapers/latest/blue-green-deployments/welcome.html
- haproxy 는 들어온 요청을 어느 서버 그룹으로 보낼지를 결정
- haproxy 에서는 blue/green 그룹별 n개 인스턴스를 선언 가능
  - 포트로 구분 (단일 서버 가정이므로)
- 사용되는 haproxy 요소
  - be_blue, be_green 두 개 backend 정의
  - map 파일(스위치) 정의
  - runtime API (reload 없이 트래픽 전환 위함)


## HAProxy Runtime API 기반 blue/green 배포 (map 방식)
- https://www.haproxy.com/documentation/haproxy-runtime-api/reference/set-server/
- 실행중인 haproxy에 명령을 보내, 재시작(reload) 없이 동작을 바꾸는 통로
  - 단, runtime api 통해 변경한 내용은 실제 파일 내용(디스크에 데이터)에 변경사항을 반영하지 않고 메모리 상에서만 변경사항을 적용한다.
- runtime api 통해 메모리 상의 map 파일 값을 변경
  - `set map /etc/haproxy/bluegreen.map active be_green`
- runtime api 통해 명령어 보내는 방법
  - `echo "show info" | socat stdio unix-connect:/run/haproxy/admin.sock`
  - `echo "명령어" | socat stdio unix-connect:/소켓경로`

```
# haproxy.cfg 설정 필요
global
    stats socket /run/haproxy/admin.sock mode 660 level admin
```

- CI 시 계정이 소켓 접근 허용을 위한 설정 필요

```
usermod -aG haproxy <deployuser>
```

### haproxy 설정파일 설정 (haproxy.cfg)
```
global
    # Runtime API 소켓: 배포 스크립트가 set map 명령을 보내는 통로
    stats socket /run/haproxy/admin.sock user haproxy group haproxy mode 660 level admin

frontend fe_app
    bind :80
    # bluegreen.map에서 'active' 키를 찾아 그 값(be_blue or be_green)의 backend로 보낸다
    # 키를 못 찾으면 be_blue로 보낸다
    use_backend %[str(active),map(/etc/haproxy/bluegreen.map,be_blue)]

backend be_blue
    option httpchk
    http-check send meth GET uri /health
    http-check expect status 200
    # 헬스체크 적용: 2초 간격, 3회 연속 실패 시 DOWN, 2회 연속 성공 시 UP
    server blue 127.0.0.1:8081 check inter 2s fall 3 rise 2

backend be_green
    option httpchk
    http-check send meth GET uri /health
    http-check expect status 200
    server green 127.0.0.1:9081 check inter 2s fall 3 rise 2
```

### haproxy map 파일 정의
- map 파일은 배포 파이프라인 내 스크립트를 통해 갱신
  - active be_blue --> active be_green
```
# /etc/haproxy/bluegreen.map
active be_blue
```

### 기존 was shutdown 시점
- map 파일 변경 이후 이미 haproxy 는 blue 로 트래픽을 보내지 않으므로 set map 이후 show map 정도만 확인하고 graceful shutdown
- shutdown 조건 검토
  - set map 이후 show map 이 바뀐 것을 확인 했을 때
  - show stat 에서 기존 blue 의 세션수가 0일 때

## HAProxy 헬스체크 기반 blue/green 배포
- HAProxy 는 등록된 모든 서버 대상 주기적으로 http 요청 통해 헬스체크
- 응답으로 200을 반환한 서버로만 트래픽을 보낸다.
- Runtime API 의 경우 트래픽을 흘릴 대상을 배포 프로세스에서 직접 지정하지만 헬스체크 방식은 각 was 가 제대로 구동되고 기존 was 를 내리는 것만으로 트래픽 전환 가능

```
frontend fe_app
    bind :80
    # map 없이 하나의 backend로 보낸다
    # blue/green 중 어느 쪽으로 갈지는 be_app의 헬스체크 결과가 결정한다
    default_backend be_app

backend be_app
    # readiness 검사 200이면 트래픽 대상, 그 외 제외
    option httpchk
    http-check send meth GET uri /health/ready
    http-check expect status 200

    # 종료 중인 서버가 연결을 거부하면 최대 3회 재시도, 매 재시도마다 다른 서버로 재전송
    retries 3
    option redispatch 1

    # 헬스체크 적용: 2초 간격, 3회 연속 실패 시 DOWN, 2회 연속 성공 시 UP
    # 평상시에는 한쪽만 실행되어 있으므로 다른 쪽은 DOWN 상태 (정상)
    server blue  127.0.0.1:8081 check inter 2s fall 3 rise 2
    server green 127.0.0.1:9081 check inter 2s fall 3 rise 2

# (선택) 배포 스크립트가 서버 상태를 조회하기 위한 읽기 전용 stats
# stats admin이 없으므로 조회만 가능
#  https://www.haproxy.com/documentation/haproxy-configuration-tutorials/alerts-and-monitoring/statistics/
frontend fe_stats_ro
    bind 127.0.0.1:8404
    stats enable
    stats uri /haproxy-stats
    stats scope be_app
```

### 기존 was shutdown 시점1
- haproxy 서버에서 /haproxy-stats 로 공개한 stats 를 배포 프로세스에서 스크립트가 지속적으로 green 상태가 UP으로 판정됐는지 체크
- UP으로 판정됐을 때 스크립트는 blue 프로세스를 shutdown
- 단, 위 방식 사용시 was 서버 -> haproxy 서버 통신 허용 필요 (방화벽 open)

### 기존 was shutdown 시점2
- was -> haproxy stats 조회 요청 불가능한 경우
- haproxy 측  인터벌(rise × inter)만큼 대기 후 기존(blue) was shutdown 하는 것도 가능하지만, 어떤 이유로든 green 이 트래픽을 받을 수 없는 상태에서 blue 가 내려가게 될 수 있음
- was -> haproxy 요청 없이 green 으로 haproxy 통해 트래픽이 들어오는지 배포 스크립트가 확인 가능해야 함.
- `http-check send meth GET uri /health/ready ver HTTP/1.1 hdr Host localhost hdr X-Health-Check haproxy`
  - haproxy 가 헬스체크를 was 로 보낼 때 http 요청 내용을 정하는 설정
- 헬스체크 인터벌이 2초이므로 2초마다 아래 요청을 was 로 보냄

```
GET /health/ready HTTP/1.1
Host: localhost
X-Health-Check: haproxy
```
- was 에서 위 헤더를 tomcat accessLog 에 남기도록 하고 배포 스크립트는 accessLog 에 해당 헬스체크 헤더값이 들어오는지 확인
- 헤더가 지정한 개수만큼 발견된 경우 blue was shutdown

```
frontend fe_app
    bind :80
    default_backend be_app

backend be_app
    option httpchk
    http-check send meth GET uri /health/ready ver HTTP/1.1 hdr Host localhost hdr X-Health-Check haproxy
    http-check expect status 200

    retries 3
    option redispatch 1

    default-server check inter 2s fall 3 rise 2
    server blue  10.0.0.10:8081
    server green 10.0.0.10:9081
```


## haproxy canary deploy
- canary?
  - blue/green 과 동일하게 두 종류의 환경을 두고 트래픽을 전환하지만 새 버전으로 트래픽을 점진적으로 전환하는 것이 blue/green 과의 차이
- haproxy 는 backend 설정시 각 서버마다 weight 설정을 통해 트래픽 비율을 조절할 수 있음

```
backend be_app
    balance roundrobin
    option httpchk
    http-check send meth GET uri /health
    http-check expect status 200
    default-server check inter 2s fall 3 rise 2

    server blue1  127.0.0.1:8081 weight 100
    server blue2  127.0.0.1:8082 weight 100
    server green1 127.0.0.1:9081 weight 0     
    server green2 127.0.0.1:9082 weight 0
```

