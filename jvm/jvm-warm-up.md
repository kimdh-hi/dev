jvm warm up

class loader
- class loader 는 3개 작업을 수행한다.
  - loading, linking, initializing
- loading
  - jvm 구동에 필요한 기본적은 class 를 포함하여 직접 작성된 class 까지를 jvm 메모리에 적재한다.
- linking
  - 로드된 클래스 파일이 참조하는 다른 클래스, 메서드, 필드 등을 검증하고 메모리 상에서 연결한다.
  - 메모리에 load 된 클래스 파일에 대한 대해 세 개 작업을 수행한다.
  - verification, preparation, resolution
- initializing
  - 클래스 멤버 변수 초기화, static 영역 실행 등을 수행한다.

class loader lazy loading (dynamic class loading)
- 일반적으로 위 과정으로 메모리에 로드된 클래스들은 lazy loading 방식으로 동작한다.
- jvm application 이 구동되고 실제 해당 클래스에 대한 요청이 들어온 경우 해당 클래스를 로드한다.
- 위 lazy loading 이 jvm application 의 배포 직후 latancy 의 원인이 된다.

JIT compiler (hotspot, dynamic translation)
- 애플리케이션 런타임에 bytecode를 machine-code로 컴파일하는 기법이다.
- interprete 시 machine-code 를 캐싱하여 이후 반복되는 machine-code 변환시 사용한다.
- 애플리케이션 구동 직후 JIT compiler 의 machine-code 캐시는 비어있는 상태이므로 interprete 단계에서 지연이 발생된다.

JVM wram up
- class loading 은 lazy 방식으로 동작하고, 배포직후 메모리에 클래스 파일이 로드되지 않은 상태.
- jit comiler 는 machine-code 를 캐싱하고 최적화하여 interprete 성능을 향상시키지만 배포 직후에는 캐시가 비어있고, 최적화된 machine-code 또한 없는 상태.
- 위 이유로 jvm application 은 배포 직후 낮은 성능을 갖게 된다.
- 개선을 위해 warn up 단계를 따로 두어 애플리케이션 구동시 빈번하게 호출되는 코드(api)에 대한 호출을 수행한다.
- spring boot application 의 경우 ApplicationRunner 에서 빈번하게 호출되는 서비스 로직 호출을 생각해볼 수 있다.


참고
- https://www.youtube.com/watch?v=CQi3SS2YspY
- https://tecoble.techcourse.co.kr/post/2021-07-15-jvm-classloader/
- https://ko.wikipedia.org/wiki/JIT_%EC%BB%B4%ED%8C%8C%EC%9D%BC