# HMAC

## HMAC (Hash-based Message Authentication Code)

- HMAC 은 암호학적 해시 함수와 공유 비밀키를 결합해 메세지 인증 코드를 만드는 방식
- "이 메세지가 중간에 바뀌지 않았고, 비밀키를 아는 사람이 만든 것이다" 를 증명하는 코드를 만드는 표준 방법
    - SHA-256 등 해시함수와 비밀키를 필요로 함
- A 서버가 B 서버로 특정 요청을 보내는 경우 해당 요청이 변조되었는지, A서버가 보낸것이 맞는지 검증 필요한 경우 사용
- 해시만 있는 경우
    - 공격자는 데이터 변조이후 다시 해시를 계산해서 붙이면 됨
    - 해시는 전송 채널 전과정이 이미 신뢰될 때 데이터가 깨졌는지를 확인 가능
- HMAC 은 부인방지 불가
    - A, B 서버는 동일한 비밀키를 가지고 있으므로 제3자에게 A가 보낸 것이라고 증명 불가 (B도 동일한 코드를 만들어낼 수 있음)

## HMAC 코드 생성 원리

- SHA-256 해시 함수에 인자로 비밀키와 메세지를 넣으면 고정된 길이의 결과가 반환됨
- 키 또는 메세지가 조금이라도 달라지는 경우 결과값은 크게 바뀌므로 `SHA-256(키 + 메세지)` 구조로 증명 가능
- 단, SHA-256의 특성상 `SHA-256(키 + 메세지)` 만으로는 부족함
    - sha-256 은 message 를 64바이트 블록 단위로 잘라서 해시값 계산
    - 결과적으로 반환되는 해시값은 계산이 끝난 결과물이 아닌 계산이 멈춘 지점의 해시값이 됨
    - 즉, `SHA-256("비밀키" + "test")=a3f9c2...` 인 경우 `SHA-256("a3f9c2..." + "123"` 처럼 뒤에 값을 추가해서 계산한 경우 비밀키를 몰라도 유효한 해시 생성 가능
    - 위처럼 비밀키를 몰라도 코드를 조작할 수 있으므로 `SHA-256(키 + 메세지)` 로는 부족

```kotlin
fun sha256(message: ByteArray): ByteArray {
    var state = 고정된초기값          // 32바이트
    
    for (block in message.blocks()) {  // 64바이트씩 잘라서
        state = f(state, block)        // state를 계속 갱신
    }
    
    return state                       // ← 그냥 state를 반환
}
```

- 비밀키를 서로 다른 두 상수와 xor 연산하여 두 개 키를 획득하고 안쪽, 바깥쪽 각각 겹처서 해시 적용

```kotlin
inner = hash((k0 xor 상수1) || 메세지))
code = hash((k0 xor 상수2) || inner))
```

- 공격자가 바깥쪽 해시 결과(code) 만 볼 수 있으므로 값 변조를 시도해도 실제 메세지를 담고 있는 inner 영역에 실질적인 영향을 끼칠 수 없음.
    - 상수1 (ipad) = 0x36 을 블록 크기만큼 반복
    - 상수2 (opad) = 0x5c 을 블록 크기만큼 반복

## timestamp

- HMAC 통해 생성된 코드는 중간에 메세지가 변조되었는지 비밀키를 공유하는 서버가 보낸것이 맞는지를 증명 가능
- 언제 만들어졌는지는 증명 불가
- 언제 만들어졌는지, 즉 코드의 신선도 증명을 위해 timestamp 를 포함
- hmac 통해 코드 생성시 인자로 timestamp 를 포함한다.
    - 당연히 hmac 코드 생성시 인자로 사용된 timestamp는  다른 메세지들과 함께 평문으로 수신측에 함께 전달되어야 함.
- 수신측은 동일 timestamp 로 hmac 코드 검증 이후 현재 시간 기준 timstamp 기반 조건 판단 가능
- 중간에 메세지가 탈취되어 제3자가 메세지를 반복 전송하거나 먼 미래에 요청시 문제가 되는 경우 timestamp 사용 가능
- 공격자는 비밀키를 알 수 없으므로 timestamp 를 조작해도 hmac 코드 생성 불가

```python
# 만들 때
ts = int(time.time())
code = hmac.new(key, f"{ts}.{body}".encode(), hashlib.sha256).hexdigest()
# 전송: ts + body + code

# 검증할 때
expected = hmac.new(key, f"{ts}.{body}".encode(), hashlib.sha256).hexdigest()
if not hmac.compare_digest(expected, code):
    return 'error'                              # 위조
if abs(time.time() - ts) > 300:
    return 'error'                              # 5분 지남 → 폐기
```

## Reference

- claude
- RFC 2104 — HMAC 원 규격. 이중 해시 구조와 ipad/opad
https://www.rfc-editor.org/rfc/rfc2104.html
- FIPS 198-1 — NIST 표준. K0 유도 규칙, 전체 계산 절차
https://nvlpubs.nist.gov/nistpubs/FIPS/NIST.FIPS.198-1.pdf
- RFC 4231 — 테스트 벡터 (직접 구현 시 검증용)
https://www.rfc-editor.org/rfc/rfc4231.html