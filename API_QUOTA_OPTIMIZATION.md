# Google Places API - Gestione Quota e Ottimizzazioni

## 🎯 Limiti Google Places API

### Free Tier
- **$200 di crediti mensili gratuiti**
- **Nearby Search**: $32 per 1000 richieste
- **Place Details**: $17 per 1000 richieste

### Calcolo Richieste Script Completo

Senza limiti:
```
70 città × 9 keywords = 630 Nearby Search
~300 negozi trovati × 1 Place Details = 300 Place Details
──────────────────────────────────────────────────────
TOTALE: ~930 richieste
```

Con tutti i negozi:
```
70 città × 9 keywords = 630 Nearby Search  
~500 negozi trovati × 1 Place Details = 500 Place Details
──────────────────────────────────────────────────────
TOTALE: ~1130 richieste ❌ SUPERA IL LIMITE!
```

---

## ✅ Nuove Features per Gestire la Quota

### 1. Limite Massimo Richieste (Default: 950)

**Java Endpoint:**
```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?dryRun=false&maxRequests=950"
```

**Python Script:**
```bash
python populate_shops_script.py --max-requests 950
```

Lo script si ferma automaticamente quando raggiunge il limite.

### 2. Skip Place Details (Risparmia ~30-50% delle richieste)

Se non hai bisogno di telefono/website/orari dettagliati:

**Java Endpoint:**
```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?skipPlaceDetails=true"
```

**Python Script:**
```bash
python populate_shops_script.py --skip-details
```

Con `skipPlaceDetails=true`:
```
630 Nearby Search + 0 Place Details = 630 richieste ✅
Risparmio: 300+ chiamate API!
```

### 3. Monitoraggio Real-Time

Il sistema traccia e mostra:
- Chiamate Nearby Search
- Chiamate Place Details  
- Totale vs Limite
- Si ferma automaticamente al raggiungimento del limite

**Output:**
```json
{
  "totalFound": 450,
  "totalInserted": 320,
  "totalSkipped": 130,
  "apiRequestsUsed": 890,
  "nearbySearchCalls": 630,
  "placeDetailsCalls": 260,
  "requestLimit": 950
}
```

---

## 📊 Strategie di Ottimizzazione

### Strategia 1: Solo Nearby Search (Veloce, Economico)
```bash
# 630 richieste totali
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?skipPlaceDetails=true"
```

**Pro:**
- ✅ Veloce (nessun delay per Place Details)
- ✅ Economico (solo $20 invece di $35)
- ✅ Ottieni già: nome, indirizzo, coordinate, rating

**Contro:**
- ❌ Nessun telefono
- ❌ Nessun website
- ❌ Nessuna descrizione dettagliata
- ❌ Nessun orario strutturato

### Strategia 2: Limit a 500 con Place Details
```bash
# 500 richieste totali (~250 nearby + ~250 details)
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?maxRequests=500"
```

**Pro:**
- ✅ Dati completi per i negozi trovati
- ✅ Sotto il limite free tier
- ✅ Copre ~35-40 città

**Contro:**
- ❌ Non copre tutta Europa
- ❌ Devi eseguire lo script più volte

### Strategia 3: Multi-Pass (Consigliato)

**Pass 1:** Nearby Search su tutta Europa (630 richieste)
```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?skipPlaceDetails=true&maxRequests=630"
```

**Pass 2:** Arricchisci solo negozi italiani con Place Details
```sql
-- Seleziona shop IDs italiani
SELECT id, place_id FROM shops WHERE address LIKE '%Italia%' AND phone_number IS NULL;
```

Poi crea uno script dedicato per arricchire solo quelli.

### Strategia 4: Ridurre Keywords

Invece di 9 keywords, usa solo le più efficaci:
```java
private static final List<String> SEARCH_KEYWORDS = Arrays.asList(
    "pokemon card shop",
    "magic the gathering shop",
    "TCG shop",
    "fumetteria carte"  // Solo per Italia
);
```

```
70 città × 4 keywords = 280 Nearby Search
~200 negozi × 1 Place Details = 200 Place Details
──────────────────────────────────────────────
TOTALE: 480 richieste ✅
```

### Strategia 5: Solo Città Principali

Modifica `EUROPEAN_CITIES` per includere solo le top 30 città:
```java
// Solo capitali e città >1M abitanti
private static final Map<String, double[]> EUROPEAN_CITIES = Map.ofEntries(
    Map.entry("Roma, Italia", new double[]{41.9028, 12.4964}),
    Map.entry("Milano, Italia", new double[]{45.4642, 9.1900}),
    Map.entry("Paris, France", new double[]{48.8566, 2.3522}),
    Map.entry("Berlin, Germany", new double[]{52.5200, 13.4050}),
    Map.entry("Madrid, Spain", new double[]{40.4168, -3.7038}),
    Map.entry("London, UK", new double[]{51.5074, -0.1278}),
    // ... solo 30 città invece di 70
);
```

```
30 città × 9 keywords = 270 Nearby Search
~200 negozi × 1 Place Details = 200 Place Details
──────────────────────────────────────────────
TOTALE: 470 richieste ✅
```

---

## 🚀 Raccomandazioni Pratiche

### Per Sviluppo/Test
```bash
# Dry run con limite basso
python populate_shops_script.py --dry-run --max-requests 50 --skip-details
```

### Per Prima Popolazione Completa
```bash
# Strategia Multi-Pass consigliata

# 1. Prima passata: solo nearby search
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true"

# 2. Attendi nuovo mese per refresh crediti (o paga)
# 3. Arricchisci con place details in batch
```

### Per Aggiornamenti Periodici
```bash
# Solo città principali con dettagli completi
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?dryRun=false&maxRequests=500"
```

---

## 📈 Monitoraggio Costi

### Calcolo Costo Esatto

```python
# Nearby Search
nearby_cost = (nearby_calls / 1000) * 32

# Place Details  
details_cost = (details_calls / 1000) * 17

# Totale
total_cost = nearby_cost + details_cost

print(f"Costo totale: ${total_cost:.2f}")
```

### Esempio con 950 richieste (630 nearby + 320 details)
```
Nearby: (630 / 1000) * $32 = $20.16
Details: (320 / 1000) * $17 = $5.44
─────────────────────────────────────
TOTALE: $25.60 ✅ Sotto i $200 free tier
```

---

## 🔧 Configurazioni Custom

### application.properties
```properties
# Limite default per tutti i run
google.places.max.requests=950

# Skip details di default
google.places.skip.details=false
```

### Variabili d'Ambiente
```bash
export GOOGLE_PLACES_MAX_REQUESTS=500
export GOOGLE_PLACES_SKIP_DETAILS=true
```

---

## 💡 Best Practices

1. **Inizia sempre con dry-run**
   ```bash
   --dryRun=true --maxRequests=50
   ```

2. **Monitora i costi su Google Cloud Console**
   - Vai su "Billing" → "Reports"
   - Filtra per "Places API"

3. **Imposta budget alerts**
   - Google Cloud → Billing → Budgets
   - Alert a $50, $100, $150

4. **Log delle richieste**
   ```bash
   tail -f logs/spring-boot-logger.log | grep "API calls"
   ```

5. **Esegui in orari di basso traffico**
   - Meno probabilità di timeout
   - Migliori performance

---

## 🎯 Esempi Pratici

### Solo Italia con Dettagli Completi
```bash
# Modifica EUROPEAN_CITIES per includere solo città italiane
# Poi:
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?maxRequests=200"
```

```
10 città IT × 9 keywords = 90 Nearby
~60 negozi × 1 Details = 60 Details
──────────────────────────────────
TOTALE: 150 richieste (costo ~$5)
```

### Europa Completa Senza Dettagli
```bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?skipPlaceDetails=true"
```

```
70 città × 9 keywords = 630 richieste
Costo: ~$20
```

### Test su 1 Città
```bash
# Modifica temporaneamente per includere solo Roma
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google?maxRequests=50&dryRun=true"
```

---

## ⚠️ Limiti da Ricordare

- Google Places ha anche **limiti di rate** (es. max X richieste/secondo)
- Il free tier è **mensile** ($200/mese)
- I crediti **non si accumulano** mese dopo mese
- Dopo i $200, vieni **automaticamente addebitato** se hai metodo di pagamento

---

## 🔄 Automazione Mensile

Per popolare automaticamente ogni mese:

```bash
# Cron job
0 2 1 * * /path/to/populate_shops.sh

# populate_shops.sh
#!/bin/bash
curl -X POST "http://localhost:8080/api/admin/shops/populate-from-google" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d "skipPlaceDetails=true" \
  -d "maxRequests=630"
```

Questo ti costa $20/mese e mantiene il database aggiornato!
