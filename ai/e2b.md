## E2B (Execute to Build)

- Firecracker microVM 기반 격리된 환경 명령/코드 실행
    - https://github.com/firecracker-microvm/firecracker
- price
    - https://e2b.dev/pricing
    - Free
        - one-time $100 credit
        - max 20개 concurrently running
        - up to 1-hour session length
    - pro
        - $150/mo
        - max 100 개 concurrently running
        - up to 24-hour session length
    - enterprise
        - …
    - 단순히 sandbox 에 요청 이후 실행하고 응답 후 즉시 종료할 것이라면 concurrently running 만 고민하면 됨.

## E2B Template

- sandbox 는 격리환경 지원을 위한 별도 인스턴스(VM) 이고 Template 은 해당 인스턴스가 어떤 상태로 시작할지를 미리 정의한 청사진
- Template 이 없는 경우 매번 필요할 때 pip install, apt-get install 등 필요한 의존성 등을 세팅하게 됨
    - sandbox 는 매 번 쓰고 버리므로 위 작업이 매 번 발생
- Template 은 설치/설정이 끝난 상태를 스냅샷으로 굳혀두므로, sandbox 생성시 바로 해당 상태에서 시작
- Template 생성방법
    - https://e2b.dev/docs/code-interpreting/customize-template
    - E2B SDK 통한 생성

```jsx
// .env 파일에 E2B_API_KEY 세팅
// template.ts
import { Template } from 'e2b';

export const template = Template()
  .fromTemplate("code-interpreter-v1")
  .aptInstall(['ffmpeg'])                      // 시스템 패키지
  .pipInstall(['pandas', 'numpy', 'requests']) // Python 패키지
  .npmInstall(['axios']);                      // Node.js 패키지

// build.prod.ts
import "dotenv/config";
import { Template, defaultBuildLogger } from 'e2b';
import { template } from './template';

async function main() {
  await Template.build(template, 'custom-packages', {  // 'custom-packages'가 alias
    cpuCount: 2,
    memoryMB: 2048,
    onBuildLogs: defaultBuildLogger(),
  });
}

main().catch(console.error);

// npx tsx build.prod.ts
```

```jsx
import { Sandbox } from 'e2b'

const sbx = await Sandbox.create("custom-packages")
```

## 지원

Langchain 통합

- https://docs.langchain.com/oss/python/integrations/providers/e2b
    - https://e2b.dev/docs
- node 지원
    - https://www.npmjs.com/package/e2b

## Reference

- https://github.com/e2b-dev/e2b
- https://e2b.dev/dashboard/s-default-team-867b/sandboxes/monitoring (e2b console)
- https://e2b.dev/docs
- https://thekkom.tistory.com/562