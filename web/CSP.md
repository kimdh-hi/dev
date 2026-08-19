# CSP

## CSP (Content Security Policy)

- CSP 는 서버가 브라우저에게 어떤 리소스를 어떤 출처로부터 로드할 수 있는가를 지시하는 선언적 보안 메커니즘
- 브라우저는 서버가 응답한 `Content-Security-Policy` 헤더에 명시된 정책에 따라 여러 지점에서 CSP 정책 기반으로 검사 진행
    - `<meta http-equiv>` meta 테그 통해 지정가능하지만 일부 기능 지원x
    - 지시어은 세미콜론으로 구분
- `Content-Security-Policy` 헤더 적용 전 운영 부담이 있다면
- `Content-Security-Policy-Report-Only` 헤더 통해 테스트 권장
- 단, sandbox의 경우 Content-Security-Policy-Report-Only 지원 x
- json 응답 위주의 일반적인 api 백엔드 서버의 경우 직접적인 사용처 적음
    - 브라우저가 서버의 응답으로 문서(dom)를 만드는 경우에 해당
    - 단, api 문서(swagger-ui) 등 html 서빙하는 경우 해당
- api 서버의 경우 파일 다운로드와 같은 기능을 제공하는 경우 html 등의 파일을 inline 으로 응답하는 경우 보안 이슈 발생 가능
    - CSP `default-src 'none'` 설정
    - 인라인 <script>...</script>, 외부 스크립트 로드, 인라인 이벤트 핸들러, fetch/XHR 등등 차단

```
HTTP/1.1 200 OK
Content-Type: text/html
Content-Security-Policy: default-src 'self'; script-src 'self' <https://cdn.example.com>
```

## 지시어

| 지시어 | 역할 | 권장값 |
| --- | --- | --- |
| `default-src` | 대부분 fetch 지시어의 폴백 | `'self'` |
| `script-src` | 스크립트 실행 통제 (핵심) | nonce + `'strict-dynamic'` |
| `connect-src` | fetch/XHR/WebSocket 대상 | `'self' <https://api.내도메인`> |
| `object-src` | 레거시 플러그인 | `'none'` |
| `base-uri` | `<base>` 주입 차단 | `'none'` |
| `form-action` | 폼 제출 대상 | `'self'` |
| `frame-ancestors` | clickjacking 방어 (X-Frame-Options 대체) | `'none'` 또는 허용 출처 |
| `upgrade-insecure-requests` | http 서브리소스 자동 https 승격 | 값 없이 선언 |
| `report-to` / `report-uri` | 위반 보고 | 엔드포인트 지정 |

## sandbox

- sandbox 는 문서로부터 전체 권한을 빼앗고, 필요한 것만 되돌려주는 방식
- 별도 값없이 sandbox 만 지정시 최대 제한
    - 스크립트, 팝업, 플러그인 등 차단, same-origin 정책 강제 (origin 격리)
- `allow-same-origin` 키워드가 없는 sandbox 는 origin이 null 이므로 same-origin 정책 검사에 항상 실패하여 cookie 등 접근 불가
- 필요한 것만 `allow-xx` 키워드로 명시
    - allow-scripts
    - allow-same-origin
    - allow-forms
    - ...

```
Content-Security-Policy: sandbox
```

## CSP 검사 지점

### 네트워크 요청 전 (외부 리소스 요청 전)

- <script src>, <img src>, fetch(), WebSocket 연결 ...
- 대상 URL이 지시어의 소스 목록과 일치하는지 검사 (일치하지 않는 경우 요청 자체를 보내지 않고 차단)
- script-src-elem → script-src → default-src 순으로 조회

### 코드 실행 전

```
<script>doSomething()</script>              <!-- 인라인 스크립트 -->
<button onclick="doSomething()">            <!-- 이벤트 핸들러 속성 -->
<a href="javascript:doSomething()">         <!-- javascript: URL -->
<div style="color:red">                     <!-- 인라인 스타일 속성 -->
<script>eval(userInput)</script>            <!-- 문자열 → 코드 -->
```

## 백엔드 개발자 관점 CSP 고려사항

```
이슈 케이스
server: aaa.com
client: bbb.com

1. client 는 server 로 부터 인증 완료
- jwt 를 cookie, localStorage 등에 저장

2. client 로부터 악의적인 html 업로드 이후 다운로드
- 다운로드 된 파일의 origin 은 aaa.com

3. bbb.com 에서 origin 이 aaa.com 인 문서가 로드
- CORS 무력화 (origin 이 aaa.com 임)
- cookie httpOnly, sameSite 무력화 (읽지 않고 자동으로 전송됨)
```

- 사용자가 업로드 한 파일을 브라우저가 "웹페이지" 로 해석시 보안 이슈 발생 가능
- 다운로드 시 응답헤더 설정 추가

| 헤더 | 설명 |
| --- | --- |
| `Content-Disposition: attachment` | inline으로 띄우지 말고 파일로 저장할 것 |
| `X-Content-Type-Options: nosniff` | 서버에서 응답한 타입을 그대로 시용할 것. 추측x |
| `default-src 'none'` | fetch/XHR 등 다운로드 된 문서 내에서는 어떤 것도 하지 말 것 |
| `sandbox` | origin 정보를 비워 same-origin 제약에 걸려 same-origin 에 따른 허용작업 금지 |
- presignedURL 적용을 통한 개선
    - 우리 서버의 origin과 분리되므로 세션 쿠키, dom 등 접근위험 낮아짐
    - 단, 스토리지 서비스 내부 위험 사항에 대한 것은 별도 검토 (response-content-disposition, response-content-type)

## reference

- https://developer.mozilla.org/en-US/docs/Web/HTTP/Guides/CSP
- https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Content-Security-Policy/sandbox
- claude