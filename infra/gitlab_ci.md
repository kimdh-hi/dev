# gitlab ci/cd

## GitLab Runner 설치 및 등록

- https://docs.gitlab.com/runner/install/
- GitLab 이 서비스되는 서버와 **별도 독립적인 서버에 GitLab Runner 설치 권장**
    - GitLab Runner 는 `.gitlab-ci.yml` 의 `script` 에 명시된 명령어를 실행
    - 해당 명령어는 코드를 push 할 수 있는 모든 사용자가 작성/수정 가능하므로 **Runner 입장에서 script 를 포함하는 job 은 외부에서 들어온 통제되지 않은 코드**이므로 보안이슈 발생이 가능한 것
    - https://docs.gitlab.com/runner/security/
- OS 별, docker, k8s 등 설치방식 제공
    - https://docs.gitlab.com/runner/install/
- Runner 등록
    - https://docs.gitlab.com/runner/register/
    - runner 를 하나 이상 gitlab 인스턴스와 연결하는 프로세스
    - runner, gitlab 인증
        - https://docs.gitlab.com/security/tokens/#runner-authentication-tokens
        - gitlab 16.0부터 **runner authentication token 도입**되어 runner registration token 대체 (deprecated, 18.0부터 제거)
            - 현재 사내 기준 v18.0.2 로 확인
        - **gitlab UI 통해 runner 생성** 이후 발급받은 **authentication token 을 runner 에 등록**
    
    ```
    //gitlab runner 서버에서 authentication token 발급 예시
    
    > sudo gitlab-runner register
    
    Enter the GitLab instance URL: → 입력
    Enter the registration/authentication token: → 입력
    Enter a description for the runner: → 입력
    Enter tags for the runner: → 입력
    Enter optional maintenance note: → 입력
    Enter an executor: → 입력
    ```
    
    ```
    runner registration token
    - registration token 발급
    - registration token 통해 runner 생성
    
    runner authentication token
    - gitlab UI 통해 runner 생성
    - 생성시 authentication token 발급
    - 해당 runner 에 token 등록
    
    => runner 를 등록할 수 있는 token 에서 해당 runner 를 인증할 수 있는 토큰으로 개선
    ```
    
- gitlab UI runner 생성
    - Settings > CI/CD
    - project/group/instance(global) runner 생성 가능

## Executor

- gitlab runner 가 job 을 실행하는 방법
    - job 을 어떤 환경에서, 어떤 방식으로 실행시킬 것인가를 결정하는 구성요소
    - docker, shell, ssh, virtualbox, k8s …
- 설정방법
    - runner register 시 (`sudo gitlab-runner register` ) 대화형 인터페이스 통해 등록
        - Enter an executor:
    - config.toml 의 `[[runners]]` 섹션 수정

```
[[
runners
]]
  name = "dev-server-docker-runner"
  url = "https://gitlab.example.com"
  # 아래 4개는 register 시 자동 생성됨. 직접 손으로 쓰지 않음
  id = 12
  token = "glrt-xxxxxxxxxxxxxxxxxxxx"
  token_obtained_at = 2026-07-20T00:00:00Z
  token_expires_at = 0001-01-01T00:00:00Z

  executor = "docker"
```

- shell executor 사용시 주의
    - gitlab-runner user 의 권한으로 호스트에서 직접 쉘 명령어 실행하는 형태이므로 보안이슈 발생 가능
    - container 방식과 다르게 이전 runner 의 실행결과 생성된 것들이 깨끗이 정리되지 않음.
- docker executor
    - docker 설치 및 runner 에서 docker daemon 접근 허용 필수
    - 각 job 을 별도 docker container 상에서 실행하는 방식
        - 격리단위가 파이프라인 단위가 아닌 job 단위임
    - container 특성을 그대로 가지므로 job 간에 커널 수준은 공유