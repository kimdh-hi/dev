SSH / Telnet

SSH
- ssh (Secure Shell)
- 원격 호스트에 접속하기 위한 프로토콜
- 기존 원격 호스트 접속에 사용되는 `telnet` 의 경우 암호화가 지원되지 않는다는 보안상 취약점 존재
  - `telnet` 을 사용하는 경우 `wireshark` 와 같은 패킷 분석 툴로 원격 호스트로 전송되는 비밀번호, 파일 등의 내용을 탈취당할 수 있다.

ssh 접속
- `ssh [사용자 계정]@[원격지 Ip] -p [포트번호]
  - `ssh root@192.168.001.001` -p [포트번호]

sshpass
- ssh 로 원격 호스트 접속시 추가로 명령어를 실행할 수 있는 기능
- `sshpass -p [ssh패스워드] ssh [사용자 계정]@[원격지 I] -p [포트번호] "ls"

ssh host key checkinh 비활성화
- ssh 접속시 host 의 key 를 체크하지 않고 접속
- ssh 로 원격지 서버 접속시 서버에 대한 인증을 위해 로컬에 저장한 서버의 키와 비교하는 작업
- `ssh -o StrictHostKeychecking=no`

---

Telnet
- `telnet [IP] [포트번호]