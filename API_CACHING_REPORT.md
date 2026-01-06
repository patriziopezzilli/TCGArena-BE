# TCG Arena Backend - API Caching Report

**Data Generazione:** 6 Gennaio 2026  
**Ultimo Aggiornamento:** 6 Gennaio 2026  
**Totale Controller:** 42  
**Totale Endpoint:** ~270+

---

## ✅ IMPLEMENTAZIONE COMPLETATA

L'implementazione della cache è stata completata. Ecco un riepilogo:

### Service Layer Cache (Spring @Cacheable)

| Service | Caches Implementate | TTL |
|---------|---------------------|-----|
| `CardTemplateService` | cardTemplates, cardTemplateById, cardSearch, cardFilters | 6-12 ore |
| `ExpansionService` | expansions, expansionById | 12-24 ore |
| `TCGSetService` | sets, setById, setCards | 12 ore |
| `ProDeckService` | proDecks, proDeckById, recentProDecks | 6 ore |
| `AchievementService` | achievements, achievementById | 12 ore |
| `PartnerService` | partners, partnerById | 12 ore |
| `ShopService` | shops, shopById | 30 min |
| `RewardService` | rewards, rewardById | 30 min |
| `TournamentService` | tournaments, tournamentById | 5 min |
| `GlobalChatService` | globalChat | 15 sec |
| `UserService` | leaderboard | 10 min |

### HTTP Cache Headers (Browser/CDN Cache)

| Controller | Endpoint | Cache-Control TTL |
|------------|----------|-------------------|
| `PublicController` | /api/public/shops/{id} | 30 min |
| `PublicController` | /api/public/tournaments/{id} | 5 min |
| `PublicController` | /api/public/community-events/{id} | 5 min |
| `PublicController` | /api/public/cards/{id} | 6 ore |
| `ArenaApiController` | /api/arena/games | 1 ora |
| `ArenaApiController` | /api/arena/games/{id} | 1 ora |
| `ArenaApiController` | /api/arena/sets | 30 min |
| `ArenaApiController` | /api/arena/sets/{id} | 1 ora |
| `ArenaApiController` | /api/arena/cards | 15 min |
| `ArenaApiController` | /api/arena/cards/{id} | 6 ore |
| `ArenaApiController` | /api/arena/cards/tcgplayer/{id} | 6 ore |
| `ArenaApiController` | /api/arena/cards/scryfall/{id} | 6 ore |

---

## Indice
1. [Riepilogo Raccomandazioni Caching](#riepilogo-raccomandazioni-caching)
2. [API Altamente Cacheable](#api-altamente-cacheable)
3. [API Moderatamente Cacheable](#api-moderatamente-cacheable)
4. [API Non Cacheable](#api-non-cacheable)
5. [Elenco Completo API per Controller](#elenco-completo-api-per-controller)

---

## Riepilogo Raccomandazioni Caching

| Categoria | Numero Endpoint | Eviction Time Suggerito |
|-----------|-----------------|------------------------|
| 🟢 **Altamente Cacheable** | ~45 | 5 min - 24 ore |
| 🟡 **Moderatamente Cacheable** | ~35 | 1 min - 5 min |
| 🔴 **Non Cacheable** | ~190 | N/A |

---

## 🟢 API Altamente Cacheable

Queste API restituiscono dati che cambiano raramente e sono ideali per il caching aggressivo.

### Card Templates & Catalogo (TTL: 1-24 ore)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/cards/templates` | GET | **6 ore** | Catalogo carte cambia raramente |
| `/api/cards/templates/{id}` | GET | **12 ore** | Singola carta, dati statici |
| `/api/cards/templates/search` | GET | **30 min** | Ricerche frequenti ma dati stabili |
| `/api/cards/templates/unified-search` | GET | **30 min** | Ricerche unificate |
| `/api/cards/templates/filters/tcg-types` | GET | **24 ore** | Tipi TCG fissi |
| `/api/cards/templates/filters/rarities` | GET | **24 ore** | Rarità fisse |
| `/api/cards/templates/filters/set-codes` | GET | **6 ore** | Set codes stabili |
| `/api/cards/templates/{id}/market-price` | GET | **5 min** | Prezzi cambiano frequentemente |

### Espansioni & Set (TTL: 6-24 ore)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/expansions` | GET | **12 ore** | Lista espansioni stabile |
| `/api/expansions/{id}` | GET | **24 ore** | Singola espansione, dati statici |
| `/api/expansions/{id}/cards` | GET | **12 ore** | Carte di un'espansione, stabili |
| `/api/expansions/recent` | GET | **6 ore** | Nuove espansioni rare |
| `/api/sets` | GET | **12 ore** | Lista set stabile |
| `/api/sets/{id}` | GET | **24 ore** | Singolo set, dati statici |
| `/api/sets/{id}/card-templates` | GET | **12 ore** | Carte del set stabili |
| `/api/sets/by-code/{code}/cards` | GET | **12 ore** | Carte per codice set |

### Pro Decks (TTL: 1-6 ore)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/pro-decks` | GET | **6 ore** | Decks professionali, aggiornamento raro |
| `/api/pro-decks/{id}` | GET | **6 ore** | Singolo deck pro |
| `/api/pro-decks/recent` | GET | **1 ora** | Recenti, più dinamici |

### Achievement & Partner (TTL: 1-12 ore)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/achievements` | GET | **12 ore** | Achievement configurati raramente |
| `/api/achievements/{id}` | GET | **12 ore** | Singolo achievement |
| `/api/partners` | GET | **6 ore** | Partner cambiano raramente |
| `/api/partners/{id}` | GET | **6 ore** | Singolo partner |

### Shops Pubblici (TTL: 5-30 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/shops` | GET | **15 min** | Lista negozi con filtri geo |
| `/api/shops/{id}` | GET | **30 min** | Dettaglio singolo negozio |
| `/api/public/shops/{id}` | GET | **30 min** | Condivisione pubblica |
| `/api/shops/{shopId}/news` | GET | **5 min** | News negozio pubbliche |
| `/api/shops/{shopId}/rewards` | GET | **10 min** | Rewards disponibili |

### Arena API (Pubblica, TTL: 1-12 ore)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/arena/games` | GET | **24 ore** | Lista giochi fissa |
| `/api/arena/games/{id}` | GET | **24 ore** | Dettaglio gioco |
| `/api/arena/sets` | GET | **12 ore** | Lista set Arena |
| `/api/arena/sets/{id}` | GET | **12 ore** | Dettaglio set |
| `/api/arena/cards` | GET | **6 ore** | Ricerca carte |
| `/api/arena/cards/{id}` | GET | **12 ore** | Dettaglio carta con varianti |

### Contenuti Pubblici (TTL: 5-30 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/public/tournaments/{id}` | GET | **5 min** | Info torneo condivise |
| `/api/public/events/{id}` | GET | **5 min** | Evento community condiviso |
| `/api/public/cards/{id}` | GET | **30 min** | Carta condivisa |
| `/health` | GET | **30 sec** | Health check (cache breve) |

---

## 🟡 API Moderatamente Cacheable

Queste API hanno dati che cambiano più frequentemente ma possono comunque beneficiare di un caching breve.

### Tornei (TTL: 1-5 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/tournaments` | GET | **2 min** | Lista tornei cambia spesso |
| `/api/tournaments/upcoming` | GET | **2 min** | Tornei imminenti |
| `/api/tournaments/nearby` | GET | **2 min** | Basato su posizione utente |
| `/api/tournaments/past` | GET | **5 min** | Tornei passati stabili |
| `/api/tournaments/{id}` | GET | **1 min** | Dettaglio torneo |
| `/api/tournaments/{id}/participants` | GET | **30 sec** | Partecipanti cambiano |
| `/api/tournaments/{id}/updates` | GET | **30 sec** | Aggiornamenti live |

### Community Content (TTL: 1-5 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/community/events` | GET | **2 min** | Eventi community |
| `/api/community/events/{id}` | GET | **1 min** | Singolo evento |
| `/api/community/pulls` | GET | **1 min** | Feed pulls, molto dinamico |
| `/api/threads` | GET | **2 min** | Lista thread community |
| `/api/threads/{id}` | GET | **1 min** | Thread con risposte |

### Leaderboard (TTL: 5-15 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/users/leaderboard` | GET | **10 min** | Classifica utenti |
| `/api/users/leaderboard/collection` | GET | **10 min** | Classifica collezione |
| `/api/users/leaderboard/tournaments` | GET | **10 min** | Classifica tornei |
| `/api/users/leaderboard/active-players` | GET | **5 min** | Giocatori attivi |

### Trade Pubblico (TTL: 1-3 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/trade/listings/public` | GET | **2 min** | Annunci scambio pubblici |
| `/api/trade/user/{userId}` | GET | **2 min** | Lista trade utente |

### Global Chat (TTL: 10-30 sec)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/global-chat/messages` | GET | **15 sec** | Messaggi recenti |
| `/api/global-chat/messages/after` | GET | **10 sec** | Nuovi messaggi |

### Home Dashboard (TTL: 1-3 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/home/dashboard` | GET | **2 min** | Dashboard aggregata (per utente) |

### Rewards (TTL: 5-10 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/rewards` | GET | **10 min** | Lista rewards attive |
| `/api/rewards/partner/{partnerId}` | GET | **10 min** | Rewards per partner |
| `/api/shop-rewards/available` | GET | **5 min** | Rewards negozio disponibili |

### User Search (TTL: 2-5 min)

| Endpoint | Metodo | Eviction Time | Motivazione |
|----------|--------|---------------|-------------|
| `/api/users/search` | GET | **3 min** | Ricerca utenti |

---

## 🔴 API Non Cacheable

Queste API **NON dovrebbero essere cachate** perché:
- Modificano dati (POST/PUT/DELETE)
- Contengono dati altamente personalizzati
- Richiedono dati real-time
- Gestiscono sessioni o token

### Autenticazione (Mai cache)
- `POST /api/auth/login` - Login
- `POST /api/auth/register` - Registrazione
- `POST /api/auth/refresh` - Refresh token
- `POST /api/auth/forgot-password` - Password reset
- `POST /api/auth/verify-otp` - OTP verification
- `POST /api/auth/verify-email` - Email verification

### Azioni Utente (Mai cache)
- `POST /api/cards/{id}/add-to-collection` - Aggiunta carta
- `DELETE /api/cards/collection/{id}` - Rimozione carta
- `POST /api/decks` - Creazione deck
- `PUT /api/decks/{id}` - Modifica deck
- `DELETE /api/decks/{id}` - Eliminazione deck
- `POST /api/tournaments/{id}/register` - Registrazione torneo
- `POST /api/trade/add` - Aggiunta scambio
- `POST /api/chat/{id}/send` - Invio messaggio

### Dati Utente Personalizzati (Evitare cache condivisa)
- `GET /api/cards/collection` - Collezione utente (cache per utente OK)
- `GET /api/decks` - Deck utente (cache per utente OK)
- `GET /api/notifications` - Notifiche personali
- `GET /api/chat` - Conversazioni personali
- `GET /api/trade/matches` - Match personalizzati
- `GET /api/users/{id}/stats` - Stats personali

### Real-time / WebSocket
- WebSocket `/ws/**` - Connessioni real-time
- WebSocket `/app/arena-chat` - Chat real-time

### Admin & Merchant Operations (Mai cache)
- Tutte le operazioni `POST/PUT/DELETE` sotto `/api/admin/**`
- Tutte le operazioni `POST/PUT/DELETE` sotto `/api/merchant/**`
- Operazioni batch `/api/batch/**`

---

## Elenco Completo API per Controller

### 1. HealthController
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/health` | ❌ | 🟢 30s |

### 2. JwtAuthenticationController (`/api/auth`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| POST | `/api/auth/login` | ❌ | 🔴 No |
| POST | `/api/auth/register` | ❌ | 🔴 No |
| POST | `/api/auth/refresh` | ❌ | 🔴 No |
| POST | `/api/auth/register-merchant` | ❌ | 🔴 No |
| POST | `/api/auth/create-admin` | ❌ | 🔴 No |
| POST | `/api/auth/forgot-password` | ❌ | 🔴 No |
| POST | `/api/auth/verify-otp` | ❌ | 🔴 No |
| POST | `/api/auth/reset-password` | ❌ | 🔴 No |
| POST | `/api/auth/verify-email` | ❌ | 🔴 No |
| POST | `/api/auth/resend-verification` | ❌ | 🔴 No |

### 3. UserController (`/api/users`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/users` | ✅ | 🔴 No |
| GET | `/api/users/{id}` | ✅ | 🔴 No |
| GET | `/api/users/search` | ✅ | 🟡 3m |
| GET | `/api/users/leaderboard` | ✅ | 🟡 10m |
| GET | `/api/users/leaderboard/collection` | ✅ | 🟡 10m |
| GET | `/api/users/leaderboard/tournaments` | ✅ | 🟡 10m |
| GET | `/api/users/leaderboard/active-players` | ✅ | 🟡 5m |
| GET | `/api/users/{id}/stats` | ✅ | 🔴 No |
| GET | `/api/users/{id}/favorite-tcgs` | ✅ | 🔴 No |
| POST | `/api/users` | ✅ | 🔴 No |
| PUT | `/api/users/{id}` | ✅ | 🔴 No |
| DELETE | `/api/users/{id}` | ✅ | 🔴 No |
| PATCH | `/api/users/{id}/profile` | ✅ | 🔴 No |
| PUT | `/api/users/{id}/profile-image` | ✅ | 🔴 No |
| PUT | `/api/users/{id}/device-token` | ✅ | 🔴 No |
| PUT | `/api/users/{id}/favorite-tcgs` | ✅ | 🔴 No |
| PUT | `/api/users/{id}/privacy` | ✅ | 🔴 No |
| PUT | `/api/users/{id}/location` | ✅ | 🔴 No |

### 4. UserActivityController
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/user-activities` | ✅ | 🔴 No |
| GET | `/api/user-activities/{userId}` | ✅ | 🔴 No |
| GET | `/api/user-activities/recent/global` | ✅ | 🟡 1m |

### 5. AchievementController (`/api/achievements`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/achievements` | ✅ | 🟢 12h |
| GET | `/api/achievements/{id}` | ✅ | 🟢 12h |
| GET | `/api/achievements/user/{userId}` | ✅ | 🔴 No |
| POST | `/api/achievements` | ✅ Admin | 🔴 No |
| POST | `/api/achievements/{id}/unlock` | ✅ | 🔴 No |

### 6. CardController (`/api/cards/templates`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/cards/templates` | ✅ | 🟢 6h |
| GET | `/api/cards/templates/{id}` | ✅ | 🟢 12h |
| GET | `/api/cards/templates/search` | ✅ | 🟢 30m |
| GET | `/api/cards/templates/unified-search` | ✅ | 🟢 30m |
| GET | `/api/cards/templates/search/advanced` | ✅ | 🟢 30m |
| GET | `/api/cards/templates/filters/tcg-types` | ✅ | 🟢 24h |
| GET | `/api/cards/templates/filters/rarities` | ✅ | 🟢 24h |
| GET | `/api/cards/templates/filters/set-codes` | ✅ | 🟢 6h |
| GET | `/api/cards/templates/{id}/market-price` | ✅ | 🟢 5m |
| POST | `/api/cards/templates` | ✅ | 🔴 No |
| POST | `/api/cards/templates/smart-scan` | ✅ | 🔴 No |
| PUT | `/api/cards/templates/{id}` | ✅ | 🔴 No |
| DELETE | `/api/cards/templates/{id}` | ✅ | 🔴 No |

### 7. UserCardController (`/api/cards/collection`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/cards/collection` | ✅ | 🔴 No (personale) |
| GET | `/api/cards/collection/{userCardId}/decks` | ✅ | 🔴 No |
| POST | `/api/cards/{cardTemplateId}/add-to-collection` | ✅ | 🔴 No |
| DELETE | `/api/cards/collection/{userCardId}` | ✅ | 🔴 No |
| PUT | `/api/cards/collection/{userCardId}` | ✅ | 🔴 No |
| POST | `/api/cards/collection/{userCardId}/assign-deck` | ✅ | 🔴 No |
| DELETE | `/api/cards/collection/{userCardId}/remove-from-deck` | ✅ | 🔴 No |

### 8. DeckController (`/api/decks`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/decks` | ✅ | 🔴 No (personale) |
| GET | `/api/decks/public` | ✅ | 🟡 5m |
| GET | `/api/decks/{id}` | ✅ | 🔴 No |
| GET | `/api/decks/collection` | ✅ | 🔴 No |
| POST | `/api/decks` | ✅ | 🔴 No |
| POST | `/api/decks/create` | ✅ | 🔴 No |
| PUT | `/api/decks/{id}` | ✅ | 🔴 No |
| PUT | `/api/decks/{id}/visibility` | ✅ | 🔴 No |
| DELETE | `/api/decks/{id}` | ✅ | 🔴 No |
| POST | `/api/decks/{id}/cards` | ✅ | 🔴 No |
| POST | `/api/decks/{id}/add-template` | ✅ | 🔴 No |
| DELETE | `/api/decks/{id}/cards/{deckCardId}` | ✅ | 🔴 No |

### 9. ExpansionController (`/api/expansions`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/expansions` | ✅ | 🟢 12h |
| GET | `/api/expansions/{id}` | ✅ | 🟢 24h |
| GET | `/api/expansions/{id}/cards` | ✅ | 🟢 12h |
| GET | `/api/expansions/recent` | ✅ | 🟢 6h |
| GET | `/api/expansions/stats` | ✅ | 🟢 6h |
| POST | `/api/expansions` | ✅ | 🔴 No |
| PUT | `/api/expansions/{id}` | ✅ | 🔴 No |
| DELETE | `/api/expansions/{id}` | ✅ | 🔴 No |

### 10. TCGSetController (`/api/sets`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/sets` | ✅ | 🟢 12h |
| GET | `/api/sets/{id}` | ✅ | 🟢 24h |
| GET | `/api/sets/{id}/card-templates` | ✅ | 🟢 12h |
| GET | `/api/sets/by-code/{code}/cards` | ✅ | 🟢 12h |
| POST | `/api/sets` | ✅ | 🔴 No |
| PUT | `/api/sets/{id}` | ✅ | 🔴 No |
| DELETE | `/api/sets/{id}` | ✅ | 🔴 No |
| POST | `/api/sets/sync-release-dates` | ✅ | 🔴 No |

### 11. ShopController (`/api/shops`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/shops` | ✅ | 🟢 15m |
| GET | `/api/shops/{id}` | ✅ | 🟢 30m |
| GET | `/api/shops/public/unverified/search` | ❌ | 🟢 15m |
| GET | `/api/shops/{shopId}/is-subscribed` | ✅ | 🔴 No |
| GET | `/api/shops/subscriptions` | ✅ | 🔴 No |
| GET | `/api/shops/user/{userId}/subscriptions` | ✅ | 🔴 No |
| GET | `/api/shops/{shopId}/subscribers` | ✅ Merchant | 🔴 No |
| GET | `/api/shops/{shopId}/subscriber-count` | ✅ | 🟡 5m |
| GET | `/api/shops/{shopId}/reservation-settings` | ✅ | 🟡 5m |
| POST | `/api/shops` | ✅ | 🔴 No |
| PUT | `/api/shops/{id}` | ✅ | 🔴 No |
| DELETE | `/api/shops/{id}` | ✅ | 🔴 No |
| POST | `/api/shops/{shopId}/subscribe` | ✅ | 🔴 No |
| DELETE | `/api/shops/{shopId}/subscribe` | ✅ | 🔴 No |
| POST | `/api/shops/suggest` | ✅ | 🔴 No |
| PUT | `/api/shops/{shopId}/reservation-settings` | ✅ | 🔴 No |

### 12. ShopNewsController
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/shops/{shopId}/news` | ❌ | 🟢 5m |
| GET | `/api/merchant/shops/{shopId}/news` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/shops/{shopId}/news/active` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/shops/{shopId}/news/future` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/shops/{shopId}/news/expired` | ✅ Merchant | 🔴 No |
| POST | `/api/merchant/shops/{shopId}/news` | ✅ Merchant | 🔴 No |
| PUT | `/api/merchant/shops/{shopId}/news/{newsId}` | ✅ Merchant | 🔴 No |
| DELETE | `/api/merchant/shops/{shopId}/news/{newsId}` | ✅ Merchant | 🔴 No |

### 13. ShopRewardController
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/shops/{shopId}/rewards` | ✅ | 🟢 10m |
| GET | `/api/shop-rewards/available` | ✅ | 🟡 5m |
| GET | `/api/shop-rewards/my-redemptions` | ✅ | 🔴 No |
| GET | `/api/shop-rewards/points-criteria` | ❌ | 🟢 1h |
| POST | `/api/shop-rewards/{rewardId}/redeem` | ✅ | 🔴 No |
| GET | `/api/merchant/rewards` | ✅ Merchant | 🔴 No |
| POST | `/api/merchant/rewards` | ✅ Merchant | 🔴 No |
| PUT | `/api/merchant/rewards/{rewardId}` | ✅ Merchant | 🔴 No |
| DELETE | `/api/merchant/rewards/{rewardId}` | ✅ Merchant | 🔴 No |
| PATCH | `/api/merchant/rewards/{rewardId}/toggle` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/redemptions` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/redemptions/pending` | ✅ Merchant | 🔴 No |
| POST | `/api/merchant/redemptions/{id}/fulfill` | ✅ Merchant | 🔴 No |
| POST | `/api/merchant/redemptions/{id}/cancel` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/redemptions/code/{code}` | ✅ Merchant | 🔴 No |

### 14. MerchantBackofficeController (`/api/merchant`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/merchant/shop/status` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/dashboard/stats` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/dashboard/notifications` | ✅ Merchant | 🔴 No |
| GET | `/api/merchant/profile` | ✅ Merchant | 🔴 No |
| PUT | `/api/merchant/shop/{shopId}` | ✅ Merchant | 🔴 No |
| POST | `/api/merchant/shop/photo` | ✅ Merchant | 🔴 No |

### 15. TournamentController (`/api/tournaments`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/tournaments` | ✅ | 🟡 2m |
| GET | `/api/tournaments/{id}` | ✅ | 🟡 1m |
| GET | `/api/tournaments/upcoming` | ✅ | 🟡 2m |
| GET | `/api/tournaments/nearby` | ✅ | 🟡 2m |
| GET | `/api/tournaments/past` | ✅ | 🟡 5m |
| GET | `/api/tournaments/pending-requests` | ✅ Merchant | 🔴 No |
| GET | `/api/tournaments/code/{code}` | ❌ | 🟡 1m |
| GET | `/api/tournaments/{id}/participants` | ✅ | 🟡 30s |
| GET | `/api/tournaments/{id}/participants/registered` | ✅ | 🟡 30s |
| GET | `/api/tournaments/{id}/participants/waiting` | ✅ | 🟡 30s |
| GET | `/api/tournaments/{id}/participants/detailed` | ✅ | 🟡 30s |
| GET | `/api/tournaments/{id}/updates` | ❌ | 🟡 30s |
| GET | `/api/tournaments/{id}/updates/count` | ❌ | 🟡 30s |
| POST | `/api/tournaments` | ✅ Merchant | 🔴 No |
| PUT | `/api/tournaments/{id}` | ✅ | 🔴 No |
| DELETE | `/api/tournaments/{id}` | ✅ | 🔴 No |
| POST | `/api/tournaments/{id}/register` | ✅ | 🔴 No |
| DELETE | `/api/tournaments/{id}/register` | ✅ | 🔴 No |
| POST | `/api/tournaments/code/{code}/register` | ❌ | 🔴 No |
| POST | `/api/tournaments/{id}/participants/manual` | ✅ Merchant | 🔴 No |
| POST | `/api/tournaments/{id}/participants/add` | ✅ Merchant | 🔴 No |
| POST | `/api/tournaments/checkin` | ✅ | 🔴 No |
| POST | `/api/tournaments/{id}/self-checkin` | ✅ | 🔴 No |
| POST | `/api/tournaments/{id}/start` | ✅ Organizer | 🔴 No |
| POST | `/api/tournaments/{id}/complete` | ✅ Organizer | 🔴 No |
| DELETE | `/api/tournaments/{id}/participants/{pId}` | ✅ Organizer | 🔴 No |
| POST | `/api/tournaments/{id}/updates` | ✅ Organizer | 🔴 No |
| DELETE | `/api/tournaments/{id}/updates/{uId}` | ✅ Organizer | 🔴 No |
| POST | `/api/tournaments/{id}/request` | ✅ | 🔴 No |
| PUT | `/api/tournaments/{id}/approve` | ✅ Shop Owner | 🔴 No |
| PUT | `/api/tournaments/{id}/reject` | ✅ Shop Owner | 🔴 No |

### 16. ChatController (`/api/chat`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/chat` | ✅ | 🔴 No |
| GET | `/api/chat/{id}/messages` | ✅ | 🔴 No |
| POST | `/api/chat/start` | ✅ | 🔴 No |
| POST | `/api/chat/{id}/send` | ✅ | 🔴 No |
| POST | `/api/chat/{id}/complete` | ✅ | 🔴 No |
| POST | `/api/chat/{id}/close-without-agreement` | ✅ | 🔴 No |
| POST | `/api/chat/{id}/read` | ✅ | 🔴 No |

### 17. GlobalChatController (`/api/global-chat`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/global-chat/messages` | ❌ | 🟡 15s |
| GET | `/api/global-chat/messages/after` | ❌ | 🟡 10s |
| GET | `/api/global-chat/rate-limit` | ❌ | 🟡 5s |
| POST | `/api/global-chat/messages` | ✅ | 🔴 No |
| WebSocket | `/app/arena-chat` | ✅ | 🔴 No |

### 18. TradeController (`/api/trade`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/trade/list` | ✅ | 🔴 No |
| GET | `/api/trade/user/{userId}` | ❌ | 🟡 2m |
| GET | `/api/trade/matches` | ✅ | 🔴 No |
| GET | `/api/trade/listings/public` | ❌ | 🟡 2m |
| GET | `/api/trade/chat/{matchId}` | ✅ | 🔴 No |
| POST | `/api/trade/add` | ✅ | 🔴 No |
| POST | `/api/trade/remove` | ✅ | 🔴 No |
| POST | `/api/trade/chat/{matchId}/send` | ✅ | 🔴 No |
| POST | `/api/trade/chat/{matchId}/start` | ✅ | 🔴 No |
| POST | `/api/trade/complete/{matchId}` | ✅ | 🔴 No |
| POST | `/api/trade/cancel/{matchId}` | ✅ | 🔴 No |

### 19. CommunityEventController (`/api/community/events`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/community/events` | ✅ | 🟡 2m |
| GET | `/api/community/events/{id}` | ✅ | 🟡 1m |
| GET | `/api/community/events/my/created` | ✅ | 🔴 No |
| GET | `/api/community/events/my/joined` | ✅ | 🔴 No |
| POST | `/api/community/events` | ✅ | 🔴 No |
| POST | `/api/community/events/{id}/join` | ✅ | 🔴 No |
| DELETE | `/api/community/events/{id}/join` | ✅ | 🔴 No |
| DELETE | `/api/community/events/{id}` | ✅ | 🔴 No |

### 20. CommunityThreadController (`/api/threads`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/threads` | Partial | 🟡 2m |
| GET | `/api/threads/{id}` | Partial | 🟡 1m |
| GET | `/api/threads/{id}/can-respond` | ✅ | 🔴 No |
| POST | `/api/threads` | ✅ | 🔴 No |
| POST | `/api/threads/{id}/responses` | ✅ | 🔴 No |

### 21. CommunityPullController (`/api/community/pulls`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/community/pulls` | Partial | 🟡 1m |
| POST | `/api/community/pulls` | ✅ | 🔴 No |
| POST | `/api/community/pulls/{id}/like` | ✅ | 🔴 No |

### 22. CommunityStatsController
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/community/stats` | ✅ | 🔴 No |

### 23. RadarController (`/api/radar`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/radar/nearby` | ✅ | 🔴 No (realtime) |
| PUT | `/api/radar/location` | ✅ | 🔴 No |
| POST | `/api/radar/ping/{userId}` | ✅ | 🔴 No |

### 24. RewardController (`/api/rewards`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/rewards` | ✅ | 🟡 10m |
| GET | `/api/rewards/partner/{partnerId}` | ✅ | 🟡 10m |
| GET | `/api/rewards/{id}` | ✅ | 🟡 10m |
| GET | `/api/rewards/user/{userId}/points` | ✅ | 🔴 No |
| GET | `/api/rewards/history` | ✅ | 🔴 No |
| POST | `/api/rewards/{id}/redeem` | ✅ | 🔴 No |
| POST | `/api/rewards` | ✅ Admin | 🔴 No |
| GET | `/api/rewards/admin/transactions` | ✅ Admin | 🔴 No |
| GET | `/api/rewards/admin/pending` | ✅ Admin | 🔴 No |
| PUT | `/api/rewards/admin/transactions/{id}` | ✅ Admin | 🔴 No |

### 25. PartnerController (`/api/partners`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/partners` | ✅ | 🟢 6h |
| GET | `/api/partners/all` | ✅ Admin | 🔴 No |
| GET | `/api/partners/{id}` | ✅ | 🟢 6h |
| POST | `/api/partners` | ✅ Admin | 🔴 No |
| DELETE | `/api/partners/{id}` | ✅ Admin | 🔴 No |

### 26. NotificationController (`/api/notifications`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/notifications` | ✅ | 🔴 No |
| GET | `/api/notifications/unread` | ✅ | 🔴 No |
| PUT | `/api/notifications/{id}/read` | ✅ | 🔴 No |
| POST | `/api/notifications/register-device` | ✅ | 🔴 No |
| DELETE | `/api/notifications/unregister-device` | ✅ | 🔴 No |
| POST | `/api/notifications/test-push` | ✅ | 🔴 No |
| POST | `/api/notifications/shop/{id}/broadcast` | ✅ Merchant | 🔴 No |
| POST | `/api/notifications/admin/clean-invalid-tokens` | ✅ Admin | 🔴 No |
| GET | `/api/notifications/admin/token-statistics` | ✅ Admin | 🔴 No |
| GET | `/api/notifications/admin/firebase-check` | ✅ Admin | 🔴 No |

### 27. ImageController (`/api/images`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/images/entity/{type}/{id}` | ✅ | 🟡 5m |
| GET | `/api/images/user/{userId}` | ✅ | 🔴 No |
| POST | `/api/images/upload` | ✅ | 🔴 No |
| DELETE | `/api/images/{id}` | ✅ | 🔴 No |

### 28. SuggestionController (`/api/suggestions`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/suggestions` | ✅ Admin | 🔴 No |
| POST | `/api/suggestions` | ✅ | 🔴 No |
| PUT | `/api/suggestions/{id}/read` | ✅ Admin | 🔴 No |

### 29. InventoryCardController (`/api/inventory`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/inventory` | ✅ | 🟡 5m |
| GET | `/api/inventory/{id}` | ✅ | 🟡 5m |
| GET | `/api/inventory/template/download` | ✅ | 🟢 24h |
| GET | `/api/inventory/stats` | ✅ Merchant | 🔴 No |
| GET | `/api/inventory/low-stock` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory` | ✅ Merchant | 🔴 No |
| PUT | `/api/inventory/{id}` | ✅ Merchant | 🔴 No |
| DELETE | `/api/inventory/{id}` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory/bulk-import` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory/ai-import` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory/bulk-add-by-set/{setId}` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory/bulk-add-by-expansion/{expansionId}` | ✅ Merchant | 🔴 No |
| POST | `/api/inventory/bulk-add-by-templates` | ✅ Merchant | 🔴 No |

### 30. ReservationController (`/api/reservations`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/reservations` | ✅ | 🔴 No |
| GET | `/api/reservations/my` | ✅ | 🔴 No |
| GET | `/api/reservations/user/{userId}` | ✅ | 🔴 No |
| GET | `/api/reservations/merchant` | ✅ Merchant | 🔴 No |
| POST | `/api/reservations` | ✅ | 🔴 No |
| POST | `/api/reservations/validate` | ✅ Merchant | 🔴 No |
| PUT | `/api/reservations/{id}/validate` | ✅ Merchant | 🔴 No |
| PUT | `/api/reservations/{id}/complete` | ✅ Merchant | 🔴 No |
| PUT | `/api/reservations/{id}/cancel` | ✅ | 🔴 No |

### 31. CustomerRequestController (`/api/requests`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/requests` | ✅ | 🔴 No |
| GET | `/api/requests/{id}` | ✅ | 🔴 No |
| GET | `/api/requests/{id}/messages` | ✅ | 🔴 No |
| GET | `/api/requests/merchant/stats` | ✅ Merchant | 🔴 No |
| POST | `/api/requests` | ✅ | 🔴 No |
| PUT | `/api/requests/{id}/status` | ✅ Merchant | 🔴 No |
| POST | `/api/requests/{id}/cancel` | ✅ | 🔴 No |
| POST | `/api/requests/{id}/messages` | ✅ | 🔴 No |
| POST | `/api/requests/{id}/messages/merchant` | ✅ Merchant | 🔴 No |
| POST | `/api/requests/{id}/read` | ✅ | 🔴 No |

### 32. PendingReviewController (`/api/reviews/pending`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/reviews/pending` | ✅ | 🔴 No |
| GET | `/api/reviews/pending/count` | ✅ | 🔴 No |
| POST | `/api/reviews/pending/{id}/submit` | ✅ | 🔴 No |

### 33. WaitingListController (`/api/waiting-list`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| POST | `/api/waiting-list/join` | ❌ | 🔴 No |
| GET | `/api/waiting-list/all` | ✅ Admin | 🔴 No |
| GET | `/api/waiting-list/uncontacted` | ✅ Admin | 🔴 No |
| PUT | `/api/waiting-list/{id}/contacted` | ✅ Admin | 🔴 No |

### 34. WalletController (`/api/wallet`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/wallet/pass` | ✅ | 🔴 No |

### 35. HomeDashboardController (`/api/home/dashboard`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/home/dashboard` | ✅ | 🟡 2m (per user) |

### 36. ProDeckController (`/api/pro-decks`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/pro-decks` | ✅ | 🟢 6h |
| GET | `/api/pro-decks/{id}` | ✅ | 🟢 6h |
| GET | `/api/pro-decks/recent` | ✅ | 🟢 1h |

### 37. BatchController (`/api/batch`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/batch/justtcg/supported` | ✅ | 🟢 24h |
| GET | `/api/batch/justtcg/games` | ✅ | 🟢 24h |
| GET | `/api/batch/justtcg/status` | ✅ Admin | 🔴 No |
| POST | `/api/batch/import/{tcgType}` | ✅ Admin | 🔴 No |
| POST | `/api/batch/justtcg/import` | ✅ Admin | 🔴 No |

### 38. PublicController (`/api/public`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/public/shops/{id}` | ❌ | 🟢 30m |
| GET | `/api/public/tournaments/{id}` | ❌ | 🟡 5m |
| GET | `/api/public/events/{id}` | ❌ | 🟡 5m |
| GET | `/api/public/cards/{id}` | ❌ | 🟢 30m |

### 39. ArenaApiController (`/api/arena`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/arena/games` | API Key | 🟢 24h |
| GET | `/api/arena/games/{id}` | API Key | 🟢 24h |
| GET | `/api/arena/sets` | API Key | 🟢 12h |
| GET | `/api/arena/sets/{id}` | API Key | 🟢 12h |
| GET | `/api/arena/cards` | API Key | 🟢 6h |
| GET | `/api/arena/cards/{id}` | API Key | 🟢 12h |

### 40. ArenaApiKeyAdminController (`/api/admin/arena-keys`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/admin/arena-keys` | ✅ Admin | 🔴 No |
| GET | `/api/admin/arena-keys/{id}` | ✅ Admin | 🔴 No |
| POST | `/api/admin/arena-keys` | ✅ Admin | 🔴 No |
| PUT | `/api/admin/arena-keys/{id}` | ✅ Admin | 🔴 No |
| DELETE | `/api/admin/arena-keys/{id}` | ✅ Admin | 🔴 No |
| POST | `/api/admin/arena-keys/{id}/regenerate` | ✅ Admin | 🔴 No |
| POST | `/api/admin/arena-keys/{id}/toggle` | ✅ Admin | 🔴 No |

### 41. AdminShopPopulationController (`/api/admin/shops`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/admin/shops/google-status` | ❌ | 🟡 5m |
| POST | `/api/admin/shops/populate-from-google` | API Key | 🔴 No |

### 42. CardImageAdminController (`/api/admin/images`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/admin/images/status` | ✅ Admin | 🔴 No |
| POST | `/api/admin/images/sync` | ✅ Admin | 🔴 No |

### 43. AdminController (`/api/admin`)
| Metodo | Endpoint | Auth | Cache |
|--------|----------|------|-------|
| GET | `/api/admin/shops` | ✅ | 🔴 No |
| GET | `/api/admin/shops/pending` | ✅ | 🔴 No |
| GET | `/api/admin/shops/stats` | ✅ | 🔴 No |
| GET | `/api/admin/shop-suggestions` | ✅ | 🔴 No |
| GET | `/api/admin/broadcast-news` | ✅ | 🔴 No |
| GET | `/api/admin/broadcast-news/active` | ✅ | 🔴 No |
| GET | `/api/admin/broadcast-news/future` | ✅ | 🔴 No |
| GET | `/api/admin/broadcast-news/expired` | ✅ | 🔴 No |
| GET | `/api/admin/broadcast/recipients-count` | ✅ | 🔴 No |
| GET | `/api/admin/diagnostics/check-duplicates` | ✅ | 🔴 No |
| POST | `/api/admin/shops/{id}/activate` | ✅ | 🔴 No |
| POST | `/api/admin/shops/{id}/deactivate` | ✅ | 🔴 No |
| POST | `/api/admin/shops/{id}/reject` | ✅ | 🔴 No |
| PUT | `/api/admin/shops/{id}` | ✅ | 🔴 No |
| PUT | `/api/admin/shop-suggestions/{id}` | ✅ | 🔴 No |
| POST | `/api/admin/rewards` | ✅ | 🔴 No |
| PUT | `/api/admin/rewards/{id}` | ✅ | 🔴 No |
| DELETE | `/api/admin/rewards/{id}` | ✅ | 🔴 No |
| POST | `/api/admin/achievements` | ✅ | 🔴 No |
| PUT | `/api/admin/achievements/{id}` | ✅ | 🔴 No |
| DELETE | `/api/admin/achievements/{id}` | ✅ | 🔴 No |
| POST | `/api/admin/batch/import/{tcgType}` | ✅ | 🔴 No |
| POST | `/api/admin/broadcast/send` | ✅ | 🔴 No |
| POST | `/api/admin/broadcast-news` | ✅ | 🔴 No |
| PUT | `/api/admin/broadcast-news/{id}` | ✅ | 🔴 No |
| DELETE | `/api/admin/broadcast-news/{id}` | ✅ | 🔴 No |

---

## Implementazione Consigliata

### Configurazione Caffeine Cache (già presente nel progetto)

```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(10000);
    }

    // Cache specifiche
    @Bean
    public Cache<String, Object> cardTemplatesCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(50000)
            .build();
    }

    @Bean
    public Cache<String, Object> expansionsCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(12, TimeUnit.HOURS)
            .maximumSize(1000)
            .build();
    }

    @Bean
    public Cache<String, Object> leaderboardCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .maximumSize(100)
            .build();
    }

    @Bean
    public Cache<String, Object> proDeckCache() {
        return Caffeine.newBuilder()
            .expireAfterWrite(6, TimeUnit.HOURS)
            .maximumSize(500)
            .build();
    }
}
```

### Annotazioni nei Service

```java
// Esempio per CardTemplateService
@Cacheable(value = "cardTemplates", key = "#id")
public CardTemplate getCardTemplateById(Long id) { ... }

@Cacheable(value = "cardTemplateSearch", key = "#query + '_' + #page + '_' + #size")
public Page<CardTemplate> searchCardTemplates(String query, int page, int size) { ... }

@CacheEvict(value = {"cardTemplates", "cardTemplateSearch"}, allEntries = true)
public CardTemplate updateCardTemplate(Long id, CardTemplate template) { ... }
```

---

## HTTP Cache Headers Consigliati

Per le API pubbliche, aggiungere header HTTP:

```java
@GetMapping("/api/public/cards/{id}")
public ResponseEntity<CardDTO> getPublicCard(@PathVariable Long id) {
    CardDTO card = cardService.getCard(id);
    return ResponseEntity.ok()
        .cacheControl(CacheControl.maxAge(30, TimeUnit.MINUTES)
            .cachePublic())
        .body(card);
}
```

---

## Legenda

| Simbolo | Significato |
|---------|-------------|
| 🟢 | Altamente cacheable (TTL lungo) |
| 🟡 | Moderatamente cacheable (TTL breve) |
| 🔴 | Non cacheable |
| ✅ | Autenticazione richiesta |
| ❌ | Pubblico (no auth) |
| Admin | Solo amministratore |
| Merchant | Solo merchant |
| API Key | Protetto da API key |
