## redis requirepass

### redis NOAUTH Authentication required
- redis-cli
- auth [password]

### password 설정

```
127.0.0.1:6379> auth "test123"
(error) ERR AUTH <password> called without any password configured for the default user. Are you sure your configuration is correct?


127.0.0.1:6379> config get requirepass
1) "requirepass"
2) ""

config set requirepass [password]
```


