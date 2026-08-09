# docker container log

## docker container log
- 컨테이너 내 애플리케이션이 표준출력(stdout), 표준 에러(stderr) 에 쓴 내용을 docker 데몬이 가로채서 저장하거나 외부로 전달
- 어디에 어떻게 저장하고 전달할지 로깅 드라이버(Logging Driver)가 담당
- docker 데몬마다 기본 로깅 드라이버가 있으며 기본드라이버는 `json-file`
  - 컨테이너 로그를 내부적으로 json 형식으로 관리
- 단순히 `/var/log/xxx.log` 에만 로그를 쓰면 `docker logs` 에는 노출되지 않음.
  - 컨테이너 애플리케이션은 stdout/stderr 로 로그를 내보내야 함
- docker 데몬 기본 로깅 드라이버 확인
  - `docker info --format '{{.LoggingDriver}}'`
- 실행중인 컨테이너 드라이버 확인
  - `docker inspect -f '{{.HostConfig.LogConfig.Type}}' <CONTAINER>`

## 로깅 드라이버
- 호스트 디스크에 파일을 저장하는 로깅 드라이버
- 종류: json-file(default), local
- 디스크 고갈을 막으려면 `local` 드라이버 권장
- `json-file` 은 기본적으로 로그 회전을 하지 않으므로 출력이 많은 컨테이너는 디스크 공간 고갈 이슈 발생 가능
  - `json-file` 은 매 로그마다 stream, time 등을 추가로 붙이므로 실제 로그 내용보다 커짐
  - `json-file` 의 경우 기본값이 max-size: -1, max-file: 1 이기 때문
- 여전히 `json-file` 이 기본값인 이유는 하위호환과 k8s 런타임 사용 때문

## 회전 & 압축
- max-size 도달시 회전
- 밀려난 파일은 압축
- max-file 넘으면 가장 오래된 압축 파일 삭제

### json-file (default)
- stdout, stderr 캡쳐해 json 파일로 기록	
- max-size: -1 (무제한, default)
- max-file: 1 (default)
  - max-size 설정 반드시 필요
- compress: 기본 비활성화
```json
{
  "log": "Log line is here\n",
  "stream": "stdout",
  "time": "2019-01-01T11:11:11.111111111Z"
}
```

## 로깅 드라이버 설정 방법

```
# global 설정
# /etc/docker/daemon.json

{
  "log-driver": "local",
  "log-opts": {
    "max-size": "20m",
    "max-file": "5"
  }
}
```

```
docker run -d --name api \
  --log-driver json-file \
  --log-opt max-size=10m --log-opt max-file=3 \
  myapp:latest

# 로그를 아예 남기지 않기
docker run -it --log-driver none alpine ash

# compose
services:
  api:
    image: myapp:latest
    logging:
      driver: json-file | local
      options:
        max-size: "10m"
        max-file: "3"

# compose yaml 앵커 방식
x-logging: &default-logging
  driver: local
  options:
    max-size: "20m"
    max-file: "5"

services:
  api:
    image: myapp:latest
    logging: *default-logging
  worker:
    image: myworker:latest
    logging: *default-logging
```

### local
- stdout/stderr 출력을 디스크 사용량에 최적화된 내부 저장 형식으로 저장
- 컨테이너 당 100mb 분량의 로그를 보존하고 자동 압축
  - max-size: 20m (default)
  - max-file: 5 (default)
  - compress: 기본 활성화


## 전달 모드 (blocking, non-blocking)
- docker는 컨테이너에서 로그 드라이버로 메세지를 전달하는 두 가지 모드를 제공
- blocking(default): 컨테이너에서 드라이버로 직접 전달
- non-bloking: 컨테이너별 중간 버퍼에 로그 메세지를 저장해서 드라이버가 소비하는 방식
- 컨테이너 프로세스가 write() 로 stdout에 로그를 쓰면, 해당 데이터는 docker 데몬을 거쳐 로깅 드라이버로 감
- 디스크 I/O 포화 등의 문제로 인해 드라이버에 지연이 발생하는 경우 애플리케이션의 write() 호출 자체가 반환되지 않아 로그 찍는 행위로 인해 api 응답이 나가지 않을 수 있음

### blocking
- default 동작
- 컨테이너 앱 write() -> 로깅 드라이버로 직접 전달 -> 저장/전송
- 장점: 로그는 유실되지 않고 그 순서가 보장됨
- 단점: 로깅 지연이 애플리케이션 지연이 됨
  - 로깅 드라이버로 직접 전달 쪽에서 막히면 응답 자체가 지연됨

### non-blocking
- 컨테이너 앱 write() -> 메모리 링 버퍼 -> 로깅 드라이버가 비동기로 소비 -> 저장/전송
- 컨테이너 앱은 write() 이후 즉시 로깅관련 작업 종료
- 버퍼가 가득차면 새 메세지는 버퍼에 쓰이지 않음 (로그 유실)
- 장점: 애플리케이션은 로깅으로 인해 지연이 발생되지 않음
- 단점: 로그 유실 가능
  - 로깅이 애플리케이션 지연에 영향을 주는 것보다 로그 유실이 괜찮다는 판단
- 버퍼 크기는 `max-buffer-size` 를 통해 설정
  - default: 1m (1MB)
- 기본값 사용시 한 개로그를 2kb 로 잡으면 버퍼에는 480여개 로그 보관 가능
  - 로깅 드라이버가 멈췄을 때 480여개 이후에는 유실

### 설정방법
```
docker run -it \
  --log-opt mode=non-blocking \
  --log-opt max-buffer-size=4m \
  alpine ping 127.0.0.1

# 글로벌 설정 (/etc/docker/daemon.json)
{
  "log-driver": "local",
  "log-opts": {
    "mode": "non-blocking",
    "max-buffer-size": "4m",
    "max-size": "20m",
    "max-file": "5"
  }
}

# Compose
services:
  api:
    image: myapp:latest
    logging:
      driver: local
      options:
        mode: "non-blocking"
        max-buffer-size: "4m"
```



## reference
- https://docs.docker.com/reference/cli/docker/container/logs/
- claude