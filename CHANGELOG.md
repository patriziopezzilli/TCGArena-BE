# Changelog - TCG Arena Backend

Tutti i cambiamenti significativi al backend TCG Arena saranno documentati in questo file.

Il formato è basato su [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
e questo progetto aderisce a [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.0.0] - 2024-11-23

### Aggiunto
- **API REST completa** per gestione carte, mazzi, tornei e utenti
- **Autenticazione JWT** con Spring Security
- **Database PostgreSQL** con Hibernate/JPA
- **Documentazione API** con OpenAPI/Swagger
- **Gestione Carte**: CRUD operations per template carte e collezioni utente
- **Sistema Tornei**: Creazione, registrazione e gestione eventi
- **Marketplace**: Prezzi e scambi carte
- **Sistema Utenti**: Profili, statistiche, achievements
- **Integrazione TCG**: Supporto Pokemon, Magic, Yu-Gi-Oh!, One Piece
- **Deployment Render**: Configurazione produzione con database cloud

### Funzionalità API
- **Cards API**: `/api/cards/*` - Gestione template e collezioni
- **Decks API**: `/api/decks/*` - Costruzione e condivisione mazzi
- **Tournaments API**: `/api/tournaments/*` - Eventi e competizioni
- **Users API**: `/api/users/*` - Profili e autenticazione
- **Market API**: `/api/market/*` - Prezzi e marketplace
- **Admin API**: `/api/admin/*` - Operazioni amministrative

### Sicurezza
- **JWT Authentication**: Token-based security
- **Password Encoding**: BCrypt per hash password
- **CORS Configuration**: Cross-origin resource sharing
- **Rate Limiting**: Protezione contro abusi API

### Infrastruttura
- **Spring Boot 3.x**: Framework principale
- **PostgreSQL**: Database relazionale
- **Docker**: Containerizzazione
- **Maven**: Build e dependency management
- **GitHub Actions**: CI/CD pipeline
- **Render**: Platform as a Service per deployment

### Configurazioni
- **Multi-environment**: local, test, prod profiles
- **Database Migration**: Flyway per schema versioning
- **Monitoring**: Spring Boot Actuator per health checks
- **Logging**: Configurazione log strutturata

### Note
- Versione iniziale completa con tutte le API core
- Deployato su Render con database PostgreSQL
- Integrazione completa con app iOS
- Documentazione API completa con Swagger

## [0.1.0] - 2024-11-01

### Aggiunto
- Progetto iniziale Spring Boot TCG Arena Backend
- Setup base con Spring Security e JWT
- Configurazioni multi-ambiente
- Struttura progetto Maven
- Connessione database iniziale