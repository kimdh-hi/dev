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

## reference
- https://docs.docker.com/reference/cli/docker/container/logs/
- claude