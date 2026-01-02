# 🔔 Riepilogo Notifiche Push - TCG Arena

## ✅ Configurazione Completata

### Funzionalità Implementate
- ✅ Suono di default su iOS e Android
- ✅ Vibrazione su Android
- ✅ Badge su iOS
- ✅ Priorità alta per consegna immediata
- ✅ Rimozione automatica token non validi
- ✅ Tutte le notifiche in italiano

---

## 📱 Tipi di Notifiche

### 🎴 Prenotazioni
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Prenotazione confermata | "Prenotazione Confermata ✓" | "La tua prenotazione per [carta] è stata validata da [negozio]" |
| Prenotazione in scadenza | "Prenotazione in Scadenza ⏰" | "La tua prenotazione per [carta] presso [negozio] scade tra 30 minuti!" |

### 💬 Richieste
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Nuova risposta | "Nuova Risposta 💬" | "[negozio] ha risposto alla tua richiesta: [titolo]" |
| Cambio stato | "Aggiornamento Richiesta" | "La tua richiesta [titolo] è ora: [stato]" |

### 🎮 Tornei
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Torneo inizia presto | "Il Torneo Sta Per Iniziare! 🎮" | "[torneo] inizia tra 15 minuti presso [negozio]" |
| Torneo iniziato | "Torneo Iniziato! 🎯" | "Il torneo [titolo] è iniziato. Buona fortuna!" |
| Torneo concluso | "Torneo Concluso 🏆" | "Hai ottenuto il [posizione] posto in [torneo]! +[punti] punti" |
| Check-in disponibile | "Check-in Disponibile ✅" | "Il check-in per [torneo] è ora disponibile!" |
| Rimosso da torneo | "Aggiornamento Iscrizione" | "Sei stato rimosso dal torneo [titolo]. Contatta l'organizzatore" |

### 📅 Eventi e News
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Nuovo evento | "Nuovo Evento 📅" | "[negozio] ha pubblicato un nuovo evento: [titolo] - [data]" |
| Novità negozio | "Novità da [negozio] 📢" | "[titolo news]" |
| Nuovo partecipante | "Nuovo partecipante" | "[utente] si è iscritto al tuo evento: [titolo]" |
| Evento annullato | "Evento annullato" | "L'evento \"[titolo]\" è stato annullato" |

### 🎁 Rewards e Livelli
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Reward riscattato | "Reward Riscattato! 🎁" | "Hai riscattato [reward]. Mostra il codice al negozio" |
| Nuovo livello | "Level Up! 🎉" | "Congratulazioni! Sei salito al livello [numero]!" |

### ❤️ Community
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Like su pull | "Nuovo Like! ❤️" | "[utente] ha messo mi piace al tuo pull di [TCG]" |

### 🧪 Test
| Evento | Titolo | Messaggio |
|--------|--------|-----------|
| Notifica test | "Notifica di Test 🔔" | "Questa è una notifica di test da TCG Arena! Se la vedi, tutto funziona correttamente." |

---

## 🔧 Configurazione Tecnica

### iOS (APNs)
```json
{
  "aps": {
    "sound": "default",
    "badge": 1
  }
}
```

### Android
```json
{
  "priority": "HIGH",
  "notification": {
    "sound": "default",
    "default_sound": true,
    "default_vibrate_timings": true
  }
}
```

---

## 🛠️ Gestione Token

### Pulizia Automatica
Il sistema rimuove automaticamente i token FCM non validi quando:
- Token non registrato (UNREGISTERED)
- Token non valido (INVALID)
- Token scaduto

### Endpoint Admin

**Statistiche token:**
```bash
GET /api/notifications/admin/token-statistics
```

**Pulizia manuale:**
```bash
POST /api/notifications/admin/clean-invalid-tokens
```

**Verifica configurazione Firebase:**
```bash
GET /api/notifications/admin/firebase-status
```

---

## 📊 Logging

### Successo
```
✅ Push sent successfully to token ...xyz: projects/tcg-arena-8b86a/messages/0:123456789
```

### Token Rimosso
```
🗑️  Invalid FCM token detected: abc... - Error: UNREGISTERED
🗑️  Removing invalid device token for user 123
```

### Errore Autenticazione
```
🔐 Firebase authentication failed (HTTP 401)
   ⚠️  POSSIBLE CAUSES:
   1. FCM v1 API not enabled
   2. Service account lacks permissions
   3. APNs credentials not configured
```

---

## ✅ Checklist Setup

- [x] Firebase Cloud Messaging API abilitata
- [x] Service account con permessi corretti
- [x] APNs Authentication Key configurata (iOS)
- [x] Bundle ID corretto in Firebase
- [x] GoogleService-Info.plist aggiornato nell'app
- [x] Suono e vibrazione configurati
- [x] Tutte le notifiche in italiano
- [x] Sistema di pulizia automatica token

---

## 🎯 Best Practices

1. **Test regolari** usando l'endpoint `/api/notifications/test-push`
2. **Monitoraggio statistiche** token tramite `/admin/token-statistics`
3. **Pulizia periodica** token non validi (settimanale)
4. **Log monitoring** per errori 401 o problemi di autenticazione
5. **Aggiornamento credenziali** APNs prima della scadenza

---

## 📝 Note

- Le notifiche arrivano solo se l'app ha i permessi notification attivi
- Su iOS, le notifiche in foreground devono essere gestite dall'app
- Il badge viene incrementato automaticamente su iOS
- La vibrazione è configurata con timing di default su Android
