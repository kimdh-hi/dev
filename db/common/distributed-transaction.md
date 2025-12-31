## 분산 트랜잭션

## 2PC (two-phase commit)
- commit 을 prepare 와 commit 두 단계로 나누어 분산 트랜잭션에서 원자성을 보장
- coordinator(Transaction Manager) 와 participants(Resource Manager) 로 구성

```
flow
1. prepare
- coordinator 는 participants 에게 커밋 준비 여부 질의
- participants 는 커밋 가능 상태임을 보고

2. commit/rollback
- 모든 participants 가 준비된 경우 commit
- 한개 participants 라도 준비가 되지 않은 경우 roolback
```

성능, coordinator의 단일 장애 지점, 2PC(XA) 지원여부(NoSQL 등 지원이 안 될수도) 등의 문제도 3PC, TCC 등 다른 방안이 권장된다.

구현의 경우 X/Open 표준인 XA(eXtended Architecture) 가 사용되며, Java 진영에서는 JTA(Java Transaction API) 를 통해 XA 프로토콜을 구현하고 사용할 수 있도록 제공한다.

## TCC (Try-Confirm/Cancel)
- 데이터 정합성, 원자성을 보장하는 보상형 트랜잭션 관리 패턴
- 타 서비스 데이터 CUD 필요시 reserve -> confirm/cancel 과정 거쳐 일관성 보장
- reserve 단계에서 실제 리소스를 예약 (차감/삭제 등)하는 방식이므로 즉시 실행 후 보상하는 saga 보다 강한 일관성을 보장

```kotlin
// order -> product/point 주문 code

fun reserveOrder() {
  try {
    val placeOrderResult = reserveOrderPlace()
    val orderId = placeOrderResult.id

    reserveProductDecreaseApiRequest(orderId, placeOrderResult.productId, placeOrderResult.quantity)
    reservePointApiRequest(orderId, placeOrderResult.amount)

    confirmOrder(orderId)
  } catch (e: Exception) {
    val placeOrderResult = cancelOrderPlace()
    cancelProductDecreaseApiRequest(placeOrderResult.orderId)
    cancelPointApiRequest(placeOrderResult.orderId)
  }
}

// reserve status --> confirm status
fun confirmOrder(orderId: String) {
  confirmOrderPlace(orderId)
  confirmProductDecreaseApiRequest(orderId)
  confirmPointApiRequest(orderId)
}
```

## Saga
- 보상 트랜잭션(보상 이벤트) 결과적 일관성을 보장하는 트랜잭션 패턴
- 구현 방식: Orchestration, Choreography

### Orchestration
- coordinator(orchestrator) 가 각 참여 서비스를 순차적으로 호출하며 전체 트랜잭션의 흐름을 제어하는 방식
  - ex) order 서비스에서 주문 도중 실패한 경우 product, point 에 대한 보상 처리 수행
- 장점: 구현 난이도 및 유지보수 난이도 낮음
- coordinator 측 코드가 복잡해질 수 있음
- 서비스 간 결합도 증가 (coordinator 가 중간에서 여러 서비스를 제어하기 때문)

### Choreography
- 각 서비스는 이벤트 기반으로 타 서비스의 트랜잭션을 구동
  - ex) 각 서비스는 본인의 트랜잭션만 관리하고, 실패시 본인에 대한 보상 이벤트를 발행
- 보상 트래잭션 또한 동일하게 이벤트를 발행하여 처리
- 장점: 이벤트 기반이고, 한 개 coordinator 에 의해 제어되지 않으므로 서비스 간 결합도 낮음
- 단점: 구현 난이도가 높고, 명시적으로 coordinator 가 없으므로 로직을 한눈에 파악하기 어려움이


---

## reference

2PC
- https://en.wikipedia.org/wiki/X/Open_XA
- https://docs.spring.io/spring-boot/reference/io/jta.html

TCC
- https://docs.oracle.com/en/database/oracle/transaction-manager-for-microservices/24.2/tmmdg/tcc-transaction-model.html

Saga
- https://www.inflearn.com/course/%EC%A3%BC%EB%AC%B8%EC%8B%9C%EC%8A%A4%ED%85%9C%EC%9C%BC%EB%A1%9C-%EC%95%8C%EC%95%84%EB%B3%B4%EB%8A%94-%EB%B6%84%EC%82%B0%ED%8A%B8%EB%9E%9C%EC%9E%AD%EC%85%98/dashboard