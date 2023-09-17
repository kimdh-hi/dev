## Cookie

특정 도메인의 클라이언트의 요청으로부터 서버가 지속적으로 받고자 하는 값을 저장
- key-value
- (서버) Set-Cookie -> (클라이언트) Cookie 요청 헤더

Cookie 등장배경
- 그냥 클라이언트 별로 필요한 데이터를 모두 서버에 저장하면 되는 게 아닌가?
- 이전 서버 장비가 저렴하지 않은 시절 수많은 클라이언트에 대한 정보를 저장하는 것은 쉬운 선택이 아니었다.
- Cookie는 각 클라이언트에게 저장장치의 부담을 전가할 수 있다.

유효시간 설정
- max-age, expires
- 둘 다 설정시 max-age가 우선순위 가진다.
- 특정 Cookie 만료처리
  - max-age=0

적용 범위
- 브라우저는 쿠키를 보낸 서버가 속한 도메인으로만 쿠키를 실어 요청한다.
- `Domain` 설정시 서버가 속한 도메인의 서브 도메인까지 쿠키 적용범위를 설정할 수 있다.
  - 서버 도메인: `test.com`
  - Domain 설정: `set-cookie: key1=value1; Domain=test.com`
  - 브라우저는 `a.test.com`, `b.test.com` 으로의 요청에도 모두 `key1=value` 의 Cookie 를 보낸다. (Cookie 공유)
- 적용범위 축소 (`Path`)
  - Path 설정: `set-cookie: key1=value1; Path=/test`
  - 서버가 속한 도메인 `test.com` 의 `test.com/test` 의 요청으로만 적용범위를 축소시킨다.


한계
- Cookie 는 브라우저 환경에서 누구든 확인할 수 있고 변조할 수 있다.
- 서버는 Cookie 값이 올바르게 전달될 것으로 믿고 사용할 수밖에 없다.

보안 설정 (Secure, HttpOnly)
- `Set-Cookie: <쿠키 이름>=<쿠키 값>; Secure`
	- `Secure`가 설정된 Cookie 를 가진 브라우저는 https 프로토콜 상에서만 서버로 Cookie 를 전달한다.
- `Set-Cookie: <쿠키 이름>=<쿠키 값>; HttpOnly`
  - `HttpOnly` 가 설정된 Cookie 는 js 상에서 Cookie 에 직접 접근하는 것을 방지한다. (`Document.cookie`)




---

### ref
- https://www.daleseo.com/http-cookies/
- https://www.daleseo.com/http-session/