# Fix: Gestione Duplicati nell'Import JustTCG

**Data:** 20 Dicembre 2025  
**Issue:** Import JustTCG non salvava l'offset e permetteva la creazione di duplicati

## Problemi Risolti

### 1. ✅ Offset non salvato al completamento
**Prima:** Quando l'import completava, l'offset veniva resettato a 0
```java
updateProgress(tcgType, 0, true);  // ❌ Reset a 0
```

**Dopo:** L'offset finale viene mantenuto
```java
updateProgress(tcgType, response.currentOffset, true);  // ✅ Mantiene offset
```

### 2. ✅ Offset non salvato in caso di errore
**Prima:** Se una pagina falliva dopo averne processate altre, l'offset non veniva salvato

**Dopo:** Gestione errori con `onErrorResume` che preserva il progresso già salvato

### 3. ✅ Prevenzione duplicati Expansion
**Modifiche:**
- Aggiunto constraint unique su `(title, tcg_type)` nel model
- Reso `getOrCreateExpansion()` sincronizzato
- Gestione `ConstraintViolationException` con fallback a record esistente

### 4. ✅ Prevenzione duplicati CardTemplate
**Modifiche:**
- Aggiunto constraint unique su `(name, set_code, card_number)` nel model
- Gestione `ConstraintViolationException` in `saveCardIfNotExists()`
- Aggiornamento prezzi in caso di duplicato invece di errore

### 5. ✅ TCGSet già protetto
- Constraint `unique = true` su `setCode` già presente

## File Modificati

### Model
1. [`Expansion.java`](src/main/java/com/tcg/arena/model/Expansion.java)
   - Aggiunto `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"title", "tcg_type"}))`
   - Aggiunto `@Column(name = "tcg_type")` per mapping corretto

2. [`CardTemplate.java`](src/main/java/com/tcg/arena/model/CardTemplate.java)
   - Aggiunto `@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"name", "set_code", "card_number"}))`
   - Aggiunto `@Column(name = "set_code")` e `@Column(name = "card_number")` per mapping

### Service
3. [`JustTCGApiClient.java`](src/main/java/com/tcg/arena/service/JustTCGApiClient.java)
   - Fix offset al completamento (riga ~446)
   - Aggiunto `onErrorResume` per gestire errori senza perdere progresso
   - `getOrCreateExpansion()` ora è `synchronized` con try-catch per constraint violation
   - `saveCardIfNotExists()` gestisce constraint violation con retry e update

### Controller
4. [`AdminController.java`](src/main/java/com/tcg/arena/controller/AdminController.java)
   - Aggiunto endpoint diagnostico `/api/admin/diagnostics/check-duplicates`

### Database
5. [`V27__add_unique_constraints_for_duplicates_prevention.sql`](src/main/resources/db/migration/V27__add_unique_constraints_for_duplicates_prevention.sql)
   - Migration Flyway per aggiungere constraint unique a DB esistenti

6. [`check_duplicates_before_V27.sql`](src/main/resources/db/migration/check_duplicates_before_V27.sql)
   - Script diagnostico per verificare duplicati esistenti PRIMA della migration

## Come Gestisce i Duplicati Ora

### Scenario: Offset sbagliato e reimport

1. **Expansion duplicata:**
   - Controllo cache → se esiste, ritorna quella
   - Controllo DB per `title + tcgType` → se esiste, ritorna quella
   - Tentativo creazione → se constraint violation, ricarica da DB
   - **Risultato:** Nessun duplicato creato, riutilizza record esistente

2. **TCGSet duplicato:**
   - Controllo cache per `setCode` → se esiste, ritorna quello
   - Controllo DB per `setCode` → se esiste, ritorna quello  
   - Tentativo creazione → **fallisce per constraint unique già presente**
   - **Risultato:** Nessun duplicato creato

3. **CardTemplate duplicato:**
   - Controllo DB per `(name, setCode, cardNumber)` → se esiste, aggiorna solo i prezzi
   - Tentativo creazione → se constraint violation, ricarica e aggiorna prezzi
   - **Risultato:** Nessun duplicato creato, prezzi aggiornati

## Istruzioni Deployment

### 1. Prima di deployare in produzione:
```bash
# Eseguire lo script di controllo duplicati
mysql -u user -p database < src/main/resources/db/migration/check_duplicates_before_V27.sql
```

### 2. Se vengono trovati duplicati:
- Rivedere i duplicati manualmente
- Decidere quale record mantenere (generalmente il più vecchio per ID)
- Eseguire le query di cleanup (decommentate nello script)

### 3. Deploy:
- Flyway eseguirà automaticamente V27 migration
- I constraint unique verranno applicati al database
- L'applicazione gestirà automaticamente eventuali collision future

## Test Consigliati

1. **Test import da offset 0 su database vuoto** → dovrebbe importare tutto
2. **Test reimport da offset 0 su database popolato** → non dovrebbe creare duplicati
3. **Test resume da offset intermedio** → dovrebbe riprendere correttamente
4. **Test import interrotto e ripreso** → offset salvato correttamente
5. **Test import con errore su una pagina** → offset salvato fino alla pagina riuscita

## Logging Migliorato

Nuovi log per debugging:
```
logger.debug("Progress saved at offset: {}", newOffset);
logger.info("Import completed at offset: {}", response.currentOffset);
logger.debug("Card already exists (constraint violation), updating prices: {}", card.name);
logger.debug("Expansion already exists (constraint violation), fetching: {}", name);
```

## Compatibilità

- ✅ Backward compatible: le modifiche non rompono import esistenti
- ✅ Database migration: Flyway gestisce automaticamente V27
- ✅ Cache: pulizia automatica alla fine di ogni import
- ✅ Concorrenza: metodi sincronizzati prevengono race conditions
