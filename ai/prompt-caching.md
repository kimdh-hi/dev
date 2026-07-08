# prompt cahing

## prompt caching

- 프롬프트 캐싱은 LLM 모델의 답변을 저장하는 것이 아님
    - 캐싱은 출력 토큰 생성, 최종 응답에 영향을 주지 않고, 캐싱 여부외 무관하게 동일하게 출력
    - 프롬프트만 캐싱되고 실제 응답은 매번 새로 계산
- LLM 추론은 두 단계로 나뉨
    - pre-fill (prompt cache 가 write/hit 되는 영역)
        - 입력 프롬프트를 읽는 과정
        - 입력: "대한민국의 수도는" --> "대한민국", "의", "수도", "는" 토큰화
        - 토큰화된 결과를 동시에 신경망에 넣어서 계산 ==> 고비용 작업
        - "수도" 라는 토큰이 물이 흐르는 "수도" 일지 나라의 "수도" 일지를 판단해야 하므로 신경망은 앞 맥락을 참고
        - 결과적으로 pre-fill 계산 결과를 K.V 캐시에 저장 (cache write)
        - 이후 "대한민국" 이라는 입력 토큰에 대해서는 본인 K.V 계산은 수행하지 않고, 다음 KV 계산의 재료로만 사용됨.
    - decoding
        - 출력 토큰을 하나씩 재귀적으로 생성
- KV 캐시는 디스크가 아닌 메모리에 상주 (ttl 후 소멸)
- prompt caching 앞 입력 토큰부터 순서대로 캐시 hit 처리하고, 캐시 miss 발생 이후 입력토큰부터는 KV 계산 수행
    - docker layer cache 와 유사한 개념
    - "대한민국", "의", "수도", "는" 이 앞서 입력토큰으로 처리되어 캐싱된 경우
    - 이후 "대한민국", "의" "날씨", "는" 이 입력토큰으로 들어오는 경우 "대한민국", "의" 까지만 캐싱처리되고 "날씨", "는" 은 K.V 계산
        - 각 llm provider 별 캐싱 처리할 최소 토큰수가 있기 때문에 위 예제처럼 동작하지는 않음.
- 입력 프롬프트를 tools → system → messages 순서로 배치하는 것이 캐시 히트에 유리 (각 provider api 가 해당 순서로 배치)
    - docker image layer cache 와 유사한 개념이므로 동일하게 잘 변하지 않는 부분부터 앞에 배치하는게 유리하다.

```
openai, anthotipic, gemini 등 기본적인 prompt caching 구조는 동일하지만
캐시 적용 방법(암시적, 명시적), 캐시 격리 단위, 캐시 히트율 향상을 위한 라우팅 룰 등이 provider 별로 차이가 있음
```

## 비용

- 캐시 히트되는 경우 각 토큰에 대해 K.V 계산이 연산에 사용되는 시간비용은 축소
- 입력토큰에 대한 비용(소비토큰 당 단가) 또한 캐시 히트의 경우 최대 90% 할인 (provider 별로 다름. anthotipic 기준)
    - 단, 캐시 사용하는경우 cache write 시 1.25 배의 토큰비용이 소비 (provider 별로 다름. anthotipic 기준)

## cache_control (anthropic)

- 명시적으로 프롬프트 캐싱을 제어
- [tools][system][messages] 순 이어진 토큰의 특정 블록 꼬리에 부착
- cache-control 부착한 블록까지를 캐싱 대상으로 판단.
- 의도적으로 자주 변하는 messages 뒤에 cache_control 부착하지 않고 system 영역 까지만을 캐싱 영역으로 해서 cache write 비용 최적화 가능
- ttl 지정 가능 (default: 5m)
    - ttl 이 커질수록 입력 토큰 비용이 높아진다. (쓰기 비용)
    - By default, automatic caching uses a 5-minute TTL. You can specify a 1-hour TTL at 2x the base input token price:

```jsx
const client = new Anthropic();

const response = await client.messages.create({
  model: "claude-opus-4-8",
  max_tokens: 1024,
  cache_control: { type: "ephemeral", "ttl": "1h" },
  system:
    "You are an AI assistant tasked with analyzing literary works. Your goal is to provide insightful commentary on themes, characters, and writing style.",
  messages: [
    {
      role: "user",
      content: "Analyze the major themes in 'Pride and Prejudice'."
    }
  ]
});
console.log(response.usage);
```

## LiteLLM 지원

- https://docs.litellm.ai/docs/completion/prompt_caching
- `cache_control` 직접 포함하는 경우 각 provider 형식으로 변환 (Bedrock⇒cachePoint, google⇒context caching..)
- `cache_control_injection_points`
    - https://docs.litellm.ai/docs/tutorials/prompt_caching
    - config.yaml

```yaml
cache_control_injection_points:
  - location: message   # [tools][message] 와 같은 배열중 message 영역을 대상으로. (system, user, assistant prompt 포함 개념)
    role: system 
```

```yaml
- location: message
  index: -1   # 배열의 마지막 메시지에 주입 (전체를 캐시 write/read 대상으로)
```

- 캐시 적용시 쓰기 프리미엄, 읽기 할인이 포함된 비용 계산 가능
- 응답에 `cache_creation_input_tokens`(쓰기 발생량), `cache_read_input_tokens`(히트량) 포함하여 캐시 히트관련 디버깅 가능

## reference

openai

- https://developers.openai.com/api/docs/guides/prompt-caching
- https://developers.openai.com/cookbook/examples/prompt_caching_201

anthropic

- https://platform.claude.com/docs/en/build-with-claude/prompt-caching

gemini

- https://docs.cloud.google.com/gemini-enterprise-agent-platform/models/partner-models/claude/prompt-caching?hl=ko

LiteLLM

- https://docs.litellm.ai/docs/completion/prompt_caching
- https://docs.litellm.ai/docs/tutorials/prompt_caching