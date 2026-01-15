# 🎯 Card Rating Arena Daily Notifications

## Implementazione Notifiche Giornaliere

### 📅 Scheduling
- **Frequenza**: Ogni giorno alle ore 16:00
- **Target**: Solo utenti che hanno espresso almeno 1 voto
- **Scopo**: Ricordare agli utenti attivi di continuare a votare

### 💬 Messaggi
I messaggi ruotano casualmente tra queste opzioni simpatiche:

1. "🌟 Ehi campione! Hai ancora energia per votare qualche carta oggi?"
2. "🎯 Il tuo parere conta! Cosa ne pensi delle nuove carte uscite?"
3. "🔥 Sei un esperto di carte! Aiutaci a costruire la community dei voti!"
4. "⭐ Le tue valutazioni aiutano tutti! Pronto per qualche voto oggi?"
5. "🎪 Entra nell'arena! Le carte aspettano il tuo giudizio!"
6. "⚡ Flash vote time! Cosa ne pensi delle ultime novità?"
7. "🎨 Tu sei il giudice! Le carte attendono il tuo verdetto!"
8. "🚀 Pronti per decollare? I tuoi voti sono sempre benvenuti!"
9. "💎 Le tue opinioni sono preziose! Hai tempo per qualche voto oggi?"
10. "🎪 Benvenuto nell'arena! Le carte sono pronte per il tuo giudizio!"

### 🔧 Implementazione Tecnica

#### Scheduler
```java
@Scheduled(cron = "0 0 16 * * ?") // Ogni giorno alle 16:00
public void sendDailyCardRatingArenaNotifications()
```

#### Query Database
```sql
SELECT DISTINCT v.user FROM CardVote v
```
Trova tutti gli utenti che hanno votato almeno una volta.

#### Logica
1. Recupera tutti gli utenti che hanno votato
2. Per ogni utente, seleziona casualmente un messaggio
3. Invia notifica push tramite Firebase
4. Gestisce errori e token invalidi automaticamente
5. Piccola pausa tra notifiche per evitare sovraccarico

### 📊 Metriche
- Numero totale di notifiche inviate giornalmente
- Tasso di successo delle consegne
- Impegno degli utenti (aumento voti dopo notifica)

### 🔒 Sicurezza
- Solo utenti autenticati che hanno già interagito con il sistema
- Rispetta le preferenze di notifica esistenti
- Gestione automatica dei token FCM invalidi</content>
<parameter name="filePath">/Users/patriziopezzilli/Documents/Sviluppo/TCGArena-BE/CARD_RATING_ARENA_DAILY_NOTIFICATIONS.md