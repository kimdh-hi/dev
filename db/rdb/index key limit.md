## index key limit

### mysql key limit
- 최대 3072 bytes
  - utf8mb4 4byte
  - key column length <= varchar(768)
- https://dev.mysql.com/doc/refman/8.4/en/innodb-limits.html
- The index key prefix length limit is 3072 bytes for InnoDB tables that use DYNAMIC or COMPRESSED row format.

reference
- https://dev.mysql.com/doc/refman/8.4/en/innodb-limits.html

---

### mariadb key limit
- mysql 과 동일 (최대 3072 bytes)
- 단, 최대 길이 초과하는 경우 해당 키 캐싱하여 사용
- index key 크기 *3072 bytes 초과 가능*

refernece
- https://mariadb.com/kb/en/innodb-limitations/
- https://jira.mariadb.org/browse/MDEV-371
- https://mariadb.com/kb/en/getting-started-with-indexes/