# Valkey

## Valkey?

- BSD 라이선스 오픈소스 inmemory db
    - val(value) + key
- 2024/3 redis 가 BSD 라이선스를 부리고 SSPLv1, RSALv2 로 전환하면서 Redis Inc, 와 상업 계약없이 매니지드에서 redis 서비스를 사용할 수 없게됨 (aws elasticache)
    - redis 라이선스 변경 이후 redis BSD 라이선스 마지막 버전인 Redis 7.2.4 를 Valkey 로 포크했고, Linux Foundation 에서 2024/3 신규 프로젝트로 받아들여짐.
    - aws, google, oracle 등 주요 클라우드 서비스 업체와 더불어 여러 기업이 기여했으며 2개월 만에 첫 릴리즈 (Valkey 7.2.5)
- aws elasticache 는 Redis OSS 지원은 여전히 하지만 BSD 라이선스의 7.2 버전에 묶여있음
    - https://aws.amazon.com/ko/blogs/database/best-practices-valkey-redis-oss-clients-and-amazon-elasticache/
    - valkey 를 권장
    - Redis OSS 대비 20% 가량 비용 저렴
        - https://aws.amazon.com/ko/blogs/database/reduce-your-amazon-elasticache-costs-by-up-to-60-with-valkey-and-cudos/
        - https://aws.amazon.com/ko/elasticache/pricing/
- 비동기 I/O 스레딩, 예외처리 개선, 클러스터 안정성, observability ... 등 비용, 라이선스 외 개선안 적용됨

## redis 라이선스 변경

- 기존 BSD 라이선스 (BSD 3-Clause)
    - BSD(Berkeley Software Distribution): MIT 와 비슷한 레벨의 관대한 오픈소스 라이선스
    - 출처만 밝히면 사용, 수정, 배포 등 가능
    - cloud managed service 에서 사용목적으로 사용 가능 (ElatiCache..)
- 2024/03/20 BSD -> SSPL + RSALv2 라이선스 전환
    - RSALv2 (Redis Source Available License)
        - 오픈소스x
        - 소스를 공개되어 가져다 사용할 수는 있지만, 매니지드 서비스로 제공하는 것은 불가.
    - SSPL (Server Side Public License)
        - Redis를 매니지드 서비스에서 제3자에게 제공시 해당 서비스 전체코드 공개 필요
- 2025/5 AGPLv3 라이선스 추가
    - 오픈소스o
    - 오픈소스 사용시 수정한 버전을 네트워크 서비스로 운영하는 경우 해당 서비스를 이용한 사용자에게 수정한 코드를 공개 필요
- why?
    - aws elsticache 같은 클라우드 업체는 redis 를 통해 매출을 올리지만 Redis Inc. 는 돈을 벌지 못함
    - redis 사용자중 1%만 redis enterprice 고객으로 전환된다고 함.
- Redis 7.2.x 포함 이전 버전의 경우 BSD 라이센스 유지
- Redis 7.4.x ~ 7.8.x: RSALv2/SSPLv1
- Redis8: RSALv2, SSPLv1, AGPLv3

## aws elasticache migration (redis -> valkey)

- https://docs.aws.amazon.com/AmazonElastiCache/latest/dg/VersionManagement.HowTo.html

## Reference

- https://github.com/valkey-io/valkey
- https://github.com/redis/redis/releases
- https://tech.imweb.me/posts/redis-oss-valkey-upgrade/
- https://news.hada.io/topic?id=29451