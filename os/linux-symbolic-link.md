### linux symbolic link (soft link)

특정 폴더/파일 등에 링크를 연결하는 것으로 원본 파일과 같은 취급이 되도록 하는 것. (=윈도우의 바로가기)

symbolic link 생성
- `ln -s original copy`
- `copy` 가 바로가기에 해당
- `ln` 은 기본적으로 `hard link` 를 생성
  - `-s` 옵션을 통해 `soft link` 가 생성되도록 한다.
- `unlink`
  - `unlink <link 경로>`
  - `rm <link 경로>`


hard link
- `soft link` 와 기능은 동일 (=윈도우의 바로가기)
- 원본파일이 삭제되어도 `hard link` 가 설정된 파일에 영향이 없음
  - `soft link` 의 경우 원본파일 삭제시 link 된 파일도 삭제
- 이론적으로 `hard link` 의 경우 각 파일이 서로 다른 `inode` 를 가지기 때문

inode (index-node)
- 리눅스 시스템에서 모든 파일, 디렉토리는 고유한 `inode` 를 가진다
- `ls -i` 를 통해 `inode` 를 확인 가능