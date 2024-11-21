docker dangling image

dangling image
- 태그가 없는 이미지 `<none>`
- 다른 이미지, 컨테이너에서 참조되지 않는 이미지
- `docker images` 조회시
  - repository, tage 부분이 <none> 으로 표시됨
- dangling image 만 조회
  - `docker images -f dangling=true`
- dangling image 일괄삭제
  - `docker image prune`
  - prune 명령어는 dangling 이미지 외 이미지에는 영향을 주지 않는다.

docker prune option
- `-a`: 사용되지 않는 모든 image 제거 (dangling image 포함)
- `-f`: y/N prompt 미노출


reference
- https://docs.docker.com/reference/cli/docker/image/prune/