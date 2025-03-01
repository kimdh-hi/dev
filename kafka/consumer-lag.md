## consumer lag

- consumer lag: producer 와 consumer 의 데이터 처리량의 차이
- producer 는 항상 partition 내 가장 최신의 offset 에 데이터를 저장하지만, consumer 자신이 처리할 수 있는 만큼의 데이터를 가져가기 때문에 offset 이 보통 더 작다.
- producer 가 데이터를 저장한 offset(`log-end-offset`) 과 consumer 가 가리키고 있는 offset (`consumer-current-offset`) 의 차이를 consumer lag 이라 한다.
- consumer lag 은 consumer application 이 정상동작하는지를 모니터링 할 수 있는 지표이다.

```
producer offset 5에 데이터 저장 (log-end-offset=5)
consuemr offset 2 데이터의 데이터 처리 (current-offset=2)

consumer lag=3 (5-2)

record 3개 지연 발생
```

consumer lag monitoring
- 특정 시점에 트래픽이 몰리는 서비스를 개발중
- 평상시에는 consumer lag 이 1~10 수준으로 적은 지연
- 트래픽이 몰리는 시점이 되어 consumer lag 이 늘어나는 것이 관측되는 경우 partition 을 늘리고, consumer 를 늘려서 producer 의 처리량을 따라갈 수 있도록 조치할 수 있다.

