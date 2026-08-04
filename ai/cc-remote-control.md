## claude code remote-control
- https://code.claude.com/docs/en/remote-control
- mobile -> local claude code session 프롬프팅
- 원리
  - remote-control 세션 시작시 anthropic api 통해 본인 세션(local claude code session)을 등록
  - 등록 후 anthropic 서버에 폴링 (로컬에서 anthropic 서버쪽으로 https 통해 폴링. 인바운드 포트는 열지 않는다.)
  - 모바일 또는 브라우저는 특정 remote-control session 을 식별하고 프롬프팅 (anthoripic 서버로 메세지 발송)
  - anthropic 서버를 폴링하는 로컬 remote-control 세션이 메세지를 가져가서 처리 및 응답 스트리밍

claude cli
```
claude --remote-control (claude --rc) // 인터렉티브 세션

claude remote-control // 서버모드
```

claude desktop
```
settings > claude code > 기본적으로 원격 제어 활성화 설정
```

### 서버 모드, 인터렉티브 모드
- 서버 모드: `claude remote-control`
- 인터렉티브 모드: `claude --remote-control`

#### 서버 모드
- `claude remote-control` 시 원격 접속 가능한 QR코드, 세션 URL 을 노출
- QR 통해 접근시 claude 모바일 앱 등을 통해 원격지에서 프롬프팅 가능
- 한 개 프로세스에서 최대 32개 세션까지 연결 가능
  - 한 개 `claude remote-control` 로부터 n개 디바이스에서 각각 개별 세션 연결 가능
  - `--capacity <N>` 통해 최대 세션 수 제어 가능 (default: 32)
- `--spawn <mode>` 통해 세션 생성 방식 지정 가능
  - `same-dir` (default): 모든 세션이 작업 디렉토리를 공유
  - `worktree`: 세션마다 개별 git wortree 를 갖는다
  - `session`: 한 개 세션만을 허용

#### 인터렉티브 모드
- `claude --remote-control "session name"`
- 로컬 세션, 원격지 세션 모두 프롬프팅이 가능한 대화형 세션
- 한 개 프로세스에서 한 개 세션만 지원
- 한 개 `claude --remote-control` 에 n개 디바이스가 연결된 경우 모두 한 개 세션을 공유
