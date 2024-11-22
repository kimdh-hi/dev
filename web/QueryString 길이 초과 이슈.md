QueryString 길이 초과 이슈
- Request header is too large

- query parameter 길이가 길어지는 경우 발생 가능
- default: 8kb

sol1
- http request header size  조정
  - `server.max-http-request-header-size: 40KB`

sol2
- get → post 요청