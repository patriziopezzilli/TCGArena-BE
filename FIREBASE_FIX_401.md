# Fix Errore 401 - Firebase Cloud Messaging

## Problema
Firebase è inizializzato correttamente, ma le notifiche falliscono con errore 401.

## Causa
Il service account non ha i permessi necessari per inviare notifiche FCM.

## Soluzione

### 1. Vai alla Firebase Console
https://console.firebase.google.com/

### 2. Seleziona il progetto TCG Arena

### 3. Vai in Project Settings (⚙️ in alto a sinistra)

### 4. Tab "Service accounts"

### 5. Verifica i Permessi

Il service account deve avere il ruolo: **Firebase Cloud Messaging Admin**

### 6. Opzione A - Genera Nuovo Service Account (RACCOMANDATO)

1. Clicca su "Generate new private key"
2. Conferma il download
3. Rinomina il file in `firebase-service-account.json`
4. Sostituisci il file esistente:
   ```bash
   # Sul server
   cp nuovo-file.json /root/TCGArena-BE/firebase-service-account.json
   ```
5. Riavvia l'applicazione

### 7. Opzione B - Aggiungi Permessi Manualmente

1. Vai su Google Cloud Console: https://console.cloud.google.com/
2. Seleziona il progetto TCG Arena
3. Vai in IAM & Admin → IAM
4. Trova il service account (email tipo: firebase-adminsdk-xxxxx@tcg-arena.iam.gserviceaccount.com)
5. Clicca Edit (matita)
6. Aggiungi questi ruoli:
   - **Firebase Cloud Messaging Admin**
   - **Firebase Admin SDK Administrator Service Agent**
7. Salva

### 8. Alternative - Usa Firebase Admin SDK v2 API

Se il problema persiste, potrebbe essere necessario abilitare la FCM v1 API:

1. Vai su Google Cloud Console
2. APIs & Services → Enable APIs and Services
3. Cerca "Firebase Cloud Messaging API"
4. Abilita l'API
5. Riavvia l'applicazione

## Verifica

Dopo aver applicato le modifiche:

```bash
# Riavvia il backend
./mvnw spring-boot:run

# Testa con una notifica
curl -X POST http://localhost:8080/api/notifications/test-push \
  -H "Authorization: Bearer YOUR_TOKEN"
```

## Log Attesi

✅ **Prima (inizializzazione):**
```
✅ Firebase initialized successfully from env path: /root/TCGArena-BE/firebase-service-account.json
```

✅ **Dopo (invio notifica):**
```
✅ Push sent to token ...xyz123: projects/tcg-arena/messages/0:123456789
```

## Note

- Il file service account contiene credenziali sensibili, NON committarlo su Git
- Aggiungi al `.gitignore`: `firebase-service-account.json`
- Su Docker/produzione usa variabili d'ambiente o secrets
