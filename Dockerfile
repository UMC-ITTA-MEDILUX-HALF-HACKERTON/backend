FROM eclipse-temurin:17-jdk AS build
WORKDIR /app

# 의존성 레이어를 먼저 받아 캐시한다
COPY gradlew ./
COPY gradle/ gradle/
COPY build.gradle settings.gradle ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon > /dev/null 2>&1 || true

COPY src/ src/
# Boot 플러그인은 *-plain.jar 도 같이 만든다. 지워야 아래 COPY 와일드카드가 하나만 집는다
RUN ./gradlew bootJar --no-daemon -x test && rm -f build/libs/*-plain.jar

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# nc 는 busybox 에 있다. compose 헬스체크가 포트 오픈 여부를 본다
COPY --from=build /app/build/libs/*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
