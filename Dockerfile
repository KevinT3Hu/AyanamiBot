FROM mcr.microsoft.com/playwright/java:v1.42.0-jammy
# install jre 17
COPY build/libs/AyanamiBot.jar /app/AyanamiBot.jar
CMD ["java", "-jar", "/app/AyanamiBot.jar"]