### LocalStorage, SessionStorage

브라우자 내에 키-값 형태로 값을 저장

Cookie 와 다른 점
- Cookie와 달리 매 요청에 값을 포함하지 않는다.
- Cookie 보다 큰 용량의 데이터를 저장 할 수 있다. (최소 2mb 이상, up to 5mb?)
  - Cookie 의 경우 최대 70개, Cookie 당 4,096 bytes 로 제한된다.
- Cookie 는 요청 헤더를 조작해서 서버로 전달되는 Cookie 값을 변조할 수 있지만 LocalStorage, SessionStorage 는 불가능하다.


LocalSotrage
- Origin 이 같은 경우 `모든 탭에서 데이터가 공유`된다.
  - Origin (= Protocol, Host, Port)
- `브라우저 재시작, OS 재시작 시`에도 `데이터가 유지`된다.

SessionStorge
- 해당 탭에서만 유효하다. (탭 간 데이터를 공유하지 않는다.)
- 하나의 탭에서 iframe 으로 다른 웹페이지가 있는 경우에는 공유된다.
- 새로고침 시에는 데이터가 유지되지만, 탭을 닫고 새로 열 때는 데이터가 사라진다.