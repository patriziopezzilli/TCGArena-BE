# TCG Arena Backend - Logging Configuration

## Sistema di Logging

Il progetto utilizza SLF4J con Logback come implementazione di logging per la produzione.

### Dipendenze

- `slf4j-api`: API di logging
- `logback-classic`: Implementazione Logback

### Configurazione

Il file `src/main/resources/logback-spring.xml` contiene la configurazione di produzione:

- **Console logging**: Per sviluppo con pattern dettagliato
- **File logging**: Log principali salvati in `./logs/tcg-arena.log`
- **Error logging**: Errori separati in `./logs/tcg-arena-error.log`
- **Rotazione**: File ruotati giornalmente, mantenuti per 30 giorni
- **Limite dimensione**: 50MB per file, massimo 1GB totali per log principali, 500MB per errori

### Livelli di Logging

- **ROOT**: INFO
- **Spring Framework**: WARN (eccetto security: INFO)
- **Hibernate**: WARN (SQL: DEBUG, parametri: TRACE)
- **Applicazioni terze**: INFO
- **com.tcg.arena.security**: INFO (con file separato)
- **com.tcg.arena.batch**: INFO (con file separato)

### Utilizzo nel Codice

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class MyService {
    private static final Logger logger = LoggerFactory.getLogger(MyService.class);

    public void myMethod() {
        logger.debug("Debug message: {}", parameter);
        logger.info("Info message");
        logger.warn("Warning message: {}", issue);
        logger.error("Error message: {}", exception.getMessage(), exception);
    }
}
```

### Variabili d'Ambiente

- `LOG_DIR`: Directory per i file di log (default: `./logs`)
- `LOG_LEVEL`: Livello di logging globale (default: INFO)

### Monitoraggio in Produzione

I log vengono automaticamente:
- Ruotati giornalmente
- Compressi quando raggiungono la dimensione massima
- Eliminati dopo 30 giorni
- Separati per livello (errori in file dedicato)

### Debug in Sviluppo

Per abilitare il debug in sviluppo, aggiungere in `application.properties`:
```
logging.level.com.tcg.arena=DEBUG
logging.level.org.springframework.security=DEBUG
```