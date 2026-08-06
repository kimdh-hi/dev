# /etc/hosts, /etc/resolv.conf

- `/etc/hosts`
    - 호스트 테이블
    - 호스트명과 ip 를 매핑정보를 저장하는 정적 테이블
- `/etc/resolv.conf`
    - DNS 질의를 어디로, 어떻게 보낼지 정하는 resolver 설정파일
- 어느 것을 먼저 볼지는 `/etc/nsswitch.conf` 에서 결정

## /etc/hosts

- https://man7.org/linux/man-pages/man5/hosts.5.html
- DNS 가 호스트 테이블을 대체한 이후에도 널리 사용
- 파일 수정시 보통 즉시 반영
- 이름, 주소 매핑만 가능
    - 포트, 프로토콜, URL 경로 표현 불가
    - 와일드카드 불가 (각 서브도메인마다 줄 추가 필요)
    - dns 도구는 호스트 테이블 참고하지 않는다. (`dig`, `nslookp`, `host` ..)
- IP주소와 호스트명을 연결하는 단순 텍스트 파일로, IP 주소 하나당 한 줄로 기술

```
IP_address canonical_hostname [aliases...]
```

- IPv4/IPv6 모두 가능

## /etc/resolv.conf

- 어떤 DNS 서버에 질의할지 설정 및 DNS 질의 -> `nameserver`
- 이름 뒤로 특정 도메인 붙여서 완성 -> `'search' + 'options ndots'`

```
nameserver 192.168.1.1
nameserver 8.8.8.8
search corp.example.com example.com
options ndots:1 timeout:2 attempts:2
```

### nameserver

- `nameserver 192.168.1.1` : 첫번째 dns 서버
- 첫번째 dns 서버에서 타임아웃 발생시 다음 nameserver 로 질의
- 첫번째 dns 에서 질의 결과 매핑되는 ip를 찾지 못한 경우 응답은 정상이므로 질의를 멈춤
- 첫번째 이후 nameserver 는 이전 nameserver 가 죽었을 때를 대비하는 것이고, 답을 찾지못했을 때(NXDOMAIN) 를 대비하는 것이 아님

### search

- `search corp.example.com example.com`
- `ping aaa` 시 `ping aaa.corp.example.com` 으로 변환
    - `aaa.corp.example.com` 가 없는 경우 `aaa.example.com` 으로 시도
    - 이후 마지막으로 `aaa` 로 시도
- trailling dot
    - `aaa.corp.example.com.` 처럼 뒤에 점 붙이는 경우 search, ndots 설정과 무관하게 뒤에 search 붙이지 않음.
    - 이름 그대로 질의

### ndots

- default: 1
- ndots 는 `.` 숫자에 따라서 dns 질의 순서를 결정
    - search 를 뒤에 붙여서 먼저 질의 할지, 이름 그대로 질의할지 순서.
- 점 개수 < ndots
    - 1순위: 이름 뒤에 search 붙여서 dns 질의
    - 2순위: 이름 그대로 질의
- 점 개수 >= ndots
    - 1순위: 이름 그대로 질의
    - 2순위: 이름 뒤에 search 붙여서 dns 질의

## /etc/nsswitch.conf

- nsswitch (`NameServiceSwitch`)
- 이름 관련 정보를 어떤 소스에서, 어떤 순서로 가져올지 결정하는 설정파일
- 호스트명 뿐아니라 계정, 그룹, 서비스 포트 등 여러 범주를 포함
- `/etc/hosts` 가 DNS 보다 먼저 조회되도록 하는 것도 `/etc/nsswitch.conf` 의 설정 결과
- 파일 구조: 데이터베이스: 소스1 [상태=동작] 소스2 소스3

```
# /etc/nsswitch.conf
...
hosts:      files dns myhostname # /etc/hosts --> /etc/resolv.conf --> 자기 호스트명, localhost
...
```

## reference

- https://man7.org/linux/man-pages/man5/hosts.5.html
- https://man7.org/linux/man-pages/man5/resolv.conf.5.html
- https://man7.org/linux/man-pages/man5/nsswitch.conf.5.html
- claude