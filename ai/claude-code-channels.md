## claude code channels

- channel 는 실행중인 claude code 세션 안으로 외부 이벤트를 push 하는 기능
- channel 은 mcp 서버
    - claude code 는 channel mcp 서버와 stdin/stdout 방식으로 통신
- 일반 mcp 서버는 claude 가 질의할 때만 응답 (pull 방식)
    - claude 가 필요로 하는 tool 이 있는지를 먼저 질의하고 있다면 실행시키는 방식
- channel 은 서버가 먼저 세션으로 이벤트를 넣는다. (push 방식)
    - claude 가 mcp 서버 프로세스로 별도 질의하지 않고도 특정 내용을 프롬프트에 삽입 가능

동작 원리

- channel 은 claude code 와 동일 머신에서 도는 mcp 서버이다.
- claude code 가 channel mcp 서버를 서브프로세스로 띄우고 stdio 통해 통신
    - claude code `.mcp.json` 에 mcpServer 로 명시

```
capabilities: { experimental: { 'claude/channel': {} } }
```

- `'claude/channel': {}` 를 capabilities 로 선언한 mcp 서버를 claude code 가 서브프로세스로 띄우는 경우 claude 측에서 capabilities 확인 후 리스너로 등록

```
mcp.notification()
```

- 외부 이벤트를 수신 후 claude 로 보낼 문자열을 notification 통해 전송시 claude 는 새로운 턴의 대화 컨텍스트에 포함
- `instructions`: notification 통해 claude 로 전송한 메세지를 통해 새로운 턴의 대화 시작시 주입될 시스템 메세지
    - `instructions` 가 없는 경우 외부 이벤트에서 들어온 메세지를 대화 컨텐스트에 포함되므로 어떤 식으로 답장할지 명시가 필요하는 등이 필요한 경우 지정

```javascript
// 디스코드 봇, claude code channel 기반 통신 Pseudo-code

// mcp 서버 생성
const mcp = new Server(
  { name: 'discord', version: '0.0.1' },
  {
    capabilities: {
      experimental: { 'claude/channel': {} },
      tools: {},
    },
    instructions: 'instruction message...',
  },
)

// tool list up
mcp.setRequestHandler(ListToolsRequestSchema, async () => ({
  tools: [{
    name: 'reply',
    description: 'Send a message back to Discord',
    inputSchema: {
      type: 'object',
      properties: { channel_id: { type: 'string' }, text: { type: 'string' } },
      required: ['channel_id', 'text'],
    },
  }],
}))

// Claude → Discord
mcp.setRequestHandler(CallToolRequestSchema, async (req) => {
  const { channel_id, text } = req.params.arguments as { channel_id: string; text: string }
  discord.send(channel_id, text)
  return { content: [{ type: 'text', text: 'sent' }] }
})

// Claude Code 와 stdio 로 연결
await mcp.connect(new StdioServerTransport())

// Discord -> Claude noti
discord.on('message', async (msg) => {
  await mcp.notification({
    method: 'notifications/claude/channel',
    params: {
      content: msg.text,
      meta: { channel_id: msg.channelId },
    },
  })
})
```

## reference
- https://code.claude.com/docs/en/channels
- https://code.claude.com/docs/en/channels-reference
- claude