# TCG Arena - Comandi cURL Pubblici per Shop Population

## 🌐 Endpoint Pubblico (con Secret Key)

L'endpoint è ora **pubblico** ma protetto con una **secret key** configurabile.

---

## ✅ Test Status

```bash
curl -X GET "https://api.tcgarena.it/api/admin/shops/google-places-status"
```

**Response:**
```json
{
  "message": "Service is available",
  "endpoint": "POST /api/admin/shops/populate-from-google",
  "hint": "Use ?dryRun=true to test without inserting data",
  "authRequired": true
}
```

---

## 🧪 Dry Run Test

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=true&maxRequests=50&skipPlaceDetails=true&apiKey=tcgarena_shops_2026_secret"
```

**Response:**
```json
{
  "totalFound": 35,
  "totalInserted": 28,
  "totalSkipped": 7,
  "apiRequestsUsed": 45,
  "nearbySearchCalls": 45,
  "placeDetailsCalls": 0,
  "requestLimit": 50,
  "dryRun": true,
  "errors": []
}
```

---

## 🚀 Deployment Produzione

### Opzione 1: Solo Nearby Search (CONSIGLIATO - Sotto 1000 richieste)

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950&apiKey=tcgarena_shops_2026_secret"
```

**Cosa ottieni:**
- ✅ ~800-900 negozi TCG in tutta Italia
- ✅ Dati: nome, indirizzo, coordinate, rating
- ✅ Tempo: ~20-25 minuti
- ✅ Costo: ~$29 (sotto free tier)
- ❌ NO telefono, website, orari dettagliati

### Opzione 2: Con Place Details (Dati Completi - Più costoso)

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=false&maxRequests=500&apiKey=tcgarena_shops_2026_secret"
```

**Cosa ottieni:**
- ✅ ~250 negozi con dati COMPLETI
- ✅ Dati: tutto (telefono, website, orari, descrizione)
- ✅ Tempo: ~15-20 minuti
- ✅ Costo: ~$12
- ❌ Non copre tutta Italia (solo prime 50 città)

### Opzione 3: Limite Conservativo per Test

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=false&maxRequests=100&apiKey=tcgarena_shops_2026_secret"
```

---

## 📊 Parametri Disponibili

| Parametro | Tipo | Default | Descrizione |
|-----------|------|---------|-------------|
| `dryRun` | boolean | `true` | Se true, simula senza inserire dati |
| `maxRequests` | integer | `950` | Limite massimo chiamate API Google |
| `skipPlaceDetails` | boolean | `false` | Se true, salta chiamate Place Details |
| `apiKey` | string | **required** | Secret key per autenticazione |

---

## 🔐 Secret Key

La secret key è configurata sul server in `application.properties`:

```properties
app.shop.population.secret.key=tcgarena_shops_2026_secret
```

**Per disabilitare autenticazione** (NON CONSIGLIATO in produzione):
```properties
app.shop.population.secret.key=
```

**Per cambiarla via environment variable:**
```bash
export SHOP_POPULATION_SECRET_KEY=tua_chiave_segreta
```

---

## 🎯 Comando Rapido (Copy-Paste)

### Test Veloce
```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=true&maxRequests=10&skipPlaceDetails=true&apiKey=tcgarena_shops_2026_secret"
```

### Produzione - Solo Italia - Dati Base
```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950&apiKey=tcgarena_shops_2026_secret"
```

### Con Pretty Print (se hai jq)
```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950&apiKey=tcgarena_shops_2026_secret" | jq '.'
```

---

## ⏱️ Monitoraggio in Real-Time

```bash
# Esegui in una shell separata mentre il comando principale gira
watch -n 5 'curl -s "https://api.tcgarena.it/api/admin/shops" | jq "length"'
```

Questo mostrerà il numero di negozi nel database aggiornato ogni 5 secondi.

---

## 🐛 Errori Comuni

### 403 Forbidden
```json
{
  "error": "Forbidden",
  "message": "Invalid or missing API key. This endpoint requires authentication."
}
```

**Soluzione:** Verifica che `apiKey` sia corretto

### 400 Bad Request - API Key Not Configured
```json
{
  "error": "Google Places API key not configured. Set google.places.api.key in application.properties",
  "hint": "Configure google.places.api.key in application.properties"
}
```

**Soluzione:** Configura `GOOGLE_PLACES_API_KEY` sul server

---

## 📝 Response di Successo

```json
{
  "totalFound": 847,
  "totalInserted": 782,
  "totalSkipped": 65,
  "apiRequestsUsed": 894,
  "nearbySearchCalls": 894,
  "placeDetailsCalls": 0,
  "requestLimit": 950,
  "dryRun": false,
  "errors": []
}
```

---

## 🔄 Re-esecuzione

Puoi eseguire lo script più volte:
- I **duplicati vengono saltati** automaticamente
- Controlla per nome + distanza geografica (<100m)
- Solo nuovi negozi vengono inseriti

---

## 💡 Best Practice

1. **Sempre testare con dry-run prima:**
   ```bash
   curl "...?dryRun=true&maxRequests=50..."
   ```

2. **Usare skipPlaceDetails=true per prima popolazione:**
   - Più veloce
   - Più economico
   - Copre più territorio

3. **Arricchire con details in un secondo momento:**
   - Solo per negozi attivi
   - In batch piccoli

4. **Monitorare Google Cloud Console** per costi

---

## 🎬 Script Automatico

Usa lo script bash già pronto:

```bash
chmod +x deploy_shops_public.sh
./deploy_shops_public.sh
```

Lo script fa tutto automaticamente:
- ✅ Test connessione
- ✅ Dry run
- ✅ Conferma utente
- ✅ Popolamento
- ✅ Report risultati
- ✅ Calcolo tempo

---

## 🌍 Copertura Italia

**100+ città coperte:**
- Lazio (4 città)
- Lombardia (10 città)
- Campania (5 città)
- Piemonte (5 città)
- Toscana (9 città)
- Emilia-Romagna (10 città)
- Veneto (7 città)
- E tutte le altre regioni...

**Stima negozi:** 800-1000 negozi TCG in tutta Italia

---

## 💰 Costi Stimati

```
Solo Nearby Search (950 richieste):
- Nearby: 950 × $0.032 = $30.40
- Details: 0 × $0.017 = $0.00
TOTALE: ~$30

Con Place Details (500 richieste):
- Nearby: 250 × $0.032 = $8.00
- Details: 250 × $0.017 = $4.25
TOTALE: ~$12.25
```

**Free Tier Google:** $200/mese → Entrambe le opzioni sono coperte! ✅
