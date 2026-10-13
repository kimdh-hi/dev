# jenkins 서버 용량 부족 이슈

## jenkins shared workspace plugin

- https://plugins.jenkins.io/shared-workspace/
- 각 jenkins job 이 동일한 scm 저장소를 바라보는 경우 매 번 전체 clone 받지 않고 변경분만을 받으며 소스를 공유하도록 하기 위함
    - 동일 shared workspace 공유하는 job 실행시 .git 유무에 따라 전체 clone or 변경분 pull
- scm 저장소 url 을 지정하여 shared workspace 생성 후 job 에서 해당 shared workspace 지정
    - /path/jenkins/sharedspace/{이름} 경로에 생성
- Job 별로 shared workspace 지정시 각 job 은 shared workspace 공유
- sharedworkspace 지정하지 않는 경우 /path/jenkins/workspace/{Job이름} 경로에 생성

## 이슈

- 대상 repo 14.8gib
- 브랜치별로 19gib 씩 늘어남
    - .git(전체 브랜치 이력) + 소스 + 빌드 결과물
    - `sudo du -xh -d 1 /path/jenkins/sharedspace 2>/dev/null | sort -h | tail -2`
- jenkins scm 은 (gitlab, github..) 에서 대상 저장소를 checkout 받아서 사용
- 즉, 배포 대상 브랜치가 늘어날수록 대용량 저장소라면 용량이 매우 커짐

## 개선0 - 미사용 shared workspace 제거

## 개선1 - `xx@2` 제거

- 동일 shared workspace 를 공유하는 job 이 동시에 구동될 때 `@2` 가 붙은 동일 shared workspace 를 별도로 생성
- 이 경로는 @2 를 유발한 job 이 끝나고 다시 사용되지 않을 수 있으므로 제거 대상
- 재현 방지를 위해 동일 shared workspace를 공유하는 job 동시 빌드 비활성화 추가
- `options { disableConcurrentBuilds() }` 또는 Manage Jenkins->Plugins "Lockable Resources" 통해 방지

## 개선2 - shared workspace 플로그인 제거

- 환경별 Pipeline Job 하나로 합치기
- reference repository 사용
- shallow clone 사용