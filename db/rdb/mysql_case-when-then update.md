mysql 다중 조건, 다중 row update

```
update [table]
set [column] = case
  when [condition-column] = 'a' then 'updatedValueA'
  when [condition-column] = 'b' then 'updatedValueB'
  when [condition-column] = 'c' then 'updatedValueC'
else
```

intellij 에서 쿼리시 where 절이 없는 update 문을 허용하지 않음
- `Unsafe query: 'Update' statement without 'where' updates all table rows at once`

```
update [table]
set [column] = case
  when [condition-column] = 'a' then 'updatedValueA'
  when [condition-column] = 'b' then 'updatedValueB'
  when [condition-column] = 'c' then 'updatedValueC'
else
where [condition-column] in ('a', 'b', 'c');
```