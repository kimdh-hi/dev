# FUSE 마운트 된 경로 container 에서 볼륨 마운트시 접근 불가 이슈

## 배경

- docker container 는 기본적으로 root(uid) 으로 실행
    - 별도 USER 지정하지 않은 경우 기본값으로 root(uid:0, gid:0) 으로 실행
- 호스트 경로를 마운트 한 경우 컨테이너에서 마운트 된 호스트 경로를 볼 수 있음
    - mount namespace
- 기본적으로 컨테이너에서 마운트를 걸면 컨테이너는 root 권한으로 실행되므로 rwx 권한 관계없이 권한 체크 무시하고 접근 가능

## FUSE

- FUSE (Filesystem in Userspace)
- 호스트 특정 경로를 object storage(oci bucket, aws s3..) 에 마운트하게 되면 실제 마운트 된 경로는 폴더처럼 보이게 만든 가짜 폴더가 된다.
- FUSE 는 가짜 폴더로부터 파일 읽기/쓰기 등 시 실제로는 oci로부터 파일을 읽고 쓰는 것이지만 사용자로 하여금 폴더에서 해당 작업 발생한 것처럼 동작한다.
- 일반 사용자가 호스트 특정 경로에 FUSE 마운트시 해당 경로에 대한 쓰기 권한이 있어야 한다.
- 마운트 후에 해당 경로에는 마운트를 한 uid만 접근 가능하다 (root 조차 접근 불가)
- 즉, docker 가 FUSE 마운트 된 경로를 볼륨 마운트 한 경우 해당 경로를 볼 수는 있지만 그 어떠한 접근도 불가하다.
- 단, FUSE 마운트 옵션에 따라 root 를 허용할수도, 제3자를 허용하는 것이 가능하다.
    - `allow-root`: root 접근 허용, 제3자x
    - `allow-other`: root, 제3자 모두 허용

## FUSE 마운트 된 경로 docker container 에서 볼륨 마운트시 접근 불가 이슈 해결방안

- FUSE 마운트 옵션 수정
    - `allow-root`: root 접근 허용, 제3자x
    - `allow-other`: root, 제3자 모두 허용
    - `allow_other` + `default_permissions`
- docker 컨테이너 자체가 FUSE 마운트 한 user 권한으로 실행하여 해결
    - `user: "<FUSE mount uid>:<FUSE mount uid>"`

## reference

- https://docs.kernel.org/filesystems/fuse/fuse.html
- https://docs.docker.com/engine/storage/bind-mounts/
- https://docs.docker.com/reference/compose-file/services/