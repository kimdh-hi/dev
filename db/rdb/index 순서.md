### Index order

GroupBy index order
- groupBy 절 사용시 index 가 적용되려면 groupBy 에 사용된 컬럼의 순서와 index 의 순서가 같아야 한다.
```
example

index: col1, col2, col3

group by col1 // 인덱스 사용o
group by col1, col2 // 인덱스 사용o
group by col2 col1 // 인덱스 사용x (순서 미일치)
```

group by + where
- group by 순서 미일치 시, 선행된 where 절까지 순서가 지켜진다면 인덱스 사용됨
```
example

index: col1, col2, col3

where col1=xxx
group by col2, col3 // 인덱스 사용o
```

---

order by index order
- group by 와 거의 동일
- 주의, 컬럼 사이에 `desc` 사용시 인덱스 사용 불가능

```
example

index: col1, col2, col3

order by col1, col2 // 인덱스 사용 o
order by col2, col1 // 인덱스 사용 x
order by col1, col2 desc, col3 // col2 desc 로 인해 인덱스 사용 x
```
