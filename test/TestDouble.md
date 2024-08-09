Test Double

Test Double 종류
- Dummy
- Fake
- Stub
- Spy
- Mock

Dummy
- 객체를 필요로하지만 해당 객체의 기능은 필요하지 않는 경우 사용
- Dummy 객체의 메서드 호출시 정상동작은 보장되지 않는다.
- 테스트 시 주로 객체를 파라미터로 전달할 때 사용한다.

```kotlin
interface Printer {
  fun print(text: String)
}

// 정상동작 보장 x
class DummyPrinter: Printer {
  override fun print(text: String) {
    TODO("Not yet implemented")
  }
}
```

--- 

https://tecoble.techcourse.co.kr/post/2020-09-19-what-is-test-double/