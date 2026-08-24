# Hexagonal Architecture

## 헥사고날 아키텍처 (Ports & Adapters 패턴)

- 2005/9/4 발표 Alistair Cockburn
    - https://alistair.cockburn.us/hexagonal-architecture
- 애플리케이션 경계에서 비즈니스 로직을 외부 기술로부터 격리하기 위함
    - 외부 기술로부터 비즈니스 로직을 격리하여 테스트 하기 쉬운 구조를 만든다.
    - 외부 기술이 교체되어도 비즈니스 로직에 영향이 없도록 한다.
- 인바운드 어댑터/포트, 코어, 아웃바운드 어댑터/포트 로 구성
- 본래 헥사고날 아키텍처는 웹을 겨냥해 만들어진 것은 아님
    - https://reflectoring.io/spring-hexagonal/
    - 2019/5 Clean Architecture with Spring발푤에서 톰 홈버그가 spring 진영에서 헥사고날 아키텍처 적용 방안을 소개했고 그 구조가 표준처럼 퍼짐
        - adapter 이하 in.web, out.persistence
        - application 이하 port.in, port.out
    - https://reflectoring.io/spring-hexagonal/

```
인바운드 어댑터가 인바운드 포트를 통해 코어를 호출하고, 코어는 도메인 로직 도중 필요한 것을 아웃바운드 포트로 요청하고, 아웃바운드 어댑터가 받아 실제 구현체 통해 처리

- [바깥쪽] 인바운드 어댑터(=Driving/Primiary adapter): usecase를 호출하고 응답을 내보내는 쪽 (controller, batch, cli ...)
- [안쪽] 인바운드 포트: 코어가 제공하는 계약 (코어가 제공하는 api 명세)
- [안쪽] 아웃바운드 포트: 코어가 요구하는 계약 (코어가 필요로하는 db, 외부 api, 메세징 서비스 등에 대한 명세)
- [바깥쪽] 아웃바운드 어댑터(=Driven/Secondary adapter): 아웃바운드 포트 구현

의존성은 항상 바깥쪽에서 안쪽으로만 향한다.
```

- 인바운드 어댑터는 인바운드 포트를 통해 코어에 접근하는 모든 주체가 될 수 있다.
    - controller 뿐만 아니라 스케줄러, kafka consumer, batch 등등 모두 인바운드 어댑터(드라이빙 어댑터가 될 수 있다)

인바운드 어댑터(controller) -> 인바운드 포트(xxUsecase) -> 코어(도메인 로직, xxService) -> 아웃바운드 포트(xxRepository) -> 아웃바운드 어댑터(xxJpaRepository)

## 장점

- 기술 스택이 변경되어도 코어 비즈니스 로직은 변하지 않는다.
    - 로컬 캐시 -> 레디스 전환 등 인프라 레이어 기술 스택 변경되어도 어댑터에서 아웃바운드 포트만 제대로 구현한다면 비즈니스 로직에 영향 없음
- 인바운드 포트로 코어의 계약을 공개하면 실제 가져다 쓰는 인바운드 어댑터들 (controller, kafka, batch, cli...) 에서 동일한 로직 변경없이 사용 가능
- 구현 기술 결정을 미룰수 있음.
    - kafka? rabbitmq? 등 고민이 필요할 때 아웃바운드 포트만 정의하면 기술 결정은 뒤로 미룰 수 있음

## 적용시 고민사항

- JPA 사용하는 경우 POJO 도메인과 JPA Entity 를 분리할 것인가?
    - 애플리케이션 코어(도메인 계층 + 어플리케이션 계층) 에 POJO 도메인을 두고, jpa entity 를 별도 아웃바운드 어댑터로 둘지 고민 필요
    - JPA 에는 구현 기술이므로 코어는 POJO 도메인에만 의존하고, JPA Entity 는 아웃바운드 어댑터에 위치시켜서 코어로부터 JPA 의존성을 완전히 격리시켜야 함.
    - 격리시키는 경우 수많은 불편함이 유발됨
    - 격리시 JPA entity 내에는 비즈니스 로직이 없을 것임. 조금만 복잡해지면 어댑터에서 entity 대상으로 도메인 로직도 없이 손수 db 로직을 구현해야 함
    - 필드 하나 추가하면 도메인, jpa entity, toDomain, toEntity, dto... 수정 필요
- @Transactional 도 못 붙이나? (spring 꺼)
    - 이러지 말자..
- usecase 인터페이스
    - 팀 내 컨벤션에 따르되, 굳이 만들어야 하는 이유가 있을지 한번쯤은 점검하자.
- 초기 도입시 Archunit 과 같은 아키텍처 검증툴을 통해 의존방향을 검증하자.

=> 얼마나 순수하게 아키텍처를 지킬지에 대한 고민 및 팀 내 협의 필요

## reference

- https://blog.allegro.tech/2020/05/hexagonal-architecture-by-example.html
- https://alistair.cockburn.us/hexagonal-architecture
- https://tech.kakaopay.com/post/home-hexagonal-architecture/
- https://devblog.kakaostyle.com/ko/2025-03-21-1-domain-driven-hexagonal-architecture-by-example/