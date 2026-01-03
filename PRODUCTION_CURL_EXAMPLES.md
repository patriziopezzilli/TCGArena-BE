# TCG Arena - Comandi cURL per Produzione

## 🔐 Setup Iniziale

### 1. Ottenere JWT Token Admin

```bash
# Login come admin
curl -X POST "https://api.tcgarena.it/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@tcgarena.com",
    "password": "TUA_PASSWORD_ADMIN"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "admin@tcgarena.com",
  "role": "ADMIN"
}
```

Salva il token:
```bash
export ADMIN_TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

---

## 🧪 Test - Dry Run

### Dry Run con limite basso (consigliato prima volta)

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=true&maxRequests=50&skipPlaceDetails=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Response attesa:**
```json
{
  "totalFound": 25,
  "totalInserted": 20,
  "totalSkipped": 5,
  "apiRequestsUsed": 45,
  "nearbySearchCalls": 45,
  "placeDetailsCalls": 0,
  "requestLimit": 50,
  "dryRun": true,
  "errors": []
}
```

---

## 🚀 Produzione - Popolamento Reale

### Opzione 1: Solo Nearby Search (CONSIGLIATO)
**Più veloce, economico, copre tutta Italia**

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Stima:**
- 100+ città italiane × 9 keywords = ~900 richieste
- Costo: ~$29
- Tempo: ~15-20 minuti
- Dati: nome, indirizzo, coordinate, rating (NO telefono/website)

### Opzione 2: Con Place Details (PIÙ COMPLETO)
**Più lento, più costoso, dati completi**

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=false&maxRequests=950" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Stima:**
- ~475 Nearby Search + ~475 Place Details = 950 richieste
- Costo: ~$23
- Tempo: ~30-40 minuti
- Dati: TUTTI (telefono, website, orari, descrizione)

### Opzione 3: Limite Conservativo
**Per testare con budget limitato**

```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=false&maxRequests=200" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json"
```

**Stima:**
- ~100 Nearby + ~100 Details = 200 richieste
- Costo: ~$5
- Copre: ~20-25 città
- Dati: COMPLETI per i negozi trovati

---

## 📊 Monitoraggio

### Verifica negozi inseriti

```bash
# Ottieni tutti i negozi (inclusi inattivi)
curl -X GET "https://api.tcgarena.it/api/admin/shops" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Conta negozi per stato

```bash
# Query SQL diretta (se hai accesso al DB)
SELECT 
  active,
  is_verified,
  COUNT(*) as count
FROM shops
GROUP BY active, is_verified;
```

### Ottieni statistiche API Google

Vai su:
https://console.cloud.google.com/apis/api/places-backend.googleapis.com/metrics

---

## 🔄 Arricchimento Dati Post-Popolazione

Se hai usato `skipPlaceDetails=true`, puoi arricchire dopo:

### 1. Crea endpoint dedicato per arricchimento
```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/enrich-details?shopId=123" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### 2. Oppure arricchisci in batch
```bash
curl -X POST "https://api.tcgarena.it/api/admin/shops/enrich-batch" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "shopIds": [1, 2, 3, 4, 5],
    "fields": ["phone", "website", "openingHours"]
  }'
```

---

## ✅ Attivazione Negozi

Dopo la popolazione, i negozi sono **inattivi** e **non verificati**.

### Attiva singolo negozio

```bash
curl -X PUT "https://api.tcgarena.it/api/admin/shops/123" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "active": true,
    "isVerified": true
  }'
```

### Attivazione in batch (SQL)

```sql
-- Attiva tutti i negozi con rating >= 4.0
UPDATE shops 
SET active = true, is_verified = true 
WHERE rating >= 4.0 AND active = false;

-- Attiva solo negozi in città specifiche
UPDATE shops 
SET active = true, is_verified = true 
WHERE address LIKE '%Roma%' OR address LIKE '%Milano%';
```

---

## 🐛 Troubleshooting

### Errore: "Google Places API key not configured"

```bash
# Verifica variabile ambiente sul server
ssh user@api.tcgarena.it
echo $GOOGLE_PLACES_API_KEY

# Oppure controlla application.properties
cat /path/to/application.properties | grep google.places.api.key
```

### Errore: "REQUEST_DENIED"

- Verifica che Places API sia abilitata su Google Cloud Console
- Controlla restrizioni API Key
- Verifica quota disponibile

### Errore: "OVER_QUERY_LIMIT"

```bash
# Riduci maxRequests
curl -X POST "...?maxRequests=100"
```

### Errore: "Unauthorized"

```bash
# Rinnova JWT token (scade dopo 24h)
curl -X POST "https://api.tcgarena.it/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@tcgarena.com","password":"..."}'
```

---

## 📝 Log e Debugging

### Visualizza log in tempo reale

```bash
ssh user@api.tcgarena.it
tail -f /var/log/tcgarena/spring-boot.log | grep GooglePlacesService
```

### Filtra solo errori

```bash
tail -f /var/log/tcgarena/spring-boot.log | grep -i error
```

---

## 🎯 Raccomandazione Deployment

### Piano Consigliato

**Fase 1: Test**
```bash
# Dry run per verificare configurazione
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=true&maxRequests=50&skipPlaceDetails=true" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Fase 2: Popolazione Base**
```bash
# Solo nearby search - veloce ed economico
curl -X POST "https://api.tcgarena.it/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

**Fase 3: Verifica**
```bash
# Controlla negozi inseriti
curl -X GET "https://api.tcgarena.it/api/admin/shops" \
  -H "Authorization: Bearer $ADMIN_TOKEN" | jq 'length'
```

**Fase 4: Attivazione Selettiva**
```sql
-- Attiva solo i migliori negozi
UPDATE shops 
SET active = true, is_verified = true 
WHERE rating >= 4.0;
```

**Fase 5: Arricchimento (Opzionale)**
```bash
# Mese successivo, arricchisci con place details
# Solo per negozi attivi
```

---

## 📞 Supporto

In caso di problemi:
1. Controlla i log del server
2. Verifica Google Cloud Console per errori API
3. Controlla database per duplicati
4. Verifica configurazione API key

---

## 💰 Costo Stimato

### Setup Iniziale (Italia Completa)
```
Opzione 1 - Solo Nearby: $29
Opzione 2 - Con Details: $23
Opzione 3 - Batch limitato: $5-10
```

### Mantenimento Mensile
```
Update parziale: $5-10/mese
Refresh completo: $20-30/mese
```

**TOTALE ANNO 1:**
- Setup: $25
- Manutenzione: $60-120
- **TOTALE: $85-145/anno** (ben sotto i $2400 del free tier!)
