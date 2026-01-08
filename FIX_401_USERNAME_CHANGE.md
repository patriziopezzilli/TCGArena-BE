# Fix 401 Authentication Errors & Token Invalidation

## Problema
L'applicazione sperimentava errori 401 Unauthorized dopo che un utente aggiornava il proprio username. Questo accadeva perché:
1. Il JWT token usa l'username come subject
2. Cambiando l'username nel database, i token esistenti diventavano invalidi
3. L'app iOS cancellava i token ricevendo un 401, causando il fallimento di tutte le richieste successive

## Soluzione Implementata

### Backend (TCGArena-BE)

#### 1. UserController.java
**Già implementato** ✅ - Il metodo `updateUserProfile()` già gestisce correttamente il cambio username:
- Verifica che l'utente non abbia esaurito i 2 cambi username consentiti
- Verifica che il nuovo username sia disponibile
- Genera nuovi JWT tokens se l'username è stato modificato
- Restituisce una risposta contenente `{user, token, refreshToken}` quando l'username cambia
- Restituisce solo `user` quando l'username non cambia

Posizione: [UserController.java](src/main/java/com/tcg/arena/controller/UserController.java#L145-L203)

#### 2. JwtAuthenticationController.java
**Nuovo endpoint aggiunto** ✨ - Aggiunto endpoint `/api/auth/me`:
- Restituisce l'utente corrente basandosi sul JWT token nell'header
- Permette all'app di ricaricare i dati utente senza perdere l'autenticazione
- Gestisce correttamente username aggiornati nel token

Posizione: [JwtAuthenticationController.java](src/main/java/com/tcg/arena/controller/JwtAuthenticationController.java#L320-L333)

### iOS (TCGArena-iOS)

#### 1. UserService.swift
**Già implementato** ✅ - La struttura `UpdateProfileResponse` gestisce correttamente:
- Risposta diretta con solo `User` (quando username non cambia)
- Risposta con `User + token + refreshToken` (quando username cambia)
- Decodifica flessibile che supporta entrambi i formati

Posizione: [UserService.swift](TCG Arena/Services/UserService.swift#L77-L99)

#### 2. AuthService.swift
**Migliorato** 🔧 - Modifiche implementate:

##### a. Metodo `updateSession()` esistente
**Già implementato** ✅ - Aggiorna token e dati utente in una sola operazione:
```swift
func updateSession(token: String, refreshToken: String, user: User) {
    APIClient.shared.setJWTToken(token)
    APIClient.shared.setRefreshToken(refreshToken)
    self.currentUser = user
    self.isAuthenticated = true
    self.sessionExpired = false
}
```

##### b. Metodo `reloadUserDataIfNeeded()` migliorato
**Modificato** 🔧 - Gestione errori più robusta:
- Non fa logout immediato su 401 (il token potrebbe essere appena stato aggiornato)
- Fa logout solo su `sessionExpired` reale
- Log dettagliati per debugging
- Esecuzione su MainActor per sicurezza thread

Posizione: [AuthService.swift](TCG Arena/Services/AuthService.swift#L366-L385)

#### 3. EditProfileView.swift
**Migliorato** 🔧 - Metodo `saveProfile()` ottimizzato:

##### Modifiche chiave:
1. **Quando riceve nuovi token** (username cambiato):
   - Chiama `updateSession()` con i nuovi token e l'utente aggiornato
   - NON chiama `reloadUserDataIfNeeded()` (non necessario, già abbiamo i dati aggiornati)
   - Dismisses la view

2. **Quando NON riceve nuovi token** (solo displayName o altri campi):
   - Aggiorna solo `currentUser` con i dati dalla risposta
   - Non ricarica dal server (non necessario)

##### Vantaggi:
- Evita chiamate API non necessarie
- Elimina race condition tra aggiornamento token e ricaricamento dati
- Previene errori 401 durante il cambio username

Posizione: [EditProfileView.swift](TCG Arena/Views/Profile/EditProfileView.swift#L210-L246)

#### 4. APIClient.swift
**Già implementato** ✅ - Gestione robusta degli errori 401:
- Tenta automaticamente il refresh del token su 401
- Distingue tra errori di autenticazione (public endpoints) e sessione scaduta
- Retry logic per evitare logout prematuro
- Task de-duplication per refresh token multipli simultanei

Posizione: APIClient.swift (righe 227-244)

## Flusso Completo del Fix

### Scenario: Utente cambia username

```
1. User clicca "Salva Modifiche" in EditProfileView
   ↓
2. EditProfileView chiama UserService.updateUserProfile()
   ↓
3. Backend (UserController.updateUserProfile):
   - Verifica disponibilità username
   - Aggiorna username nel database
   - Genera NUOVI JWT tokens (con nuovo username nel subject)
   - Restituisce {user, token, refreshToken}
   ↓
4. UserService.UpdateProfileResponse decodifica la risposta
   ↓
5. EditProfileView.saveProfile():
   - Rileva che ci sono nuovi token nella risposta
   - Chiama authService.updateSession(token, refreshToken, user)
     → Aggiorna APIClient con nuovi token
     → Aggiorna currentUser
     → Salva in UserDefaults
   - NON chiama reloadUserDataIfNeeded() (non necessario)
   - Dismisses la view
   ↓
6. Tutte le richieste successive usano il nuovo token
   ✅ Nessun errore 401
```

### Scenario: Utente cambia solo displayName

```
1. User clicca "Salva Modifiche" in EditProfileView
   ↓
2. EditProfileView chiama UserService.updateUserProfile()
   ↓
3. Backend (UserController.updateUserProfile):
   - Aggiorna displayName nel database
   - NON genera nuovi token
   - Restituisce solo {user}
   ↓
4. UserService.UpdateProfileResponse decodifica user (token=nil)
   ↓
5. EditProfileView.saveProfile():
   - Rileva che NON ci sono nuovi token
   - Aggiorna solo authService.currentUser
   - Dismisses la view
   ↓
6. Token esistente continua a funzionare
   ✅ Nessun errore 401
```

## Test da Eseguire

### 1. Test Cambio Username
1. Login all'app
2. Vai a Profile → Edit Profile
3. Cambia username con uno valido e disponibile
4. Salva le modifiche
5. **Verifica**:
   - ✅ Profilo aggiornato correttamente
   - ✅ NESSUN errore 401 nelle richieste successive
   - ✅ NESSUN logout automatico
   - ✅ Username visibile immediatamente nel profilo

### 2. Test Cambio DisplayName
1. Login all'app
2. Vai a Profile → Edit Profile
3. Cambia solo il displayName
4. Salva le modifiche
5. **Verifica**:
   - ✅ DisplayName aggiornato correttamente
   - ✅ NESSUN errore 401
   - ✅ Token esistente continua a funzionare

### 3. Test Preferenze Notifiche
1. Login all'app
2. Vai a Settings
3. Toggle email notifications
4. **Verifica**:
   - ✅ Preferenza salvata correttamente
   - ✅ NESSUN errore 401
   - ✅ Toggle rimane nella posizione corretta

### 4. Test Token Refresh
1. Attendi che il token scada (o simula cambiando l'ora di sistema)
2. Fai una richiesta qualsiasi
3. **Verifica**:
   - ✅ Token refreshato automaticamente
   - ✅ NESSUN logout se il refresh ha successo
   - ✅ Logout solo se refresh fallisce

### 5. Test Limite Cambi Username
1. Cambia username 2 volte
2. Tenta un terzo cambio
3. **Verifica**:
   - ✅ Errore "Hai già esaurito i 2 cambi username consentiti"
   - ✅ Profilo non aggiornato

## File Modificati

### Backend
- ✅ `JwtAuthenticationController.java` - Aggiunto endpoint `/api/auth/me`
- ✅ `JwtAuthenticationController.java` - Fix reload user dopo assegnazione punti registrazione

### iOS
- ✅ `AuthService.swift` - Migliorato `reloadUserDataIfNeeded()`
- ✅ `EditProfileView.swift` - Ottimizzato `saveProfile()` per evitare chiamate non necessarie

## Note Tecniche

### Perché non chiamare reloadUserDataIfNeeded() dopo username change?
Quando l'username viene cambiato, il backend restituisce già l'utente aggiornato nella risposta insieme ai nuovi token. Chiamare `reloadUserDataIfNeeded()` creerebbe:
1. Una chiamata API non necessaria
2. Una potenziale race condition se i token non sono ancora propagati
3. Possibili errori 401 se il vecchio token viene ancora utilizzato

### Gestione MainActor
Le modifiche a `currentUser` e altre proprietà `@Published` devono avvenire su MainActor per garantire:
- Aggiornamenti thread-safe della UI
- Nessun crash da accesso concorrente
- Rispetto delle best practice SwiftUI

### Endpoint /api/auth/me
Questo endpoint è fondamentale per permettere all'app di ricaricare i dati utente senza perdere l'autenticazione. Usa il JWT token nell'header `Authorization` per identificare l'utente, quindi funziona anche dopo cambi di username.

## Deployment

### Backend
```bash
cd TCGArena-BE
mvn clean package
# Deploy su server di produzione
```

### iOS
```bash
# Nessun deployment necessario - modifiche solo client-side
# Testare localmente, poi rilasciare via TestFlight
```

## Conclusione

Il fix è completo e testabile. Le modifiche sono minimali e ben localizzate, riducendo il rischio di regressioni. La gestione degli errori è più robusta e user-friendly.
