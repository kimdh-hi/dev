# argocd 테스트 환경 구성

## argocd 설치

- https://argo-cd.readthedocs.io/en/stable/getting_started/

```
kubectl create namespace argocd

kubectl apply -n argocd --server-side --force-conflicts -f <https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml>
```

## 설치 확인

```
kubectl get pods -n argocd
NAME                                                READY   STATUS              RESTARTS   AGE
argocd-application-controller-0                     0/1     ContainerCreating   0          18s
argocd-applicationset-controller-6db89bc85d-mzfqr   0/1     ContainerCreating   0          18s
argocd-dex-server-5c859d8bd9-qpjcw                  0/1     Init:0/1            0          18s
argocd-notifications-controller-659d6db5fc-rs284    0/1     ContainerCreating   0          18s
argocd-redis-f8766bdc7-bds89                        0/1     Init:0/1            0          18s
argocd-repo-server-7b8c487b84-jrb52                 0/1     Init:0/1            0          18s
argocd-server-6799b86d7-tsx9w                       0/1     ContainerCreating   0          18s
```

## 외부 접속 테스트 위한 포트포워딩

- argocd 접근 (https://localhost:30000/)

```
kubectl port-forward svc/argocd-server -n argocd 30000:443

> Forwarding from 127.0.0.1:30000 -> 8080
> Forwarding from [::1]:30000 -> 8080
```

- 초기 비밀번호 확인 (id: admin)

```
kubectl -n argocd get secret argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 -d; echo

> SYkXuvGgIO18b8-k
```

- argocd 콘솔 통해 비밀번호 지정 (User Info > Update Password)

## port-forward 대신 NodePort 기반 노출

```
kubectl patch svc argocd-server -n argocd -p '{
  "spec": {
    "type": "NodePort",
    "ports": [
      {"name": "http", "port": 80, "targetPort": 8080, "nodePort": 30000},
      {"name": "https", "port": 443, "targetPort": 8443, "nodePort": 30001}
    ]
  }
}'
```