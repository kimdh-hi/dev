### Mariadb 이모지 저장 

```
//character set 확인
show variables like 'c%';
```

mariadb 는 utf8 을 3byte 로 처리한다. (현재 전세계 모든 언어는 3byte(21bit) 정도로 모두 표현가능) <br/>
이모지 중 4byte 로 표현되는 것이 있어 저장시 에러가 발생할 수 있다. <br/>

MySql, Mariadb 는 4byte utf8 문자열을 처리하기 위해 `utf8mb4` charset 을 제공한다. <br/>

#### utf8 4byte 처리
- charset=`utf8mb4`
- collation=`utf8mb4_unicode_ci`

#### 설정
```
//my.cnf
[mysqld]
default-character-set=utf8mb4
default-collation=utf8mb4_unicode_ci
```