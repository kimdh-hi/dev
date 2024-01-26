Load Banacing

L4 Load Balancing
- 4계층에 해당하는 전송계층 (TCP, UDP) 에서 IP, Port 를 활용해서 서버로 부하분산을 담당한다.
- 로드밸런싱 대상은 IP, Port 만 해당하고 TCP, UDP 헤더를 분석하여 로드밸런싱에 사용되지는 않는다.
  - 즉, 로드밸런싱 대상 패킷의 내부를 분석할 수 없기 때문에 보다 섬세한 부하분산은 어렵다.
  - 반대로, 단순한만큼 빠르고 L7 로드벨런서에 비해 저렴하다고 한다..

```
Virtual IP (VIP)

- 특정 서버그룹의 대표 IP 로 보통 L4 로드밸런서 (L4 스위치) 에 할당된다.
- 외부망에서는 L4 로드밸런서의 공인 IP 로 접근하고 해당 요청을 L4 로드밸런서 이하 사설 IP 가 할당된 내부망 서버들로 요청을 부하분산한다.
```