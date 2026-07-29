# linux user

## why

- 리눅스의 조상 유닉스 사용 당시 컴퓨터는 고가 장비였기에 여러 사람이 나누어 사용했음
- 때문에 A가 만든 파일을 B가 마음대로 읽거나 지우지 못하게 해야 했음
- 위 요구사항 해결을 위해 리눅스 사용자 시스템이 생겨남

## UID, GID

- 커널은 user 의 이름은 알지 못한다.
- 커널은 UID(UserID), GID(GroupId) 등 숫자만 다룬다.

```
$ id
uid=1000(alice) gid=1000(alice) groups=1000(alice),27(sudo),100(users)
```

- uid=1000(alice): 내 UID는 1000이고, 그 숫자에 붙은 이름표가 alice
- gid=1000(alice): 내 기본 그룹의 GID는 1000이고 이름은 alice
- groups=1000,27,100: 내가 속한 모든 그룹의 GID 목록
- 사용자 이름을 바꿔도 UID 는 그대로이므로 user 권한에 있어 변경되는 것은 없음

### 커널 접근 판단

```
$ ls -l report.txt
-rw-r--r-- 1 alice staff 2048 Jul 26 10:00 report.txt
```

```
-rw-r--r--   alice   staff
 ↑    ↑  ↑     ↑       ↑
 │    │  │     │       └─ 이 파일의 소유 그룹 (GID)
 │    │  │     └───────── 이 파일의 소유자 (UID)
 │    │  └─────────────── 그 외 모든 사람의 권한 (r--)
 │    └────────────────── 소유 그룹 멤버의 권한 (r--)
 └─────────────────────── 소유자의 권한 (rw-)
```

- 프로세스가 report.txt 에 접근시 커널은 다음 순서로 권한을 검증
    - 이 프로세스의 UID가 파일 소유자 UID와 같은가
    - 이 프로세스가 속한 그룹 중 파일의 소유 그룹이 있는가
    - 나머지 권한 적용
- 커널의 권한 검증은 순서대로 적용되고 앞단의 조건에 걸리면 다음 조건은 보지 않는다.

```
----r--r-- 1 alice staff 2048 Jul 26 10:00 test.txt
```

- 소유자 권한이 `--` 인 경우 해당 파일의 소유자가 `alice` 인 상태에서 `alice` 의 UID 를 가진 프로세스도 `test.txt` 읽지못함

### 그룹 (GID)

- 특정 그룹에게 일괄 권한 부여 위함
- 기본 그룹 (primary group)
    - user 마다 정확히 하나
    - `id` 출력시 `gid=` 부분
    - 사용자가 새 파일 생성시 파일의 소유 그룹은 생성한 사용자의 기본 그룹이 됨
- 보조 그룹 (supplementary group)
    - `id` 출력시 `groups=` 에서 첫번째를 뺀 나머지
    - 권한 검사시 기본 그룹과 동일하게 적용

```
# 보조그룹 추가
usermode -G group1 alice # alice 의 보조 그룹을 group1 하나로 교체
usermode -aG group1 alice # -a(append) 통해 group1 을 추가
```

## root

- root: UID 가 0인 사용자
- 권한 검사
    - 특권 프로세스(privileged process): effective UID가 0 (커널의 모든 권한 검사를 skip)
        - UID 0인 프로세스는 `--------` 권한 파일도 읽고 쓰기 가능
    - 비특권 프로세스(unprivileged process): effective UID가 0이 아닌 것 (UID, GID, 보조 그룹 등에 근거하여 권한 검사 받음)
- 비특권 사용자: UID 0이 아닌 모든 사용자

### real UID, effective UID

- `alice` 가 본인의 비밀번호 변경하려는 경우 `/etc/shadow` 수정 필요
    - `/etc/shadow` 는 root 만 쓸 수 있음
    - `alice` 는 UID 1000 비특권 사용자이므로 수정 불가
    - 수정을 허용한다면 다른 사람의 비밀번호까지 바꿀 수 있게 됨
    - 수정이 불가하다면 본인의 비밀번호 수정 못함
    - `effective UID` 를 통해 위 이슈를 해결한다.
- real UID 는 프로세스의 소유자를 결정하고, effective UID 는 커널 권한 검사시 사용된다.

#### `setuid` 비트

```
$ ls -l /usr/bin/passwd
-rwsr-xr-x 1 root root 68208 Feb  6 12:04 /usr/bin/passwd
   ↑
   여기가 x가 아니라 s
```

- 소유자 권한 자리에 `x` 대신 `s` 가 위치
- 이것을 실행할 때 실행한 사람이 누구이든 effective UID 를 이 파일의 소유자(root) 로 설정한다.
- 이제 alice 는 passwd 를 실행할 수 있고, passwd 내에서 real UID 를 알 수 있으므로 실행가능하되 본인것만 수정가능하도록 처리 가능

#### su, sudo

- su, sudo 도 setuid 을 활용

```
$ ls -l /usr/bin/su /usr/bin/sudo
-rwsr-xr-x 1 root root  55672 Feb  6 12:04 /usr/bin/su
-rwsr-xr-x 1 root root 277936 Apr  3 09:11 /usr/bin/sudo
   ↑
   둘 다 s (setuid 비트)
```