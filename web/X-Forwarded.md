## X-Forwarded

Forwarded
- Forwarded: for=1.1.1.1; host=www.aaa.com; proto=https;
- 표준헤더

---

X-Forwarded-For (XFF)
- 웹서버로 요청 전 앞 단의 Load Balancer, Proxy 등을 거치는 경우 Client 의 요청 IP 는 LB, Proxy 의 IP 가 된다.
- 웹서버 단에서 Client 의 IP 를 식별하는데 사용되는 사실상의? 표준 헤더이다.
- `X-Forwarded-For` 에는 Client IP 뿐만 아니라 중간에 거친 Proxy 등 중간서버의 ip로 기록된다.
  - `X-Forwarded-For: <client>, <proxy1>, <proxy2>`

---

X-Forwarded-Proto
- Client 가 Proxy, LB 에 접근시 사용한 프로토콜(HTTP, HTTPS...) 가 무엇인지 확인 가능한 헤더이다.
- cleint -> proxy -> web server
  - 위 구조에서 proxy 단은 X-Forwarded-Proto 헤더를 추가한다. (client 의 요청 프로토콜)
- `X-Forwarded-Proto: https`

---

X-Forwarded-Host
- Cleint 의 Host 를 식별하는데 사용된다.
- `X-Forwarded-Host: https://www.aaa.com`


---

참고
- https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/X-Forwarded-For
- https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/Forwarded


