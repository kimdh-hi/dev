# consumer group, partitions 수

- consumer group 은 같은 목적/기능의 consumer 집합
- topic 내 각 partition 은 consumer group 내 consumer 와 1:1 매핑
- 동일 consumer group 내에서는 1개 메세지는 1 번만 처리됨 (메세지 중복처리 방지)
- partition 수는 런타임에 줄일 수 없으므로 오토스케일링에 대비해서 산정해야 함

```
운영중 partition 수를 줄일수 없다는 것 떄문에 지나치게 많은 수의 partition 을 설정한 경우

- partition 은 메모리 수준에서 버퍼/메타데이터 등을 유지하기 때문에 메모리 낭비 발생
- rebalance 시 partition 재분배 시간 증가
- 디스크 i/o 효율 저하
...
```

| 관계 | 결과 | 권장 여부 |
| --- | --- | --- |
| Consumer = Partition | 1:1 매칭, 최대 병렬성 | ✅ 이상적 |
| Consumer < Partition | 일부 Consumer가 여러 Partition 담당 | 확장 여지 있음 |
| Consumer > Partition | 일부 Consumer는 idle | ❌ 비용 낭비 |

```
consumer 수와 partiton 수는 동일하게 하거나 배수로 설정하는게 가장 이상적 (균등분배, 병렬성)

각 partition 은
```

### msa 관점에서

- 각 마이크로서비스는 한 개 consumer group
- k8s 환경에서 각 마이크로서비스의 pod(=consumer) 는 늘어났다가 줄었다가를 반복할 수 있음
    - 각 마이크로서비스마다 pod(=consumer) 수도 다를 것임.
- partition 수는 모든 consumer group 중 가장 많은 consumer 가 필요한 시점을 기준으로 한다.
    - k8s 기준 HPA 값 기준으로 partition 수 산정 (=maxReplicas)