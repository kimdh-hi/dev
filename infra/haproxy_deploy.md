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

### haproxy 설정파일 설정 (haproxy.cfg)
```
frontend fe_app
    bind :80
    # bluegreen.map에서 'active' 키를 찾아 그 값(be_blue or be_green)의 backend로 보낸다
    # 키를 못 찾으면 be_blue로 보낸다
    use_backend %[str(active),map(/etc/haproxy/bluegreen.map,be_blue)]

backend be_blue
    option httpchk
    http-check send meth GET uri /health
    http-check expect status 200
    server blue1 127.0.0.1:8081
    server blue2 127.0.0.1:8082

backend be_green
    option httpchk
    http-check send meth GET uri /health
    http-check expect status 200
    server green1 127.0.0.1:9081
    server green2 127.0.0.1:9082
```

### haproxy map 파일 정의
- map 파일은 배포 파이프라인 내 스크립트를 통해 갱신
  - active be_blue --> active be_green
```
# /etc/haproxy/bluegreen.map
active be_blue
```

### HAProxy Runtime API
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


