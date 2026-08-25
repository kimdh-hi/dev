# GitOps

## GitOps?

- 기존 jenkins, github action(gitlab ci/cd) 같은 ci 도구를 통해 빌드 후 kubectl apply, helm upgrade 를 통해 클러스터 바깥에서 안쪽으로 밀어넣는 push 모델을 사용
- GitOps 는 pull 모델을 사용
- k8s 클러스터 내에서 git 저장소를 주기적으로 읽어와 스스로 적용
- 외부에서 클러스터 내부터 들어오는 인바운드 연결은 없고, 클러스터에서 git 으로나가는 아웃바운드 연결만 있음
- GitOps 는 표준을 정의하고 실제 GitOps 를 지원하는 툴 중 대표적인 것은 Argo CD, Flux 등이 있다.

## GitOps 4대 원칙

- Declarative
    - GitOps 로 관리되는 시스템은 원하는 상태가 선언적으로 표현되어야 함
    - 반대되는 개념으로 명력적 방식의 경우 `kubectl scale deployment XXX --replicas=3` 처럼 실행할 명령을 나열
    - deployment.yaml 같은 선언적 상태를 관리
- Versioned And Immutable
    - 선언적인 상태를 표현하는 파일에 대한 불변성과 버전관리를 강제
    - 여기서 불변성은 파일을 수정될 수 있으나 수정하면 새 버전이 생기고 이전 버전은 그대로 남는다는 성질을 의미
- Pulled Automatically
    - 사람이 수동으로 배포하지 않고, 클러스터 내부 에이전트가 주기적으로 저장소를 폴링하거나 웹훅을 받아 처리
- Continuously Reconciled
    - 수동으로 가한 클러스터 상태 변경에 대해 최종 상태로 다시 재조정한다.
    - 예를들어 트래픽이 몰려 급하게 수동으로 pod 를 늘린 경우 기존 ci/cd 프로세스에서는 수동으로 다시 배포하기 전까지 늘어난 상태로 계속 운영된다. (self-heal)
    - 지속적으로 상태를 관찰하고 최종 상태로 재조정한다.

## 저장소

- 저장소는 최소 2개 이상 사용 권장
- 애플리케이션 저장소: 애플리케이션 소스 코드, dockerfile, ci 관련 파일이 위치하는 저장소
- 배포환경 구성 저장소: k8s manifast 파일, 애플레케이션과 모니터링, 메세지 브로커 등 인프라들이 어떤 버전으로 어떻게 구서오디어야 하는지 정보 관리

## reference

- https://opengitops.dev/
- https://www.samsungsds.com/kr/insights/gitops.html
- https://argo-cd.readthedocs.io/en/stable/