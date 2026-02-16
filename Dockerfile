FROM eclipse-temurin:8-jdk

RUN useradd -ms /bin/bash appuser

RUN mkdir -p /opt

WORKDIR /opt

COPY content-validation-service-0.0.1-SNAPSHOT.jar /opt/
RUN chown -R appuser:appuser /opt

USER appuser

EXPOSE 9050
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xmx512m -jar /opt/content-validation-service-0.0.1-SNAPSHOT.jar"]

