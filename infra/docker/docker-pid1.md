# Docker PID 1

## PID 1 ..

- linux 는 부팅시 커널이 가장 먼저 띄우는 프로세스에 PID 1 가 부여됨
    - `init` `systemd` …
- PID 1 프로세스는 고아 프로세스가 생기는 경우 해당 고아 프로세스의 부모 프로세스가 됨
- PID 1 프로세스는 시그널 동작을 적용받지 않음.
- 일반적으로 `SIGTERM`  등 signal 을 받은 프로세스는 별도 핸들러가 등록되어 있지 않아도 `기본 동작`에 의해 종료됨 (`kill`)
    - PID 1 은 이 `기본 동작`  이 비활성화
    - PID 1 가 의도치 않게 죽는 것을 방지 위함
- PID 1 프로세스는 명시적으로 핸들러를 등록한 시그널만 받는다.
    - `SIGKILL` `SIGSTOP` 등 강제종료 시그널은 얘외적으로 받아서 종료 처리함
- `docker stop` 등으로 컨테이너 중단시 docker 는 container 내 PID 1 프로세스로 SIGTERM 을 보냄
    - 별도 SIGTERM 핸들러가 없다면 적용안됨

## ENTRYPOINT 작성시 주의

## shell form

```
ENTRYPOINT java -jar application.jar
```

- —> `/bin/sh -c "java -jar application.jar"`
    - 파이프, 환경변수 등 섞일 수 있으므로 sh 에게 해석 요청
- `/bin/sh` 가 PID 1번 프로세스 되고 java 는 PID 1번의 자식 프로세스가 됨

```
$ docker exec <컨테이너> ps -ef
PID  PPID  CMD
  1     0  /bin/sh -c java -jar application.jar   ← sh가 PID 1
  7     1  java -jar application.jar              ← java는 sh의 자식
```

- docker stop 시 SIGTERM 은 PID 1 에게만 `SIGTERM` 시그널 보냄
- PID 1 번인 `sh` 는 `SIGTERM` 을 자식에게 넘겨주는 로직 없음.
    - + PID 1번이므로 본인을 종료하는 기본 동작 없음.
    - 결국 n 초(default: 10초) 이후 docker 에 의해 `SIGKILL`  처리되어 별도 정리작업 (graceful shutdown) 수행안됨

## exec form

```
ENTRYPOINT ["java", "-jar", "application.jar"]
```

- shell form 과 비교하여 문법만 달라진 것 같지만 docker 에서 처리방식이 완전히 다름
- java 자체가 PID 1 프로세스로 실행됨
- docker stop 시 PID 1 프로세스로 `SIGTERM` 시그널을 보내고 java 는 해당 시그널을 shutdown hook 으로 하여 정리작업(graceful shutdown) 수행

## PID 1 프로세스에 위치시킬 수 없는 상황이라면?

https://github.com/krallin/tini

- tini 가 PID 1 프로세스로 실행하는 경우 자식 프로세스에 SIGTERM 을 전달한다.
    - 기본적으로는 1번 자식 프로세스에만 전달하므로 모든 프로세스에 SIGTERM 전달 필요시 `-g` 옵신 필요

```
FROM node:24-bookworm-slim AS deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci

FROM node:24-bookworm-slim AS build
WORKDIR /app
COPY --from=deps /app/node_modules ./node_modules
COPY . .
RUN npx nest build

FROM node:24-bookworm-slim AS prod-deps
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --omit=dev

FROM node:24-bookworm-slim AS runner
ENV ENV=prod
ENV PORT=3000
RUN apt-get update \
  && apt-get install -y --no-install-recommends tini \
  && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --chown=node:node --from=prod-deps /app/node_modules ./node_modules
COPY --chown=node:node --from=build /app/dist ./dist
COPY --chown=node:node package.json ./
COPY --chown=node:node .env .env.dev .env.prod .env.test ./
USER node
EXPOSE 3000
ENTRYPOINT ["/usr/bin/tini", "-g", "--"]
CMD ["sh", "-c", "exec ./node_modules/.bin/dotenvx run -f .env.$ENV -f .env -- node dist/main"]

```