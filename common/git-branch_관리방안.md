# git 브랜치 관리 방안

## 태그 기반 관리

- https://docs.gitlab.com/user/project/repository/branches/strategies/
- main 에는 항상 최신 소스를 유지
- main → feature 브랜치 구조
    - main 으로부터 feature 브랜치 분기
    - 작업 이후 main 으로 merge 이후 feature 제거
- main push 시 gitlab ci 통해 개발서버 자동 배포
- 알파/운영 배포 방안
    - main 커밋중 특정 커밋에 tag 지정하여 push
    - gitlab ci 는 tag 네이밍 기반으로 해당하는 파이프라인 자동 실행
        - tag: 1.1.0-alpah —> 알파
        - tag: 1.1.0 —> 운영
        - tag 에 “-alpha” 포함하는 경우 알파 파이프라인
        - tag 에 “alpha”, “beta”, “staging” 등 포함하지 않는 경우 운영 파이프라인
        - `^\d+\.\d+\.\d+$`                         → 운영 (manual 승인 후 배포)
        - `^\d+\.\d+\.\d+-alpha\.\d+$`   → 알파 (자동 배포)
        - 개발서버 배포 파이프라인은 main 배포마다 조건없이 매번 구동

```json
main:  A --- B(1.1.0-alpha) --- C --- D --- E(1.1.0)
                  ↑ QA 검증 대상            ↑ 실제 운영 배포 대상??
```

- C, D 는 QA 대상이 되지 않은 커밋이므로 운영 배포 대상이 되면 안됨
- 운영 태그를 알파 태그와 동일한 커밋에 지정

```json
main:  A --- B --- C --- D --- E
                 ↑
        1.1.0-alpha
        1.1.0            ← 같은 커밋 B에 두 태그
```

```json
git tag -a 1.1.0 1.1.0-alpha^{} -m "release 1.1.0"
git push origin 1.1.0 
```

### 1.1.0-alpha QA fail 시

- 1.1.0-alpha tag 로부터 release/1.1.0 브랜치 분기
- QA 실패 수정 이후 release/1.1.0 에서 1.1.0-alpha.2 tag push
- QA 성공 이후 release/1.1.0 에서 1.1.0 tag push
- 수정사항 main 에 merge
- docker image tag
    - main: `my-app:latest` or `my-app:[commit-hash]`
    - alpha: `my-app:1.1.0-alpha`, `my-app:1.1.0-alpha.2`
    - prod: `my-app:1.1.0`