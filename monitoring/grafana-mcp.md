# Grafana MCP

## Grafana MCP?

- Grafana MCP 는 ai assistance, llm client 등이 grafana 인스턴스에 접근하여 매트릭, 로그, 대시보드 검색 및 관리, 알림 규칙 관리 등을 가능하게 한다.
- Grafana 는 두 개 종류의 MCP 서버 제공
    - https://grafana.com/docs/grafana-cloud/ai-tools/mcp-servers/
    - OSS MCP 서버 (자체 호스팅하는 MCP 서버)
    - Cloud MCP 서버

## Grafana MCP 서버

- Cloud mcp 서버
    - 서버 실행 주체: Grafana Labs 가 호스팅
    - `https://mcp.grafana.com/mcp` 통해 접속
    - 인증: OAuth 2.1
    - Grafana Cloud 전용 (self-hosted grafana 지원안함)
- OSS MCP 서버 (mcp-grafana)
    - go로 작성
    - claude --MCP--> mcp-grafana(mcpserver) --HTTP API--> Grafana --> Loki/Tempo/Mimir(Prometheus)
    - grafana 9.0 이상 필요 (https://grafana.com/docs/grafana/latest/developer-resources/mcp/troubleshooting/grafana-version-compatibility/)
    - 인증: Grafana UI Administration -> Users and access -> Service accounts 서비스 계정 생성 후 토큰(glsa_로 시작) 발급
    - Grafana Cloud, Self-hosted Grafana 모두 사용 가능

```
# <https://grafana.com/docs/grafana-cloud/ai-tools/mcp-servers/oss-mcp/set-up/install-with-uvx/>
{
  "mcpServers": {
    "grafana": {
      "command": "uvx",
      "args": ["mcp-grafana"],
      "env": {
        "GRAFANA_URL": "<http://localhost:3000>",
        "GRAFANA_SERVICE_ACCOUNT_TOKEN": "glsa_xxx"
      }
    }
  }
}
```

## Grafana MCP tools

- https://grafana.com/docs/grafana/latest/developer-resources/mcp/reference/mcp-tools-table/

## reference

- https://github.com/grafana/mcp-grafana