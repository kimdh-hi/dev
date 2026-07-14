# Helm

- k8s 패키지 매니저
- k8s 앱 배포시 deployment, service, comfigMap… 등 수많은 manifast 를 작성 및 관리 필요
- 이 파일들을 여러 환경, 버전에 걸쳐 각각 관리하는 것의 어려움 해결 위함
- Helm 은 관련 manifast 들을 Chart(차트) 라는 단위로 묶어서 버전관리, 공유, 설치, 롤백 등을 처리 가능하게 한다.

## why use?

- 클러스터 내에 Redis 를 띄우고 싶다.
    - Helm 없이 하려면 Redis 용 Deployment, Service, PersistenceVolumeClaim 등 yaml 작성 필요
    - helm 사용시 `helm install my-redis bitnami/redis`
- 환경별 배포시
    - k8s 에 배포하려면 deployment, service, ingress 등 을 필요로 함
    - helm 없이 dev, beta, prod 환경별 띄어야 한다면 필요한 yaml 파일을 복사하고 내부에 환경별 옵션을 수정하고 관리해야 됨.
    - Helm 은 template 기능을 제공
        - `helm install <릴리스이름> <차트경로>`
    - manifast 는 한 벌만 작성하고, 환경마다 달라지는 값은 values 로 치환
    - 일반 deployment.yaml
        
        ```yaml
        # deployment.yml
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: mywebapp
        spec:
          replicas: 3
          template:
            spec:
              containers:
                - name: mywebapp
                  image: nginx:1.25
                  ports:
                    - containerPort: 80
        ```
        
    - template 적용 deployment.yaml
        
        ```yaml
        mychart/
        ├── Chart.yaml            ← "이건 mychart 차트야" (필수)
        ├── values.yaml           ← 빈칸 채울 값
        └── templates/
            └── deployment.yaml   ← 빈칸 뚫린 서식
        ```
        
        ```yaml
        # templates/deployment.yaml
        apiVersion: apps/v1
        kind: Deployment
        metadata:
          name: {{ .Values.appName }}
        spec:
          replicas: {{ .Values.replicaCount }}
          template:
            spec:
              containers:
                - name: {{ .Values.appName }}
                  image: {{ .Values.image.repository }}:{{ .Values.image.tag }}
                  ports:
                    - containerPort: {{ .Values.containerPort }}
        ```
        
        ```yaml
        # values.yaml
        appName: mywebapp
        replicaCount: 3
        containerPort: 80
        image:
          repository: nginx
          tag: "1.25"
        ```
        
        ```yaml
        # Chart.yaml
        apiVersion: v2          # 차트 API 버전. Helm 3+ 용은 v2
        name: mychart           # 차트(패키지) 이름. 패키징 시 mychart-0.1.0.tgz 로 사용됨
        description: A Helm chart to deploy the mywebapp nginx application  # 차트 한 줄 설명 (선택)
        type: application       # 차트 타입. application=배포되는 앱(기본값), library=공용 함수 모음
        version: 0.1.0          # 차트(포장지) 버전. SemVer 필수, 수정할 때마다 올림
        appVersion: "1.25"      # 안에 든 앱(nginx) 버전. 정보용, 따옴표 필수(숫자 해석 방지)
        
        ```
        
- 롤백, 업그레이드
    - 릴리즈에 변경이 있을 때마다 리비전 버전을 1씩 올려가며 스냅샷 저장
    - `helm upgrade <릴리스이름> <차트경로> [옵션]`
    - `helm rollback <릴리스이름> [리비전번호] [옵션]`
    - `helm history <릴리스이름>`
- ALB, node Observability 등 helm 통해 설치
- 직접 cli 로 관리하기 보다는 Argo CD/Flux, Terraform 등을 통해 코드로써 관리

## Reference

- https://helm.sh/docs/intro/introduction/