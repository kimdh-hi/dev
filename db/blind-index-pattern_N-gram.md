# like 검색 가능 채팅 암호화 

## N-gram 기반 Blind Index pattern?

### Blind Index pattern?

암호화된 데이터를 효율적으로 검색하기 위한 패턴
- aes 와 같은 대칭키 암호화 기법 통해 데이터를 암호화하여 db 에 저장하는 경우 `where like` 검색 불가
- 암호화 된 컬럼은 aes 와 같은 암호화 기법 적용하여 암호화 된 상태로 db 에 저장
- 암호화 된 컬럼 외 검색만을 위한 별도 hash 컬럼을 생성

Blind Index pattern 검색 flow
```
table: chat_message
- content: aes 암호화 된 컬럼

table: chat_message_blind_index
- content: chat_message.content 를 해시한 값
- chat_message_id: 원본 chat_message pk

1. '김대현' 검색어 해시화
2. chat_message_blind_index select query
3. 검색 결과 chat_message_id 기반 chat_message 조회
4. 조회 결과 복호화 후 응답
```

Blind Index pattern 도입시 암호화 된 데이터 검색이 가능하다. <br/>
단, Exact match(완전 일치) 검색만 가능하다. <br/>
like 검색을 지원하기 위해서는 N-gram 기반 기법이 추가되어야 한다.

### N-gram

- 텍스트 검색을 위해 문자열을 n개의 연속된 조각으로 나눈다.
  - N: 나눈 문자열 조각의 길이
  - Bi-gram(2-gram): 2글자씩 문자열을 나눈다.

```
안녕하세요 (Bi-gram) 적용

*안녕*하세요 --> 안녕
안*녕하*세요 --> 녕하
안녕*하세*요 --> 하세
안녕하*세요* --> 세요

결과물: ['안녕', '녕하', '하세', '세요']
```

N 이 작아질수록 검색 정밀도는 높아지지만 데이터양이 폭발적으로 늘어난다.


### N-gram 기반 Blind Index pattern flow

```
저장
1. 채팅메세지 원본 AES 암호화 하여 chat_message 저장
2. 채팅메세지 원본 2글자로 조각화 (Bi-gram)
3. 조각된 원본 채팅메세지 해시

4. 해시된 결과 목록 chat_message id 와 함께 chat_message_blind_index 에 저장

조회
1. 검색 키워드 원본 2글자로 조각화 (Bi-gram)
2. 조각된 검색 키워드 원본 해시
3. 해시된 결과 목록으로 chat_message_blind_index 조회
4. 조회 결과 chat_message id 목록(중복제거)으로 chat_message 조회
5. 조회 결과 복호화 하여 응답
```

## N-gram 기반 Blind Index pattern 고려사항

```
R=L-(n-1)

R: row
L: 원본 문자열 수 (공백포함)
n: N-gram 단위

100글자 메세지, 2-gram
- 100(2-1): 99개 추가 row 생성

100글자 메세지, 3-gram
- 100(3-1): 50개 추가 row 생성
```