## mysql random row select

sol1 - `order by rand()`
```
select col from table order by rand() limit 1;
```
