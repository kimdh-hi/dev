
## tip

```
# accept skip
claude --dangerously-skip-permissions
```

## setting

전역: ~/.claude/settings.json` 
프로젝트: .claude/settings.json

언어 설정
```json
{
  "language": "korean"
}
```

---

## skill


### skill?
- agent 에게 할 수 있는 것, 해야하는 것 등의 지침 제공
  - skill 은 점진적 공개 (progressive disclosure) 의 특징이 있음.
  - 점진적 공개: 해당 skill 이 필요할 때만 해당 md 파일을 로드하여 system prompt 에 포함 (context 낭비 최소화 목적)
  - 프롬프트의 응답에 대해 일관성을 부여하고자 할 때 용이
  - 코딩 컨벤션, 커밋 메세지 포멧 ...
- SKILL.md 
  - `./claude/skills/[스킬이름]/SKILL.md 생성`
  - 대화 시작전 yaml frontmatter 영역 로드
  - name: slash-command 이름
  - description: skill 로드 시점 결정
  - 에이전트 시작시 모든 skill 의 name/description 만 system prompt 에 load (preload)
- SKILL.md 는 500줄 이하로 유지 권장
  - 초과되는 경우 별도 SKILL 분리 혹은 additional file 로 분리
  
```
`./claude/skills/[스킬이름]/SKILL.md 생성`

/skills 통해 생성된 skill 조회
skill 로 등록시 command 로도 등록되므로 /commmand 에서도 확인 가능
```

```md

# skill 예시

---
name: code-review
description: 코드 작성 완료 후 수정된 코드에 대해 코드리뷰를 수행합니다. 새로운 코드가 추가되거나 기존 코드를 수정한 경우 코드리뷰를 수행합니다.
context: fork
agent: Explore
allowed-tools: Read, Bash, Glob, Grep # skill 동작하는 동안 권한 승인 받지않 고 사용 가능한 명령어
model: sonet
---

skill 본문
```

### skill additional resource (additional file, bundle file)
- skill 본문 내 해당 다른 md 파일을 포함시키는 경우 해당 additional file 은 본문과 함께 자동으로 로드되지 않는다. 

```md
---
name: test-guide
description: 테스트 코드 작성시 준수해야 할 가이드를 제공한다.
---

## test code guide
테스트는 이렇게 작성하세요.

### kotest guide
kotest 에 대한 guide 가 필요한 경우 ./kotest.md 를 참조한다.
```

### Skill Creator
- skill 생성시 자연어를 통해 AI 가 이해하기 쉬운 형태의 SKILL.md 을 생성하는 plugin
- https://github.com/anthropics/skills
  - 이미 제공되는 검증된 skill 사용 `/plugin install`
  - https://github.com/anthropics/skills/tree/main/skills
  - .claude-plugin/marketplace.json

```
/plugin marketplace add anthropics/skills 

/plugin install example-skills@anthropic-agent-skills
```

```
skill-creator 이용해서 xxxx 를 하는 스킬 만들어줘
```

### claude code 2.1.0 skill 관련 내용
- skill hot-reload
  - skill 수정시 세션 재시작 없이 반영
- skill 메타데이터에 context 추가
  - context 를 fork 로 지정시(context: fork) agent 에 해당하는 분리된 subagent context 에서 실행됨
  - agent: Explore(코드베이스 탐색), Plan(계획수립), general-purpose(default)

#### skill option
- SKILL.md frontmatter 영역에 여러가지 옵션 부여 가능

```md
---
name: my-skill
description: 이 기술이 하는 것
disable-model-invocation: true # claude 가 해당 skill 을 자동으로 로드하는 것을 방지할지 여부 (default: false)
allowed-tools: Read, Grep # 해당 skill 활성화시 권한을 요청하지 않고 사용할 수 있는 도구
context: fork # 별도 subagent 컨텍스트에서 실행
agent: Explore # subagent 타입 (Explore, Plan, default-purpose(default))
---

기술 지침...
```


### reference

- https://www.anthropic.com/engineering/equipping-agents-for-the-real-world-with-agent-skills

---

## plugin

### plugin?
- claude code 의 기능을 공유, 재사용, 버전관리가 가능한 단위 관리하기 위함
- skill, agent, hook, mcp, lcp 등을 하나의 디렉토리로 묶어서 공유하여 공통으로 팀 단위 사용이 가능
- 단일 프로젝트 내 `.claude/` 이하 여러 파일들을 plugin 화하여 공유 및 여러 프로젝트에 공통으로 적용


### reference

- https://code.claude.com/docs/ko/plugins
- https://roboco.io/posts/oh-my-claudecode-distilled/