# githooks

## 설정
- $GIT_DIR/hooks
  - .git/hooks 이하에 hooks 설정 파일 위치
- .git 은 원격지 repository 에 push 하지 않으므로 별도 경로에 githooks 설정 파일 commit/push
  - .githooks 이하에 hooks 설정 파일 관리
- `git config core.hooksPath .githooks` 주의
  - git config 이전 기존 `.git/hooks` 에 두고 쓰던 스크립트는 무시됨 
- hook 실행 파일 조건
  - sh, bash, py, node, exe 등 모두 가능
  - 단, 확장자를 포함하지 않는다.
  - 파일 내 첫줄에 무슨 언어인지 명시한다.
  - 실행 권한을 부여한다 (`chmod +x .githooks/pre-commit`)

```
#!/bin/sh
npm run lint || exit 1

#!/usr/bin/env python3
import sys
print("검사 중")
sys.exit(0)   
```


```
my-project/                  
│
├── .git/                    
│   └── hooks/               
│       ├── pre-commit
│
├── .githooks/
│   └── pre-commit
```

- .githooks 이하 githooks 설정을 .git에 설정 (온보딩 필요)
  - `git config core.hooksPath .githooks`

```
# `git config core.hooksPath .githooks` 실행 전 .git/config
[core]
	repositoryformatversion = 0
	filemode = true
	bare = false
[remote "origin"]
	url = https://github.com/me/my-project.git


# `git config core.hooksPath .githooks` 실행 후 .git/config
[core]
	repositoryformatversion = 0
	filemode = true
	bare = false
	hooksPath = .githooks # 추가됨
[remote "origin"]
	url = https://github.com/me/my-project.git
```

## 주요 Git Hooks

### 커밋

| hook | 시점 | 차단 |
|---|---|---|
| `pre-commit` | 커밋 메시지 입력 전 | O |
| `prepare-commit-msg` | 기본 메시지 생성 후, 에디터 열기 전 | O |
| `commit-msg` | 메시지 작성 완료 후 | O |
| `post-commit` | 커밋 완료 후 | X |

### push

| hook | 시점 | 차단 |
|---|---|---|
| `pre-push` | push 직전 (로컬) | O |
| `pre-receive` | 서버에서 ref 갱신 전 | O (전체) |
| `update` | 서버에서 ref별 갱신 전 | O (ref별) |
| `post-receive` | 서버에서 갱신 완료 후 | X |

### 브랜치 · 머지

| hook | 시점 | 차단 |
|---|---|---|
| `pre-rebase` | 리베이스 시작 전 | O |
| `post-checkout` | checkout / switch / clone 후 | 종료코드 전파 |
| `post-merge` | 머지 (= `git pull`) 완료 후 | X |

## reference
- https://git-scm.com/docs/githooks