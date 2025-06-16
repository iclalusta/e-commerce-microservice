# Adım 1: Projeyi derlemek için Maven ve JDK içeren bir "build" ortamı kullanıyoruz.
FROM maven:3.8.5-openjdk-17 AS build

# Proje dosyalarını konteynerin içine kopyalıyoruz.
WORKDIR /app
COPY . .

# Maven kullanarak projeyi derleyip çalıştırılabilir .jar dosyasını oluşturuyoruz.
RUN mvn clean package -DskipTests

# Adım 2: Sadece uygulamayı çalıştırmak için daha küçük bir Java ortamı kullanıyoruz.
# Bu, son imajımızın boyutunu önemli ölçüde küçültür.
FROM openjdk:17-jdk-slim

# Çalışma dizinini ayarlıyoruz.
WORKDIR /app

# Derlenmiş .jar dosyasını build ortamından bu yeni ortama kopyalıyoruz.
COPY --from=build /app/target/api-gateway-0.0.1-SNAPSHOT.jar app.jar

# API Gateway'in çalışacağı 8080 portunu dışarıya açıyoruz.
EXPOSE 8080

# Konteyner başladığında çalıştırılacak varsayılan komut.
ENTRYPOINT ["java", "-jar", "/app.jar"]