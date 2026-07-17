# NFS (Network File System)

- 클라이언트가 네트워크로 연결된 서버의 파일을 로컬 파일시스템처럼 접근 가능하게하는 분산 파일 시스템 프로토콜
- 일반적으로 로컬 디스크의 파일 접근시 open(), read(), write() 와 같은 system call 를 통한다.
- NFS 는 이 호출을 유지하면서 로컬디스크가 아닌 네트워크 상 원격지 서버로 접근하도록 한다.
- ONC RPC (Open Network Computing RPC)
    - 네트워크 상 원격지 서버의 read(), write() 등의 함수 호출
    - read() system call 시 NFS 클라이언트는 read RPC 요청으로 변환
- XDR (ExternalData Representation)
    - 이기종 서버 간 데이터 표현방식 통일을 위한 표준
    - RPC 통해 데이터를 네트워크로 내보내기 전 XDR 표준 형식대로 변환
- 클라이언트에서 서버의 특정 경로를 마운트하면 클라이언트 츠게엇 해당 서버 경로 이하 파일들이 로컬 파일처럼 노출

```
//마운트 전 클라이언트 A의 /mnt/data:
/mnt/data/
├── local_a.txt
└── old_folder/

//서버 B의 export 경로 /srv/nfs/data:
/srv/nfs/data/
├── server_b1.txt
└── shared_folder/

//클라이언트A 에서 마운트
sudo mount -t nfs -o vers=4.1 192.168.1.20:/srv/nfs/data /mnt/data

//마운트 후 A에서 /mnt/data 조회시
/mnt/data/
├── server_b1.txt       ← B의 내용
└── shared_folder/       ← B의 내용
```

## 설정 방법

```
서버 B(파일 보유): 192.168.1.20, 공유할 디렉터리 /srv/nfs/data
클라이언트 A(접근): 192.168.1.10, 마운트 위치 /mnt/data
```

1. 서버 측 설정

```
# 패키지 설치
sudo apt update
sudo apt install -y nfs-common

# export 항목 작성
sudo nano /etc/exports
/srv/nfs/data  192.168.1.10(rw,sync,no_subtree_check)

# /etc/exports 및 /etc/exports.d를 커널 export 테이블과 동기화(재export)
sudo exportfs -ra

# NFS 서버 데몬 시작 및 부팅 시 자동 시작
# (Debian/Ubuntu의 서비스 이름은 nfs-kernel-server)
sudo systemctl enable --now nfs-kernel-server

# 방화벽 설정
sudo ufw allow from 192.168.1.10 to any port 2049 proto tcp
sudo ufw reload
```

1. 클라이언트 측 설정

```
# 패키지 설치
sudo apt update
sudo apt install -y nfs-common

# 마운트
sudo mount -t nfs -o vers=4.1 192.168.1.20:/srv/nfs/data /mnt/data

# 마운트 결과 확인
mount | grep /mnt/data
df -h /mnt/data

# 재부팅시 유지 설정
sudo nano /etc/fstab
192.168.1.20:/srv/nfs/data  /mnt/data  nfs  vers=4.1,_netdev  0  0
```

## Cloud Service

- AWS EFS (Elastic File System)
    - EFS 는 실제 데이터를 저장하는 스토리지
    - EC2 인스턴스와 분리되어 독립적인 리소스로 존재
        - EC2 인스턴스 삭제대호 EFS 데이터는 남아있음
    - 일반적인 linux서버 a, b 에서 NFS 구성하는 것과 개념의 차이가 있음
        - a → b rw 마운트 시 a 에서 파일을 쓰면 실제 파일은 b 파일 시스템에 쓰여짐.
        - a 에서는 b 로 read 를 RPC 통해 읽어와서 b 의 파일이 보이는 것뿐.

```
[직접 구축시]
EC2 A ---NFS mount--> EC2 B

[EFS]
EC2 A --NFS mount--> mount target --> EFS
```

- OCI FSS (File Storage Service

## reference

- https://en.wikipedia.org/wiki/Network_File_System
- Claude
- https://docs.aws.amazon.com/efs/latest/ug/mounting-fs-old.html