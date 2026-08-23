# MySQL InnoDB

## Storage engine

- MySQL 은 `SQL 레이어`(파서->옵티마이저->실행기) 와 `스토리지 엔진 레이어`가 분리된 구조
- InnoDB 는 스토리지 엔진 중 하나로 신뢰성과 성능 균형을 목표로 하는 범용 스토리지 엔진이다. (`MySQL 기본 스토리지 엔진`)
- InnoDB 는 ACID 트랜잭션, 행수준 락(+MVCC), 클러스터드 인덱스 기반 저장, 크래시 복구 등의 핵심 기능을 제공

## InnoDB 아키텍처

- InnoDB 가 해결하고자 하는 것
    - 디스크는 느리다.
    - DB 서버는 언제든 죽을 수 있다.
    - 여러 사람이 동시에 같은 데이터를 건드린다.

### .ibd 파일

- https://dev.mysql.com/doc/refman/8.4/en/innodb-file-per-table-tablespaces.html
- 테이블 하나당 한개 `.ibd` 파일을 생성 (users 테이블 => users.ibd)
- `.ibd` 파일에는 해당 테이블의 행 데이터와 인덱스가 전부 저장
- 파일 내부는 16kb 페이지 단위로 구성되고, 페이지 번호와 파일 안의 위치가 고정됨.
    - 즉, page3 은 언제나 파일의 48kb 지점
    - page 사이즈 커스텀 가능 `innodb_page_size`
- 읽고 쓰는 최소 단위가 페이지(16kb) 이므로, `행 하나만 따로 읽거나 쓸 수 없음`
    - 행 하나에서 숫자 하나만 바꿔도, 디스크에 값을 반영하려면 `해당 행이 속한 16kb 페이지를 통째로 다시 써야 함`

### 메모리 구조 (Buffer-pool)

- https://dev.mysql.com/doc/refman/8.4/en/innodb-buffer-pool.html
- 테이블과 인덱스 데이터 접근시 `메모리에 캐시`해 두는 영역
- InnoDB는 데이터를 행 단위가 아닌 페이지 단위(16kb)로 읽고 씀
- 행 하나가 필요해도 그 행이 들어있는 `페이지 전체를 메모리에 로드`하고 이 메모리 공간을 `버퍼 풀(buffer-pool)` 이라 함
    - 특정 행에 대한 작업시 페이지 단위로 접근하는 것은 디스크, 메모리 동일
- 버퍼 풀은 테이블과 인덱스 데이터 접근시 캐시하는 영역이고, 전용 DB서버의 경우 `물리 메모리의 80% 까지도 할당`
    - 권장상한이 80%
- 자주 사용되는 데이터가 버퍼 풀에 머무는 이상 디스크 접근이 최소화되므로 버퍼풀 크기는 InnoDB 성능에 가장 큰 영향을 끼침
    - 메모리가 꽉차면 `LRU(Least Recently Used)` 기반으로 evict
    - `단순한 LRU 가 아닌`, 리스트를 new, old 영역으로 나누고 새로 읽은 페이지를 중간 지점에 넣고 한 번 더 접근이 되는 경우 new 영역으로 승격하는 방식
    - 한 번 사용되고 사용되지 않는 데이터를 보다 빠르게 evict 하기 위함

### WAL (Write-Ahead Logging)

- https://dev.mysql.com/blog-archive/mysql-8-0-new-lock-free-scalable-wal-design/
- 데이터 변경이 발생하면 InnoDB 는 버퍼 풀 페이지를 메모리에서 수정
- 해당 변경은 디스크에 바로 쓰지 않음
- "무엇을 어떻게 바꿨다" 라는 기록을 파일에 기록 (= `redo 로그`)
    - 이 방식을 `WAL` 이라 한다.
- 커밋 시점에 디스크에 남는 것은 `redo 로그`이고, `실제 디스크의 데이터 페이지는 백그라운드에서 여유 있게 반영`
- 디스크 데이터 파일에 반영 시점
    - 더티 페이지 수가 쌓인 경우 (`innodb_doublewrite_pages = 128` ⇒ 더티 페이지 128개 쌓인 경우 flush)
    - 버퍼 풀에 공간이 부족할 때
    - redo 로그 공간 부족할 때
    - 부하가 낮을 때 백그라운드에서 상시
    - ...
- 변경사항 커밋시 디스크 데이터 파일에 바로 쓰지 않고 `로그파일에 순차 쓰기로 기록하므로 속도 성능상 이점`
    - 디스크에 바로 쓰는 경우 그 때마다 `포그라운드에서 랜덤 write`
- db 서버가 갑자기 죽어도 `redo 로그` 기반으로 다시 재생하면 되므로 커밋된 변경 반영 가능
- `undo` 로그는 `redo 로그` 의 반대방향의 기록이다.
    - 즉, 바뀐 값이 아닌 바뀌기 전 값을 저장한다.
    - 롤백시 사용되며 MVCC의 재료가 된다.

### Doublewrite Buffer

- https://dev.mysql.com/doc/refman/8.4/en/innodb-doublewrite-buffer.html
- InnoDB는 16kb 페이지 단위로 디스크에 쓰지만, `16kb 쓰기에 대한 원자성을 디스크 레벨에서 보장하지 않는다.`
    - 디스크가 보장하는 원자 단위는 보통 4kb 또는 512바이트 페이지 단위이므로 `16kb 는 4회로 쪼개서 나갈 수 있음`
    - 버퍼풀에서 수정된 16kb 페이지를 디스크에 쓸 때 16kb 단위로 쓸텐데 16kb 쓰기에 대한 원자성을 보장하지 않음
    - 16kb 페이지 쓰기시 앞 8kb 정도만 쓰기고 뒷 내용은 쓰여지지 않는 것을 torn page(부분 쓰기)라 한다.
- redo 로그로 문제 해결 불가
    - redo 로그 “이 페이지의 이 위치를 이렇게 바꾸라” 라는 것만 기록
    - 즉, redo 로그 재생을 통해 디스크에 데이터를 온전히 반영하려면 `디스크 페이지 자체가 온전한 상태여야 한다.`
    - redo 로그는 `변경 유실`을 막지만, `페이지 자체의 물리적 손상`은 막지 못한다.
- 버퍼 풀의 더티 페이지를 디스크로 보낼 때, 디스크 제자리에 바로 쓰지 않고 `두 단계를 거친다`
    - `(1단계)` doublewrite 영역에 순차로 write → fsync 완료될 때까지 대기
    - `(2단계)` 디스크 원래 위치(.ibd) 에 write → fsync
- 디스크에 페이지를 쓰기 전 InnoDB 는 `doublewrite 영역에 페이지를 먼저` 쓰고, double write영역에 대한 write/flush 가 `완료된 이후 디스크(.ibd) 에 페이지를 쓴다.`
    - doublewrite 영역은 영구 보관소가 아니고 직전 내보낸 페이지들의 임시 사본만이 저장
- 왜 안전한가?
    - 1단계 (doublewrite 영역에 순차쓰기) 영역에서 페이지 깨진 경우
        - 디스크에는 영향X
        - redo 로그 기반으로 재생 복구
    - 2단계 (디스크 원래 위치 쓰기 중 페이지 깨진 경우)
        - doublewrite 영역에는 온전한 사본이 있음
        - doublewrite 사본 통해 디스크에 데이터 반영 이후 redo 로그 재생
- 복구 순서
    - doublewrite 여역 통해 페이지를 온전하게 만든다.
    - redo 로그 통해 유실된 변경을 반영
- doublewrite 는 `.dblwr` 파일 형태로 존재
- I/O 비용이 늘어나지만 2배로 늘어나지는 않는다.
    - 더티 페이지 flush 시 doublewrite 사용여부와 관계없이 최대 페이지 수만큼 랜덤 write 발생
    - 디스크로 랜덤 write 전 doublewrite 에는 1번의 `순차쓰기`만 발생
    - doublewrite 사용시 fsync 추가 1회, 순차쓰기1회임 (실제 전송되는 byte 전송량은 2배)
- doublewrite 는 기본 활성화
    - `innodb_doublewrite=ON`

## Reference

- https://dev.mysql.com/doc/refman/9.7/en/innodb-storage-engine.html
- claude