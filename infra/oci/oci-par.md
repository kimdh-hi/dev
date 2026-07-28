# OCI PAR (Pre-Authenticated Request)

## PAR (Pre-Authenticated Request)

- https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/usingpreauthenticatedrequests.htm
- PAR 생성시 oci 서버측에 par 레코드와 그와 매핑된 고유한 URL 을 생성
- 해당 url 을 통해 누구나 object storage 에 접근 가능
    - par 생성시 해당 url 통해 가능한 액션, 만료시간 등을 지정
- S3 의 presigned URL 유사

```
https://objectstorage.<region>.oraclecloud.com/p/AbC123xYz...토큰.../n/mynamespace/b/app-dev-uploads/o/2026/07/27/abc.jpg

- /p/AbC123xYz...토큰.../: PAR 토큰
- /n/mynamespace: 네임스페이스
- /b/app-dev-uploads: 버킷
- /o/2026/07/27/abc.jpg: 객체 key
```

- 파일 업로드/다운로드 시 클라이언트는 서버로부터 PAR 을 포함한 URL 을 응답받아 그대로 fetch 하여 사용

## S3 Presigned URL과의 차이

- 별도 추가적인 인증없이 접근 가능한 임시 URL 을 발급한다는 점은 같지만 근복적인 동작 원리가 다르다.
- presignedURL 은 대칭키 방식으로 로컬에서 url 생성시마다 sdk 통해 서명된 값을 url 쿼리스트링에 포함
    - presignedURL 생성시 별도 api 호출 등 네트워크 호출 없음
- oci par 은 par 레코드 생성 위해 oci api(`CreatePreauthenticatedRequest`) 를 호출 (네트워크 호출O)
- oci par 은 url 마다 매핑되는 실제 데이터를 oci 측 서버에 저장
- oci par 은 만료일 지정 상한 없음 (presignedURL 의 경우 최대7일, 일반적으론 ~6시간)
- oci par 은 각 url 마다 매핑되는 par 레코드가 실제 oci 측에 저장되므로, par 생성시 응답된 id 를 통해 url 무효화 가능

```
PAR 생성시 object storage 에 해당 PAR 리소스 생성
응답에는 관리용 식별자 id 와 리소스 접근용 accessUri 포함
URL 내에는 opaque bearer token 이 포함. 해당 토큰은 PAR 리소스와 매핑 목적
```

## oci PAR 생성시 응답

```json
{
  "id": "QgT6f1skUMbXDhpXKQ4BRX9u7ci8AAJ7f9OGzgdEkNJ3XQmHzeN/kDhLEbN2HvPn",
  "name": "dl-019823f1-1753620900",
  "accessUri": "/p/2WOshPVWv9uqIqy6abokChGEXYdCZ8l75CoO26YkSARiRevWlDWJD_QUvtFPUocn/n/mynamespace/b/app-dev-uploads/o/t-001/attachment/2026/07/27/test.pdf",
  "objectName": "t-001/attachment/2026/07/27/019823f1.pdf",
  "accessType": "ObjectRead",
  "bucketListingAction": null,
  "timeCreated": "2026-07-27T14:25:00.482Z",
  "timeExpires": "2026-07-27T14:40:00.000Z"
}
```

- accessUri 는 상대주소이므로 앞에 oci bucket host 붙여줘야 함
    - https://objectstorage.<region>.oraclecloud.com/p/AbC123xYz...토큰.../n/mynamespace/b/app-dev-uploads/o/2026/07/27/abc.jpg
- 업로드의 경우 위 경로를 그대로 노출해도 무방
- 다운로드의 경우 직접 bucket 접근 가능한 url 대신 우리 서버의 다운로드 api 를 응답하는 것을 권장
    - PAR 생성 요청을 lazy 하게 처리하기 위함.
    - 목록조회의 경우 한 번에 많은 PAR 생성 요청이 몰릴 수 있음.
    - `/api/files/0581f74-as.../downloads` 와 같은 경로를 응답하고 해당 api 요청시 PAR 생성 및 redirect 처리

## 다운로드 방식

1. 서버 직접 스트리밍 (PAR X)
- 서버가 직접 클라이언트와 objectstorage 간 파일 데이터를 중계
1. PAR 발급 후 302 redirect
- 다운로드시마다 PAR 레코드가 oci 서버상에 생성됨
- 매번 새로운 PAR opaque token 이 포함된 url 로 리다이랙트 되어 다운로드 진행
- 장점: 백엔드 서버에 파일 데이터가 직접 영향을 끼치지 않음.
- 단점
    - 매번 PAR 레코드 생성위해 oci api 호출이 필요
    - 매번 redirect url 에 포함된 opaque token 으로 인해 브라우저 캐싱 불가
- 단점 해소위해 백엔드 측에 object key 단위로 캐싱하고, redirect 시 백엔드에서 브라우저 캐시 지정 필요
    - 백엔드에서 캐싱시 PAR ttl 보다는 만료시간을 짧게 설정하는 것이 안전
1. S3 호환 API presigned URL
- https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm