# TCG Arena - Shop Population Guide

Questo documento spiega come popolare il database dei negozi TCG usando Google Places API.

## 🎯 Metodi Disponibili

Ci sono **2 modi** per popolare il database:

### 1. **Endpoint Java Backend** (Consigliato per produzione)
### 2. **Script Python Standalone** (Consigliato per test e sviluppo)

---

## 🔧 Prerequisiti

### Google Places API Key

1. Vai su [Google Cloud Console](https://console.cloud.google.com/)
2. Crea un nuovo progetto o seleziona uno esistente
3. Abilita le seguenti API:
   - **Places API**
   - **Places API (New)** (opzionale, per features avanzate)
4. Vai su "Credentials" e crea una API Key
5. **IMPORTANTE**: Restrizioni consigliate per sicurezza:
   - Application restrictions: HTTP referrers o IP addresses
   - API restrictions: Solo Places API

### Costi Google Places API

- **Nearby Search**: $32 per 1000 richieste
- **Place Details**: $17 per 1000 richieste
- **Free tier**: $200 di crediti mensili

**Stima per questo script**:
- ~70 città × 9 keywords = ~630 Nearby Search
- ~100-500 Place Details (dipende dai risultati)
- **Costo totale stimato**: $30-50 (coperto dal free tier)

---

## 📋 Metodo 1: Endpoint Java Backend

### Setup

1. **Aggiungi la chiave API in `application.properties`:**

```properties
# Google Places API
google.places.api.key=YOUR_GOOGLE_PLACES_API_KEY_HERE
```

2. **Avvia il backend:**

```bash
cd TCGArena-BE
./mvnw spring-boot:run
```

### Utilizzo

#### Test in Dry-Run Mode (consigliato prima)

```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?dryRun=true" \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN"
```

Questo mostrerà cosa verrebbe inserito senza modificare il database.

#### Esecuzione Reale

```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?dryRun=false" \
  -H "Authorization: Bearer YOUR_ADMIN_JWT_TOKEN"
```

### Risposta

```json
{
  "totalFound": 450,
  "totalInserted": 320,
  "totalSkipped": 130,
  "dryRun": false,
  "errors": []
}
```

---

## 🐍 Metodo 2: Script Python Standalone

### Setup

1. **Installa dipendenze:**

```bash
pip install requests mysql-connector-python
```

2. **Rendi eseguibile lo script:**

```bash
chmod +x populate_shops_script.py
```

### Utilizzo

#### Opzione A: Con parametri da linea di comando

```bash
python populate_shops_script.py \
  --api-key YOUR_GOOGLE_PLACES_API_KEY \
  --db-url "jdbc:mysql://localhost:3306/tcg_arena" \
  --db-user root \
  --db-password your_password \
  --dry-run
```

#### Opzione B: Con variabili d'ambiente

```bash
export GOOGLE_PLACES_API_KEY=your_api_key
export DB_URL=jdbc:mysql://localhost:3306/tcg_arena
export DB_USER=root
export DB_PASSWORD=your_password

# Dry run
python populate_shops_script.py --dry-run

# Esecuzione reale
python populate_shops_script.py
```

### Output

```
🔍 DRY RUN MODE - No data will be inserted

Starting shop population for 70 cities...
Using 9 search keywords

📍 Searching in: Roma, Italia
  ✓ Would insert: GameStop - Via del Corso
  ✓ Would insert: Magic Store Roma - Via Nazionale
  ⊘ Already exists: Multiplayer.com

📍 Searching in: Milano, Italia
  ✓ Would insert: Milan Games Week Store
  ...

============================================================
📊 SUMMARY
============================================================
Total shops found: 450
Total inserted: 320
Total skipped: 130
Mode: DRY RUN
============================================================
```

---

## 🌍 Città Coperte

Lo script cerca negozi nelle seguenti città europee:

### 🇮🇹 Italia (10 città)
- Roma, Milano, Napoli, Torino, Firenze
- Bologna, Venezia, Genova, Palermo, Bari

### 🇫🇷 Francia (5 città)
- Paris, Lyon, Marseille, Toulouse, Nice

### 🇪🇸 Spagna (4 città)
- Madrid, Barcelona, Valencia, Sevilla

### 🇩🇪 Germania (4 città)
- Berlin, Munich, Hamburg, Frankfurt

### 🇬🇧 UK (3 città)
- London, Manchester, Birmingham

### Altri paesi
- Netherlands, Belgium, Portugal, Austria, Switzerland, Poland, Czech Republic, Greece

**Totale: 70+ città principali**

---

## 🔍 Keywords di Ricerca

Lo script usa queste keywords per trovare negozi:

```
- "pokemon card shop"
- "magic the gathering shop"
- "yugioh card shop"
- "trading card game shop"
- "TCG shop"
- "collectible card game store"
- "fumetteria carte"
- "negozio carte pokemon"
- "negozio giochi da tavolo carte"
```

---

## 📊 Dati Estratti per Ogni Negozio

- ✅ Nome
- ✅ Indirizzo completo
- ✅ Coordinate (latitudine, longitudine)
- ✅ Numero di telefono (se disponibile)
- ✅ Sito web (se disponibile)
- ✅ TCG types supportati (rilevati dal nome)
- ✅ Servizi di default

### Campi Impostati Automaticamente

```java
type: STORE
isVerified: false  // Da verificare manualmente
active: false      // Da attivare manualmente dopo verifica
tcgTypes: rilevati dal nome o "ALL" se non specificato
services: BUY_CARDS,SELL_CARDS,TRADE,TOURNAMENTS,etc.
```

---

## ⚙️ Configurazioni Avanzate

### Modificare il Raggio di Ricerca

Nel file Java:
```java
List<Shop> shops = searchNearbyShops(lat, lng, keyword, 15000); // 15km
```

Nello script Python:
```python
def search_nearby_shops(api_key, lat, lng, keyword, radius=15000):
```

### Aggiungere Altre Città

Nel service Java (`GooglePlacesService.java`):
```java
Map.entry("Verona, Italia", new double[]{45.4384, 10.9916}),
```

Nello script Python:
```python
"Verona, Italia": (45.4384, 10.9916),
```

### Aggiungere Keywords

```java
"card game tournament",
"fumetteria giochi",
```

---

## 🛡️ Best Practices

### 1. **Sempre iniziare con Dry Run**
```bash
# Test prima
python populate_shops_script.py --dry-run

# Poi esegui
python populate_shops_script.py
```

### 2. **Rate Limiting**
Lo script include automaticamente dei delay per rispettare i limiti di Google:
- 1 secondo tra keyword searches
- 0.5 secondi tra place details

### 3. **Verificare i Dati Inseriti**

Dopo l'esecuzione, verifica nel database:
```sql
-- Conta negozi inseriti
SELECT COUNT(*) FROM shops WHERE is_verified = false;

-- Visualizza nuovi negozi per paese
SELECT 
    SUBSTRING_INDEX(address, ',', -1) as country,
    COUNT(*) as count
FROM shops
WHERE is_verified = false
GROUP BY country;

-- Controlla duplicati
SELECT name, address, COUNT(*) as count
FROM shops
GROUP BY name, address
HAVING count > 1;
```

### 4. **Attivazione Manuale**

I negozi vengono inseriti come **inattivi e non verificati**. Devi:

1. Verificare manualmente i dati
2. Impostare `is_verified = true`
3. Impostare `active = true`

```sql
-- Attiva negozi verificati
UPDATE shops 
SET active = true, is_verified = true 
WHERE id IN (1,2,3...);
```

---

## 🐛 Troubleshooting

### Errore: "Google Places API key not configured"
```bash
# Verifica che la chiave sia in application.properties
cat src/main/resources/application.properties | grep google.places
```

### Errore: "OVER_QUERY_LIMIT"
- Hai superato il limite di richieste gratuite
- Aggiungi metodo di pagamento su Google Cloud
- Oppure riduci le città/keywords

### Errore: "REQUEST_DENIED"
- Verifica che Places API sia abilitata nel progetto
- Controlla che la API Key sia corretta
- Verifica le restrizioni sulla key

### Molti duplicati
- Aumenta la precisione del check di esistenza
- Modifica `calculateDistance` per essere più stringente

---

## 📈 Monitoraggio

### Log del Backend

```bash
tail -f logs/spring-boot-logger.log | grep GooglePlacesService
```

### Metriche

```sql
-- Negozi per TCG
SELECT tcg_types, COUNT(*) 
FROM shops 
GROUP BY tcg_types;

-- Negozi per città
SELECT 
    SUBSTRING_INDEX(address, ',', -1) as country,
    COUNT(*) as total,
    SUM(CASE WHEN active = true THEN 1 ELSE 0 END) as active
FROM shops
GROUP BY country
ORDER BY total DESC;
```

---

## 🔄 Aggiornamenti Futuri

Per aggiornare i dati dei negozi esistenti:

1. Modifica lo script per fare UPDATE invece che INSERT
2. Aggiungi un campo `last_updated_from_google` timestamp
3. Re-esegui periodicamente per aggiornare telefoni/indirizzi

---

## 💡 Suggerimenti

1. **Esegui in orari di basso traffico** per evitare problemi di performance
2. **Monitora i costi** su Google Cloud Console
3. **Backup del database** prima di eseguire in modalità live
4. **Testa su ambiente di staging** prima di produzione
5. **Considera l'uso di cache** per place details già richiesti

---

## 📞 Supporto

Per problemi o domande:
- Check i logs in `logs/spring-boot-logger.log`
- Verifica Google Cloud Console per errori API
- Controlla le query MySQL per duplicati

---

## ✨ Features Future

- [ ] Integrazione con altri servizi (Yelp, Facebook Places)
- [ ] Machine learning per rilevare TCG types più accuratamente
- [ ] Estrazione automatica orari di apertura
- [ ] Download foto dei negozi
- [ ] Validazione automatica numeri di telefono
- [ ] Geocoding inverso per normalizzare indirizzi
