# TCG Arena - Email System Setup & Use Cases

## 📧 Setup Completato ✅

### ✅ Configurazione
1. **Dipendenze Maven**
   - `spring-boot-starter-mail` ✅
   - `spring-boot-starter-thymeleaf` ✅

2. **SMTP Configuration** (application.properties)
   - Host: Gmail SMTP
   - Port: 587 (TLS)
   - Auth: Enabled
   - Credenziali: Environment variables (EMAIL_USERNAME, EMAIL_PASSWORD)
   - Frontend URL: Configurabile via FRONTEND_URL

3. **EmailService Completo**
   - Metodi per email semplici e HTML
   - Template engine Thymeleaf integrato
   - Logging e error handling
   - **Metodi transazionali implementati**
   - **Metodi batch notturni implementati**

4. **Template Email Stile ShareCard**
   - Design moderno, pulito, responsive
   - Gradient header (#111827 → #374151)
   - Card eleganti con bordi arrotondati
   - Mobile-first approach
   - Dark mode friendly

---

## 🎯 Use Cases Implementati

### 1. **Autenticazione & Sicurezza**

#### 1.1 Password Reset ✅
```java
emailService.sendOtpEmailHtml(email, otp, username);
```
- Template: `password-reset.html`
- Design: Card con codice OTP grande, countdown, warning sicurezza
- Stile: Purple gradient per codice, warning boxes

#### 1.2 Welcome Email ✅
```java
emailService.sendWelcomeEmail(user);
```
- Template: `welcome.html`
- Design: Hero emoji 🎴, feature grid 2x2
- Features: Collezione, Scambi, Eventi, Community
- CTA: "Inizia ora" con link al frontend

---

### 2. **Mail Notturne (Batch Jobs)**

#### 2.1 JustTCG Import Summary ✅
```java
emailService.sendImportSummary(email, importSummaryDTO);
```
- Template: `import-summary.html`
- DTO: `ImportSummaryEmailDTO`
- Design:
  - Status banner colorato (green/yellow/red)
  - Grid 2x2 con statistiche (processate/aggiunte/aggiornate/saltate)
  - Tabella delta con modifiche carta per carta
  - Warning box per errori
- Dati tracciati:
  - `totalCardsProcessed`, `cardsAdded`, `cardsUpdated`, `cardsSkipped`, `errors`
  - Lista `CardDelta` con: cardName, setName, quantityBefore, quantityAfter, changeType
  - `importStartTime`, `importEndTime` per calcolare durata
  - `status`: SUCCESS, PARTIAL_SUCCESS, FAILED

#### 2.2 Daily Digest ✅
```java
emailService.sendDailyDigest(email, dailyDigestDTO);
```
- Template: `daily-digest.html`
- DTO: `DailyDigestEmailDTO`
- Design:
  - Header con data digest
  - User stats grid (nuove carte, scambi, messaggi, visualizzazioni)
  - Collection value change (evidenziato in gold gradient)
  - Highlights cards (match trovati, eventi, alert prezzi)
  - Recommendations cards con immagini
  - Platform stats (community growth)
- Dati tracciati:
  - **UserStats**: newCards, newTrades, completedTrades, messagesReceived, profileViews, eventsNearby, collectionValueChange
  - **PlatformStats**: newUsers, activeTrades, upcomingEvents, newShops
  - **Highlights**: type, title, description, actionUrl, icon
  - **Recommendations**: type, title, description, imageUrl, actionUrl

---

### 3. **Mail Transazionali App**

#### 3.1 Tournament Registration ✅
```java
emailService.sendTournamentRegistration(email, username, event);
```
- Template: `tournament-registration.html`
- Design: Success banner verde, event card con dettagli, info box blu
- Contenuto: Nome torneo, data/ora, luogo, CTA a dettagli evento
- Note: Suggerimento aggiunta a calendario + reminder 24h

#### 3.2 Card Reservation with QR Code ✅
```java
emailService.sendCardReservation(email, username, cardName, shopName, qrCodeUrl);
```
- Template: `card-reservation.html`
- Design: QR code centrato con border dashed, warning box giallo
- Contenuto: Nome carta, negozio, QR code grande, scadenza 48h
- Use case: Merchant shop inventory reservation

#### 3.3 Inactivity Reminder ✅
```java
emailService.sendInactivityReminder(user, daysInactive);
```
- Template: `inactivity-reminder.html`
- Design: Hero emoji 😢, feature cards "cosa ti sei perso"
- Trigger: 30 giorni di inattività
- Contenuto: Nuove funzionalità, community growth, nuove carte/set

---

## 📊 Database Tracking per Daily Digest

Per implementare il daily digest, considera di aggiungere queste tracking tables:

### User Activity Tracking
```sql
CREATE TABLE user_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    new_cards INT DEFAULT 0,
    new_trades INT DEFAULT 0,
    completed_trades INT DEFAULT 0,
    messages_received INT DEFAULT 0,
    profile_views INT DEFAULT 0,
    events_nearby INT DEFAULT 0,
    collection_value_change DECIMAL(10,2) DEFAULT 0.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY unique_user_date (user_id, stat_date)
);
```

### Platform Daily Stats
```sql
CREATE TABLE platform_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL UNIQUE,
    new_users INT DEFAULT 0,
    active_trades INT DEFAULT 0,
    upcoming_events INT DEFAULT 0,
    new_shops INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Import History
```sql
CREATE TABLE import_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    import_type VARCHAR(50) NOT NULL, -- 'JUSTTCG', 'CSV', 'MANUAL'
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(50) NOT NULL, -- 'SUCCESS', 'PARTIAL_SUCCESS', 'FAILED'
    total_cards_processed INT DEFAULT 0,
    cards_added INT DEFAULT 0,
    cards_updated INT DEFAULT 0,
    cards_skipped INT DEFAULT 0,
    errors INT DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE import_deltas (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    import_history_id BIGINT NOT NULL,
    card_template_id BIGINT NOT NULL,
    card_name VARCHAR(255),
    set_name VARCHAR(255),
    quantity_before INT DEFAULT 0,
    quantity_after INT NOT NULL,
    change_type VARCHAR(20) NOT NULL, -- 'ADDED', 'INCREASED', 'DECREASED'
    FOREIGN KEY (import_history_id) REFERENCES import_history(id) ON DELETE CASCADE
);
```

---

## 🕐 Scheduled Jobs (da implementare)

### 1. Nightly JustTCG Import (3:00 AM)
```java
@Scheduled(cron = "0 0 3 * * *") // Every day at 3 AM
public void runNightlyImport() {
    // 1. Per ogni utente con JustTCG collegato
    // 2. Esegui import e traccia in import_history
    // 3. Salva deltas in import_deltas
    // 4. Invia email con emailService.sendImportSummary()
}
```

### 2. Daily Digest Email (8:00 AM)
```java
@Scheduled(cron = "0 0 8 * * *") // Every day at 8 AM
public void sendDailyDigests() {
    // 1. Calcola statistiche giornata precedente
    // 2. Genera highlights (matches, eventi, alert)
    // 3. Genera recommendations personalizzate
    // 4. Invia email con emailService.sendDailyDigest()
    // Solo per utenti che hanno abilitato questa notifica!
}
```

### 3. Email Preferences
```sql
CREATE TABLE user_email_preferences (
    user_id BIGINT PRIMARY KEY,
    welcome_email BOOLEAN DEFAULT TRUE,
    password_reset BOOLEAN DEFAULT TRUE,
    tournament_notifications BOOLEAN DEFAULT TRUE,
    reservation_confirmations BOOLEAN DEFAULT TRUE,
    inactivity_reminders BOOLEAN DEFAULT TRUE,
    import_summaries BOOLEAN DEFAULT TRUE,
    daily_digest BOOLEAN DEFAULT FALSE, -- Opt-in
    marketing_emails BOOLEAN DEFAULT FALSE,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
```

---

## 🚀 Implementazione Priority List

### ✅ COMPLETATO
1. ✅ Setup infrastructure (Thymeleaf, EmailService, templates)
2. ✅ Password reset email (già esistente, migliorato)
3. ✅ Welcome email → **Integrato in JwtAuthenticationController.register()**
4. ✅ Import summary email → **EmailSchedulerService @3:00 AM (SOLO admin)**
5. ✅ Daily digest email → **EmailSchedulerService @8:00 AM (SOLO admin)**
6. ✅ Tournament/Event registration → **Integrato in TournamentController e CommunityEventController**
7. ✅ Card reservation con QR (metodo pronto, da integrare in ShopController)
8. ✅ Inactivity reminder → **Scheduled job @10:00 AM (utenti inattivi)**
9. ✅ Correzione domini da tcgarena.com → **tcgarena.it**
10. ✅ Admin email configurato → **patriziopezzilli@gmail.com**

### 🔨 TODO HIGH PRIORITY
1. **Creare scheduled jobs** per import e digest
2. **Database tables** per tracking (import_history, user_daily_stats, etc.)
3. **User email preferences** system
4. **Event reminder** (24h prima)
5. **Trade notifications** (request, accepted, completed)

### 🔨 TODO MEDIUM PRIORITY
6. Security alerts (login nuovo dispositivo)
7. Shop activation approved/rejected
8. Event cancelled/updated
9. New review received
10. Email verification al signup

### 🔨 TODO LOW PRIORITY
11. Monthly shop report
12. Low stock alert per merchant
13. Pull feed milestone (likes)
14. Chat notifications (batch)
15. Premium expiring reminder

---

## 🛠️ Setup Produzione

### Gmail SMTP Setup
1. Creare account Gmail dedicato (es. noreply@tcgarena.com)
2. Abilitare "2-Step Verification"
3. Generare "App Password" per TCG Arena
4. Configurare variabili ambiente:
   ```bash
   export EMAIL_USERNAME=noreply@tcgarena.com
   export EMAIL_PASSWORD=your_app_password
   export FRONTEND_URL=https://tcgarena.com
   ```

### Alternative SMTP Providers
- **SendGrid**: 100 email/giorno gratis
- **Mailgun**: 5000 email/mese gratis (primi 3 mesi)
- **Amazon SES**: $0.10 per 1000 email
- **Postmark**: Template email specializzati

---

## 📝 Best Practices

### Email Design
- ✅ Responsive design (mobile-first)
- ✅ Dark mode friendly (evitare pure black/white)
- ✅ Call-to-action chiari e visibili
- ✅ Testo alternativo per immagini
- ✅ Footer con unsubscribe link

### Sending Strategy
- ⚠️ Rate limiting: max 100 email/min
- ⚠️ Batch notifications: raggruppare notifiche simili
- ⚠️ Priorità: Security > Transactional > Marketing
- ⚠️ Retry logic: 3 tentativi con backoff esponenziale
- ⚠️ Email preferences: permettere opt-out selettivo

### Testing
- Test su diversi client (Gmail, Outlook, Apple Mail)
- Test rendering mobile
- Verificare link e variabili Thymeleaf
- Spam score checking

---

## 🚀 Prossimi Step

1. **Implementare use cases prioritari**:
   - Welcome email
   - Email verification
   - Trade notifications
   - Event notifications

2. **Creare template HTML per ogni use case**

3. **Aggiungere metodi specifici in EmailService**:
   ```java
   sendWelcomeEmail(User user)
   sendTradeNotification(Trade trade, User recipient)
   sendEventReminder(Event event, User participant)
   ```

4. **Implementare email preferences**:
   - User settings per tipo di notifica
   - Opt-out specifico per categoria

5. **Monitoring & Analytics**:
   - Log invio email
   - Track delivery rate
   - Monitor bounce rate
