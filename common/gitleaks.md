# git 민감정보 유출 방지

## gitleaks
- https://github.com/gitleaks/gitleaks
- go 기반 민감정보 스캐너 
- 전체 git 히스토리 또는 특정 파일 대상 스캔하며 하드코딩 된 민감정보 유츨 탐지


```
Warning

Gitleaks is feature complete. I'm not merging new features into Gitleaks. Future releases will be security patches only. 
I'm shifting my focus to Betterleaks

gitleaks 추가 기능 개발 계획 없음.

betterleaks 대체 가능
https://github.com/betterleaks/betterleaks
```

### 설치
```
cd ~/myproject
gitleaks git -v # git 히스토리 전체 대상
gitleaks dir -v # 특정 경로 대상
cat .env | gitleaks stdin -v # 파이프 통해 특정 파일만 대상
```

### 결과
```
Finding:     SMTP_USER=<secret...>
Secret:      secret...
RuleID:      aws-access-token
Entropy:     3.684184
File:        .env.dev
Line:        132
Commit:      a269c08629a1d91575a408bb3c0ba94ecf70a329
Author:      dhkim2
Email:       dhkim2@??.com
Date:        2026-07-30T05:28:05Z
Fingerprint: a269c08629a1d91575a408bb3c0ba94ecf70a329:.env.dev:aws-access-token:132
```

### 탐지
- 기본 규칙: https://github.com/gitleaks/gitleaks/blob/master/config/gitleaks.toml
- 커스텀 규칙: .gitleaks.toml
- exit 코드 기반 탐지
  - 0: 누출없음
  - 1: 노출 발견 or 에러
  - 126: unknown
- default 탐지 기준
  - `ghp_`, `AKIA`, `sk_live_`, `xoxb-` 등 서비스 제공자가 정한 형식
  - 개인키 블록 헤더, JWT의 점 구분 3세그먼트, 사용자:비밀번호@호스트
  - password, api_key, secret, token 뒤에 붙은 값
  - ...

```
# exit 코드 확인
gitleaks dir; echo "exit=$?"
```

```toml
title = "우리 회사 설정"

[extend]
useDefault = true # 기본 규칙 상속하고 커스텀 규칙을 추가

[[rules]]
id = "acme-internal-token"
description = "ACME 내부 서비스 토큰"
regex = '''acme_tok_[a-zA-Z0-9]{32}'''
keywords = ["acme_tok_"]
```

- 오탐 방지: .gitleaks.toml

```toml
[extend]
useDefault = true
disabledRules = ["generic-api-key"]   # 상속하되 이 규칙은 제외
```

### 적용

#### 적용 전 고려사항
- 적용을 위해 온보딩이 필요한가?
  - git pull 이후 부가적인 설정이 필요한지
  - 설정 자체를 강제화 가능한지
- 민감정보가 git 에 push 될 위험이 있는가?
  - 로컬 또는 gitlab 등 원격지 서버 중 어디서 민감정보 유출 감지를 수행할지
  - 서버에서 하는 경우 이미 push 된 commit 이력이므로 git history 에 남게 되어 해당 민감정보 폐기 및 재발급 필요

#### githooks
- 각각 온보딩 필요, 강제 불가
- 온보딩 위한 sh 파일 제공 필요

```
# .githooks/pre-commit  (저장소에 커밋)
#!/bin/sh
gitleaks git --pre-commit --staged --redact --no-banner -v
```

- 온보딩
```
chmod +x .githooks/pre-commit
git config core.hooksPath .githooks 
```

#### ci 시점 (github action, gitlab ci/cd, jenkins...)
- 온보딩 불필요, 강제 가능
- 단, git 커밋 로그에 민감정보가 남음
- ci 시점에 민감정보 유출로 판단된 경우 해당 키 폐기 및 재암호화/재발급 이후 commit/push 필요

```yaml
# gitleaks
secret_scan:
  stage: verify
  image:
    name: ghcr.io/gitleaks/gitleaks:v8.30.1
    entrypoint: [""]
  interruptible: true
  allow_failure: false
  script:
    - gitleaks dir . --redact --verbose
```

```yaml
# betterleaks
secret_scan:
  stage: verify
  image:
    name: ghcr.io/betterleaks/betterleaks:v1.7.3
    entrypoint: [""]
  interruptible: true
  allow_failure: false
  script:
    - betterleaks dir --redact --verbose --  
```

