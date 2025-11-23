# Contributing to TCG Arena Backend

Grazie per il tuo interesse nel contribuire al backend di TCG Arena! Questo documento fornisce le linee guida per contribuire al progetto Spring Boot.

## 🚀 Come Contribuire

### 1. Fork e Clone
```bash
git clone https://github.com/patriziopezzilli/TCGArena-BE.git
cd "TCG Arena - Backend"
```

### 2. Setup Ambiente di Sviluppo
```bash
# Assicurati di avere Java 17+ installato
java -version

# Usa il wrapper Maven incluso
./mvnw --version

# Compila il progetto
./mvnw clean compile
```

### 3. Crea un Branch
```bash
git checkout -b feature/nome-della-tua-feature
```

### 4. Sviluppa
- Segui le convenzioni di codice Java
- Aggiungi test unitari e di integrazione
- Assicurati che tutti i test passino: `./mvnw test`
- Aggiorna la documentazione API se necessario

### 5. Commit
```bash
git add .
git commit -m "feat: descrizione della feature aggiunta"
```

Usa i seguenti prefissi per i commit:
- `feat:` per nuove funzionalità API
- `fix:` per correzioni di bug
- `docs:` per aggiornamenti alla documentazione
- `style:` per modifiche di stile/formattazione
- `refactor:` per refactoring del codice
- `test:` per aggiunta/modifica di test
- `chore:` per aggiornamenti di configurazione

### 6. Push e Pull Request
```bash
git push origin feature/nome-della-tua-feature
```

## 📋 Requisiti per le Pull Request

- [ ] Il codice compila senza errori (`./mvnw compile`)
- [ ] Tutti i test passano (`./mvnw test`)
- [ ] Il codice segue le convenzioni Java/Spring Boot
- [ ] La documentazione API è aggiornata
- [ ] I cambiamenti sono testati localmente
- [ ] Nessun segreto o configurazione sensibile è committato

## 🏗️ Setup Ambiente di Sviluppo

### Prerequisiti
- Java 17+ (OpenJDK o Oracle JDK)
- Maven 3.6+ (o usa il wrapper `./mvnw`)
- PostgreSQL 12+ (per sviluppo locale)
- Docker (opzionale, per containerizzazione)

### Configurazione Database
```bash
# Crea un database PostgreSQL locale
createdb tcgarena_dev

# Oppure usa Docker
docker run --name postgres-tcgarena -e POSTGRES_DB=tcgarena_dev -e POSTGRES_USER=tcgarena -e POSTGRES_PASSWORD=password -p 5432:5432 -d postgres:13
```

### File di Configurazione
- `application-local.properties`: Configurazione per sviluppo locale
- `application-test.properties`: Configurazione per test
- `application-prod.properties`: Configurazione per produzione

### Esecuzione
```bash
# Sviluppo locale
./mvnw spring-boot:run -Dspring-boot.run.profiles=local

# Con debug abilitato
./mvnw spring-boot:run -Dspring-boot.run.profiles=local -Dspring-boot.run.jvmArguments="-Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005"

# Test
./mvnw test

# Package
./mvnw clean package
```

## 🐛 Segnalazione Bug

Usa il template delle Issues di GitHub per segnalare bug, includendo:
- Descrizione del problema
- Passi per riprodurre
- Log dell'applicazione
- Versione Java/Maven
- Ambiente (locale, produzione, ecc.)

## 📚 Documentazione API

L'API è documentata usando OpenAPI/Swagger. Una volta avviata l'applicazione:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## 🧪 Testing

```bash
# Test unitari
./mvnw test -Dtest="*Test"

# Test di integrazione
./mvnw test -Dtest="*IT"

# Coverage report
./mvnw test jacoco:report
```

## 🚀 Deployment

### Locale
```bash
./mvnw clean package
java -jar target/*.jar --spring.profiles.active=local
```

### Docker
```bash
# Build immagine
docker build -t tcgarena-backend .

# Run container
docker run -p 8080:8080 tcgarena-backend
```

### Produzione (Render)
Il deployment in produzione avviene automaticamente tramite GitHub Actions o manualmente su Render.

## 💡 Suggerimenti per Nuove Features

Prima di implementare nuove API, crea una Issue per discutere l'idea con il team.

## 📞 Contatti

Per domande o chiarimenti, apri una Issue o contatta il maintainer del progetto.

Grazie per contribuire a TCG Arena Backend! 🎮