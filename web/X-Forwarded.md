## X-Forwarded

X-Forwarded-For (XFF)
- 웹서버로 요청 전 앞 단의 Load Balancer, Proxy 등을 거치는 경우 Client 의 요청 IP 는 LB, Proxy 의 IP 가 된다.
- 웹서버 단에서 Client 의 IP 를 식별하는데 사용되는 사실상의? 표준 헤더이다.
- 표준헤더는 `Forwarded` 이지만 실질적인 표준은 XFF, XFH 라고 생각하면 된다.


---

참고
- https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/X-Forwarded-For
- https://developer.mozilla.org/ko/docs/Web/HTTP/Headers/Forwarded


