# git flow

## git flow?

- https://nvie.com/posts/a-successful-git-branching-model/
- 2010년 1월 5일 Vincent Driessen이 "A successful Git branching model"이라는 글에서 공개한 브랜치 운영 규칙
- git 의 새로운 기능이 아닌, 지켜야 할 절차의 집합

## 브랜치 구조

- 수명이 무한한 영구 브랜치 2개와 수명이 유한한 보조 브랜치 3개로 나눔
- 영구 브랜치(main, develop), 보조 브랜치(feature, release, hotfix)

### 영구 브랜치

- main, develop
- main
    - 직접 커밋/푸시 금지
    - release, hotfix 브랜치 merge 커밋만 존재
    - main 의 커밋수가 릴리스 횟수와 일치해야 정상
    - main 브랜치의 merge 커밋은 번드시 버전으로 태그가 되어야 함
    - main 이 공식 릴리즈 이력을 저장하는 것.
- develop
    - 다음 릴리즈에 배포될 완료된 기능 (아직 배포 안 된, 다음 버전)
    - 프로젝트 변경사항의 완전한 이력을 저장
    - 별도 태그 필요 없음

### 보조 브랜치

- feature
    - develop 으로부터 브랜치 분기
    - develop 으로 merge
    - master, develop, release/, hotfix/ 를 제외한 이름으로 브랜치 생성
    - 일반적으로 개발자 로컬에서만 관리되고 origin 에는 올리지 않는다.
- release
    - develop 으로부터 브랜치 분기
    - release 브랜치를 develop 으로부터 딸 때 버전 확정 (`release/1.1.0`)
    - main 머지 전 배포 전 마무리 작업 이후 main 에 머지 (v1.1.0 태그)
    - main 머지 완료 이후 release -> develop 머지
    - main, develop 머지 이후 브랜치 제거
- hotfix
    - 운영 장애 발생시 main의 특정 태그로부터 브랜치 분기
    - hotfix 변경사항은 develop, main 양쪽 머지
    - main 머지시 새 버전태그 생성할 것.
    - hotfix 브랜치는 수정작업 이후 무조건 main 으로 머지
    - main 머지 완료 이후 develop 머지 후 hotfix 브랜치 삭제
    - 단, release 브랜치가 있는 경우 release 브랜치에만 머지하고 develop에 중복으로 병합할지는 팀 내 약속에 따르면 될 듯
        - 어차피 release 에 넣으면 추후 배포시 develop 으로 머지됨.
        - 괜한 실수가 싫다면 release, develop 모두 머지 이후 hotfix 삭제..

## git tag

- 태그는 특정 커밋에 붙이는 고정된 이름표
- 저장소 이력의 특정 지점을 중요한 시점으로 표시하는 기능으로 보통 v1.0, v2.0 같은 릴리즈 지점을 표시
- 8/1 코드 배포 완료, 8/12 장애 발생
    - 8/1에 배포한 코드가 정확히 어떤 커밋이었는지?
    - 태그가 없다면 git log 를 확인하면서 커밋날짜를 찾아야 함.
    - 특정 커밋에 릴리즈 지점을 표시한 태그가 있다면 해당 태그 기반으로 찾고 hotfix 브랜치 분기 가능
- git tag 통해 태그 생성시 HEAD가 가리키는 커밋에 붙음
    - git push 는 커밋만 원격지에 올라가므로 git push origin [태그] 별도 필요
- 동일 이름의 태그 두개 지정 불가능
    - `f` 옵션 사용시 기존 태그 교체
- 한 커밋에 서로 다른 이름의 태그 여러 개 지정 가능
- 이름 계층 구조 충돌 불가능
    - `git tag v1`
    - `git tag v1/1`
    - `fatal: cannot lock ref 'refs/tags/v1/1': 'refs/tags/v1' exists; cannot create 'refs/tags/v1/1'`

## reference

- https://techblog.woowahan.com/2553/
- https://nvie.com/posts/a-successful-git-branching-model/
- https://devocean.sk.com/blog/techBoardDetail.do?ID=165571&boardType=techBlog