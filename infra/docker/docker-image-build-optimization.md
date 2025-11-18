# docker image build optimization


## 목적
- 단순 코드 변경시 외부 라이브러리 등 변경이 없는 부분까지 매번 새롭게 빌드되는 것은 비효율적이므로 변경되지 않은 부분(layer)을 재사용하기 위함
- 빌드 결과 이미지 경량화

```
FROM FROM eclipse-temurin:21-jdk
COPY build/libs/*.jar app.jar
ENTRYPOINT [ "java", "-jar", "/app.jar" ]
```
- docker layer image cache 사용x
- multi stage build 통한 이미지 경량화x

## docker image layers
- docker image 는 여러 개 layer 로 구성
- 각 layer 는 동일한 base image 를 공유하므로 저장 공간 절약
- 각 layer 마다 변경된 부분만을 새로운 layer 로 새롭게 생성하므로 이미지 빌드 시간 단축 가능
- 단, 변경사항에 의해 새로운 layer 생성된 경우 해당 layer 이후 모든 layer 는 다시 생성됨.

## docker build cache
- docker image layer 생성시 이전에 동일한 layer (해당 명령에 동일한 결과를 만들어낸 layer) 를 재사용하여 빌드시간을 단축
- 특정 layer 에서 cache invalidate 된 경우 이후 layer 도 모두 새롭게 빌드된다.

## springboot layers
https://docs.spring.io/spring-boot/maven-plugin/packaging.html#packaging.layers.configuration
1. dependencies: 외부 라이브러리
2. spring-boot-loader: JarLauncher, loader...
3. snapshot-dependencies: snapshot 버전 외부 라이브러리
4. application: 애플리케이션 자체 클래스, 리소스

springboot 에서 권정하는 변경 빈도가 낮은 순이므로 위 순서대로 layer 선언

```yaml
# build stage
FROM eclipse-temurin:21 as builder
#...

FROM eclipse-temurin:21-jre
WORKDIR /application
COPY --from=builder /builder/extracted/dependencies/ ./
COPY --from=builder /builder/extracted/spring-boot-loader/ ./
COPY --from=builder /builder/extracted/snapshot-dependencies/ ./
COPY --from=builder /builder/extracted/application/ ./
#...
...
```


## docker multi-stage build
- `multi-stage build` 는 한 개 `Dockerfile` 에 여러 개 `from` 명령어를 사용하여 `build stage`, `runtime stage` 등으로 구분하는 것을 의미한다.
- runtime stage 는 build stage 의 결과물만을 사용
- runtime stage (마지막 from) 에는 build 시에만 필요한 환경 등을 제외하여 보다 작은 크기의 최종 이미지를 빌드한다.

ex) build stage 에서는 jdk (full jdk) base image 사용, runtime stage 에서는 jre base image 사용


---

## reference

Packaging Executable Archives (layers)
- https://docs.spring.io/spring-boot/maven-plugin/packaging.html#packaging.layers.configuration

docker image layers
- https://docs.docker.com/get-started/docker-concepts/building-images/understanding-image-layers/?utm_source=chatgpt.com

docker build cache
- https://docs.docker.com/get-started/docker-concepts/building-images/using-the-build-cache/?utm_source=chatgpt.com

multi-stage build
- https://docs.docker.com/build/building/multi-stage/?utm_source=chatgpt.com
