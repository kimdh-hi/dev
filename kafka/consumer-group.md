Consumer Group

- 각 consumer group 은 다른 consumer group 으로부터 격리된 환경에서 운영됨
- consumer group 내 consumer 는 한 topic 의 여러 partition 이 할당될 수 있음
- topic 내 partition 수와 consumer group 내 consumer 수를 동일하게 하는 경우 partition 과 consumer 가 1:1로 매칭되어 최대의 성능을 낼 수 있으므로, 일반적으로 partition 수와 consumer group 내 consumer 수는 동일하게 한다.
- partition 수보다 consumer 수가 많은 경우 불필요한 유휴 상태 consumer 가 생기므로 비효율적임

why cosumer group
- a, b, c 각 다른 서버가 운영되는 경우
- a 에서 b, c 로 데이터 동기화를 위해 특정 topic 에 메세지 발행
- b, c 에 각각 다른 consumer group 을 할당하는 경우 topic 의 메세지를 격리되어 처리 가능
- 예로, b 서버 장애로 데이터 동기화 메세지 처리를 못한 경우 c 는 독립적으로 처리 가능, 이후 b 서버가 복구되는 경우 마지막 소비한 메세지로부터 이후 메세지들을 처리 가능 (결과적으로 데이터 동기화 가능)

