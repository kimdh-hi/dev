# NAT

## NAT (Network Address Translation)

- IPv4 주소 부족 이슈로 사설ip 구역 지정
    - 사설ip: 10.x.x.x, 172.16~31.x.x, 192.168.x.x
- 사설 ip를 외부 공인망고 매핑시키기 위해 NAT 등장
    - 사설 ip 를 변환하여 공인 ip 와 매핑
    - 사설 ip 영역과 공인ip 영역 사이 패킷을 중계

## 기본 동작 원리

- NAPT 의 원리
- 192.168.0.10:5000 --> 1.1.1.1:80 으로 패킷을 내보내는 경우
    - NAT 는 출발지를 공인주소와 새 포트로 바꿔 내보내고 (203.0.113.1:6001) 이를 테이블에 저장
    - 응답이 203.0.113.1:6001 로 돌아오는 경우 테이블로부터 대응되는 사설 ip/port 조회(192.168.0.10:5000)하여 패킷을 전달
- 응답에는 사설 ip 에 대한 정보가 없으므로 응답 패킷을 본래 요청 출발지로 중계하기 위해 새로운 포트를 key 값으로 사용하는 개념

## NAT 종류

- Basic NAT
    - 공인IP, 사설IP 1:1 매핑
    - IP 만 변환하므로 공인 IP 응답이 왔을 때 IP 만으로 응답을 돌려줄 출발지가 식별 가능하므로 여러 개 사설IP 를 매핑시키는 것이 불가
    - 단, 정적 Basic NAT, 동적 Basic NAT 개념이 있어서 결과적으로 1:1 매핑은 맞지만 사설IP 를 각 기기가 동적으로 돌려쓸 수 있었음.
- NAPT
    - IP 와 전송 계층 포트(TCP/UDP) 까지 변환
    - 여러 내부 호스트가 한 개 공인 IP 를 포트로 구분해서 공유하는 형태
    - 현재 NAT 는 NAPT 를 지칭

## Reference

- https://ko.wikipedia.org/wiki/%EB%84%A4%ED%8A%B8%EC%9B%8C%ED%81%AC_%EC%A3%BC%EC%86%8C_%EB%B3%80%ED%99%98
- https://www.networkworld.com/article/965192/what-is-ipv6-and-why-aren-t-we-there-yet.html