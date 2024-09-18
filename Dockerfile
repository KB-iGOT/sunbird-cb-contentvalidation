FROM openjdk:8
COPY content-validation-service-0.0.1-SNAPSHOT.jar /opt/
EXPOSE 9050
CMD ["/bin/bash", "-c", "java -XX:+PrintFlagsFinal $JAVA_OPTIONS -XX:+UnlockExperimentalVMOptions -XX:+UseCGroupMemoryLimitForHeap -Xmx512m -jar /opt/content-validation-service-0.0.1-SNAPSHOT.jar"]

