## Gradle 

#### Gradle Daemon

빌드시 시간을 줄여주는 백그라운드 프로세스

실행중인 Gradle Daemon 상태확인
```
gradle --status
```

Gradle daemon 메모리
- 최대 힙 크기=512mb (jvm 기본 최소 힙 크기)

Gradle daemon 사용 방지
- // gradle daemon 이 빌드시 에러의 원인인지 확인
```
--no-daemon
```

#### gradle build 시 heap 사이즈 조정

`gradle.properties` 조정
- Gradle daemon 의 힙 사이즈 조정에 대한 설정

```
org.gradle.jvmargs=-Xmx512m
```

실제 빌드시 사용되는 `gradle test worker` 의 heap 사이즈 조정 <br/>
```
test {
  maxHeapSize = "1024m" // default 512m
}
```

---

### ref
https://docs.gradle.org/current/userguide/gradle_daemon.html <br/>
