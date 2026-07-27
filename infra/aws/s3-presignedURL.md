# AWS S3 Presigned URL

## S3 Presinged URL

- https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html
- S3 객체는 기본적으로 비공개이고 소유자만 접근 허용
- Presigned URL 은 위 원칙을 유지하며 버킷 정책을 수정하지 않고 `특정 객체에 대한 시간 제한 접근 권한을 URL 통해 지원`
- 서버는 특정 파일에 특정 시간 동안만 접근 가능한 url 을 생성해서 응답시 클라이언트는 별도 추가적인 인증없이 해당 url 로 업로드/다운로드 등 가능
- presinged url 은 설정한 만료시간까지 재사용 가능 (`1회용이 아님`)
- 우리 서버는 클라이언트의 S3 관련 요청에 대한 권한검증만 수행하고 presignedURL 을 응답하면 클라이언트는 해당 url로 바로 리소스 업로드/다운로드 등을 하므로 `파일 데이터 자체는 우리 서버를 거치지 않는다.`
- 서버에서 presignedURL 생성시 aws 로 별도 api 호출 등 네트워크 비용없이 로컬에서 계산하여 url 생성한디.

```
<https://examplebucket.s3.amazonaws.com/test.txt>
  ?X-Amz-Algorithm=AWS4-HMAC-SHA256
  &X-Amz-Credential=AKIA...%2F20130721%2Fus-east-1%2Fs3%2Faws4_request
  &X-Amz-Date=20130721T201207Z
  &X-Amz-Expires=86400
  &X-Amz-SignedHeaders=host
  &X-Amz-Signature=abc123...
```

- URL 구성요소
    - X-Amz-Algorithm: 어떤 방식으로 서명했는지 표시. SigV4는 항상 AWS4-HMAC-SHA256
    - X-Amz-Credential:	누가 만들었는지(액세스 키 ID) + 어느 리전/서비스용인지
    - X-Amz-Date: 언제 만들었는지 (UTC 기준)
    - `X-Amz-Expires`: 만든 시각으로부터 몇 초 동안 유효한지
    - X-Amz-SignedHeaders: 서명에 어떤 헤더를 포함시켰는지 목록
    - X-Amz-Signature: 위 내용 전부를 비밀 키로 계산한 결과값
- 만료 시간
    - https://docs.aws.amazon.com/AmazonS3/latest/userguide/using-presigned-url.html
    - aws sdk 통해 설정시 최대 7일까지 설정 가능 (console 통해 생성시는 up to 12시간)
    - 최대 7일까지 허용 가능한 방법이 있지만 ec2 instance profile, ecs/fargate `자격증명수명 등에 의해 일반적으로 6시간 정도까지 유효`
        - `자격증명 수명에 의한 조기 만료`

## 업로드

- `Presigned PUT`, `Presigned POST`
- Presigned POST 의 경우 업로드 되는 파일에 대한 `크기, 타입, 경로 등 제한 가능`
    - `Presigned PUT` 의 경우 `제한 불가능`하므로 의도치 않은 대용량 파일 업로드 제한할 방법 없음.
- `Presigned PUT URL`
    - url 자체에 저장될 파일의 위치를 포함
    - body 에는 파일 원본 바이트만 포함되고 인증정보 등은 모두 URL 쿼리스트링에 포함
    - 클라이언트 측에서 사용시 편리
    - 단, 업로드시 파일 크기에 대한 검증 불가

```
PUT /uploads/user-42/photo.jpg?X-Amz-Algorithm=AWS4-HMAC-SHA256&X-Amz-Credential=...&X-Amz-Date=...&X-Amz-Expires=600&X-Amz-SignedHeaders=host&X-Amz-Signature=abc123 HTTP/1.1
Host: my-bucket.s3.ap-northeast-2.amazonaws.com
Content-Type: image/jpeg

<file bytes>
```

- `Presigned POST URL`
    - `policy 지정 가능`
    - policy 내부에서 파일 최대크기, 업로드 경로명 등등 검증 규칙 설정 가능

```
POST / HTTP/1.1
Host: my-bucket.s3.ap-northeast-2.amazonaws.com
Content-Type: multipart/form-data; boundary=----X

------X
Content-Disposition: form-data; name="key"

uploads/user-42/photo.jpg
------X
Content-Disposition: form-data; name="Content-Type"

image/jpeg
------X
Content-Disposition: form-data; name="policy"

eyJleHBpcmF0aW9uIjoiMjAyNi0wNy0yN1QxMjowMDowMFoiLCJjb25kaXRpb25z...
------X
Content-Disposition: form-data; name="x-amz-algorithm"

AWS4-HMAC-SHA256
------X
Content-Disposition: form-data; name="x-amz-credential"

AKIA.../20260727/ap-northeast-2/s3/aws4_request
------X
Content-Disposition: form-data; name="x-amz-date"

20260727T090000Z
------X
Content-Disposition: form-data; name="x-amz-signature"

def456...
------X
Content-Disposition: form-data; name="file"; filename="photo.jpg"
Content-Type: image/jpeg

<파일 바이트>
------X--
```

## 업로드-다운로드 flow

1. 업로드 url 발급
- 발급 시점에 pending 상태로 db 레코드 생성
- sdk 통해 presignedURL 생성시 버킷명, 객체키 필요하므로 서버에서 지정
1. 클라이언트 측에서 업로드 url 통해 업로드 완료 이후 서버로 콜백
- pending -> ready update
- 콜백 방식은 클라이언트가 직접할수도 있고, S3 이벤트 통해서 처리도 가능
    - 처리 자체를 멱등성있게 한다면 방어적으로 두 개 모두 받는 것 가능
    - `s3:ObjectCreated:*`
1. 다운로드 시 권장 검증 및 db 조회 후 presignURL 생성
- 실제 presignedURL 생성시 버킷명, 객체키 정도만 필수값