docker context
- docker cli 대상 환경 설정 (local, remote...)
- docker cli 는 기본적으로 local docker daemon 과 상호작용 함.
- docker context 사용시 원격 서버, 클라우드 인프라의 docker daemon 과 쉽게 연결 가능
  - 배포시 원격서버에서 이미지 build, run 등의 docker cli 수행 등.

docker context cli
```
//context 목록
docker context ls 

//context detail
docker context inspect

//context 생성
docker context create my-context --docker "host=ssh//user@remote-host"

//context 전환
docker context use my-context

//해당 context 에서 명령어 수행
docker --context my-context run -d --name ....

//특정 user context cli
- sudo -u [user] docker context ls
- sudo -u [user] docker inspect [contextName]
```

docker context meta 정보
- `~/.docker/contexts/meta`