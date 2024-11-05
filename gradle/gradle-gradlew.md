gradle, gradlew

gradle buld시 `Gradle Wrapper` 를 사용하는 것을 권장한다.
- https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html
- /gradle/wrapper
  - gradle-wrapper.jar
  - gradle-wrapper.properties

`gradlew` 는 build 시 `Gradle Wrapper` 를 사용한다.
- 해당 환경에 gradle 이 설치되어 있지 않은 경우 `gradle wrapper script` 에 지정된 version 등을 기반으로 gradle 설치 후 build 를 수행한다.

./gradlew build
- linux, osx

./gradlew.bat build
- windows


ref
- https://docs.gradle.org/current/userguide/gradle_wrapper_basics.html2