promethus vs zipkin

data 수집
- promethus: pull-base, push-base
- zipkin: push-base

storage
- promethus: 시계열 db
- zipkin:  ElasticSearch, Mysql 등 외부 솔루션을 사용

query
- promethus: Powerful Query (PromQL)
  - 경계값에 대한 알림 등에 탁월
- zipkin:  Simple Query

---

https://www.squadcast.com/compare/prometheus-vs-zipkin-a-comprehensive-comparison