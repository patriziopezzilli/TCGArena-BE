# Gestione Token FCM - Miglioramenti

## Problema Risolto

Gli errori nei log relativi alle notifiche push erano causati da:

1. **Token FCM non validi** - Token scaduti o non più registrati sui dispositivi
2. **Errori 401** - Problemi di autenticazione con Firebase

## Modifiche Implementate

### 1. Rilevamento Automatico Token Non Validi

Il servizio `FirebaseMessagingService` ora:
- Rileva automaticamente token FCM non validi o non registrati
- Lancia un'eccezione `InvalidTokenException` per token non validi
- Migliora il logging con emoji e preview dei token

### 2. Pulizia Automatica Database

Il `NotificationService` ora:
- **Rimuove automaticamente** i token non validi quando vengono rilevati
- Gestisce la pulizia durante l'invio di notifiche normali
- Gestisce la pulizia durante i broadcast

### 3. Nuovi Endpoint Admin

#### GET `/api/notifications/admin/token-statistics`
Restituisce statistiche sui token registrati:
```json
{
  "totalTokens": 45,
  "iosTokens": 30,
  "androidTokens": 15,
  "uniqueUsers": 38
}
```

#### POST `/api/notifications/admin/clean-invalid-tokens`
Esegue una pulizia manuale di tutti i token non validi:
```json
{
  "message": "Token non validi rimossi",
  "removedTokens": 8
}
```

## Come Utilizzare

### Monitorare i Token

```bash
curl -X GET http://localhost:8080/api/notifications/admin/token-statistics \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

### Pulire Token Non Validi

```bash
curl -X POST http://localhost:8080/api/notifications/admin/clean-invalid-tokens \
  -H "Authorization: Bearer YOUR_ADMIN_TOKEN"
```

## Logging Migliorato

### Prima:
```
ERROR c.t.a.s.FirebaseMessagingService - Failed to send push notification: The registration token is not a valid FCM registration token
```

### Dopo:
```
WARN  c.t.a.s.FirebaseMessagingService - 🗑️  Invalid FCM token detected: abc123def456... - Error: INVALID_ARGUMENT
INFO  c.t.a.s.NotificationService - 🗑️  Removing invalid device token for user 123
```

## Gestione Errori

Il sistema ora distingue tra:

1. **Token non validi** → Rimossi automaticamente dal database
2. **Errori di autenticazione (401)** → Log con indicazione di controllare le credenziali Firebase
3. **Altri errori** → Log per debugging

## Best Practices

1. **Pulizia Periodica**: Eseguire `clean-invalid-tokens` settimanalmente tramite cron job
2. **Monitoraggio**: Controllare le statistiche per identificare problemi
3. **Credenziali Firebase**: Assicurarsi che `firebase-service-account.json` sia configurato correttamente

## Risoluzione Problemi

### Se vedi ancora errori 401:
1. Verifica che il file `firebase-service-account.json` esista
2. Controlla i permessi del service account su Firebase Console
3. Verifica che il progetto Firebase sia configurato correttamente

### Se i token continuano a essere non validi:
1. Controlla che le app (iOS/Android) registrino correttamente i token FCM
2. Verifica che i token vengano inviati al backend al login
3. Assicurati che i certificati APNs (per iOS) siano configurati su Firebase
