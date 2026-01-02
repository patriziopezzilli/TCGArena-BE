#!/bin/bash

# =============================================================================
# TCG Arena Backend - Linux Server Deploy Script
# =============================================================================
# Questo script esegue il deploy del backend in sequenza:
# 1. Ferma il processo Java esistente
# 2. Aggiorna il codice con git pull
# 3. Compila con Maven
# 4. Avvia l'applicazione in background
# =============================================================================

set -e  # Esce in caso di errore

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Directory del progetto (dove si trova questo script)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
JAR_NAME="tcg-arena-backend-0.0.1-SNAPSHOT.jar"

echo -e "${BLUE}=============================================${NC}"
echo -e "${BLUE}   TCG Arena Backend - Linux Deploy${NC}"
echo -e "${BLUE}=============================================${NC}"
echo ""

# -----------------------------------------------------------------------------
# STEP 1: Ferma il processo Java esistente
# -----------------------------------------------------------------------------
echo -e "${YELLOW}[1/4] Cercando processi Java TCG Arena...${NC}"

# Trova il PID del processo che contiene "tcg-arena"
OLD_PID=$(ps aux | grep -i "tcg-arena" | grep -v grep | grep java | awk '{print $2}' || true)

if [ -n "$OLD_PID" ]; then
    echo -e "       Trovato processo con PID: ${RED}$OLD_PID${NC}"
    echo -e "       Terminando il processo..."
    kill $OLD_PID
    sleep 2
    
    # Verifica se il processo è stato terminato
    if ps -p $OLD_PID > /dev/null 2>&1; then
        echo -e "       ${YELLOW}Processo ancora attivo, forzo terminazione...${NC}"
        kill -9 $OLD_PID
        sleep 1
    fi
    
    echo -e "       ${GREEN}✓ Processo terminato${NC}"
else
    echo -e "       ${GREEN}✓ Nessun processo TCG Arena in esecuzione${NC}"
fi

echo ""

# -----------------------------------------------------------------------------
# STEP 2: Git Pull
# -----------------------------------------------------------------------------
echo -e "${YELLOW}[2/4] Aggiornando il codice da Git...${NC}"

cd "$SCRIPT_DIR"
git pull

echo -e "       ${GREEN}✓ Codice aggiornato${NC}"
echo ""

# -----------------------------------------------------------------------------
# STEP 3: Maven Build
# -----------------------------------------------------------------------------
echo -e "${YELLOW}[3/4] Compilando con Maven...${NC}"

mvn clean install -DskipTests

echo -e "       ${GREEN}✓ Build completata${NC}"
echo ""

# -----------------------------------------------------------------------------
# STEP 4: Avvia l'applicazione
# -----------------------------------------------------------------------------
echo -e "${YELLOW}[4/4] Avviando l'applicazione...${NC}"

cd "$SCRIPT_DIR/target"

# Avvia in background con nohup
export FIREBASE_SERVICE_ACCOUNT_PATH="/root/TCGArena-BE/firebase-service-account.json"
nohup java -jar $JAR_NAME > app.log 2>&1 &

NEW_PID=$!
echo -e "       ${GREEN}✓ Applicazione avviata con PID: $NEW_PID${NC}"
echo ""

# Attendi qualche secondo e verifica che l'app sia partita
sleep 3

if ps -p $NEW_PID > /dev/null 2>&1; then
    echo -e "${GREEN}=============================================${NC}"
    echo -e "${GREEN}   ✓ Deploy completato con successo!${NC}"
    echo -e "${GREEN}=============================================${NC}"
    echo ""
    echo -e "Log disponibile in: ${BLUE}$SCRIPT_DIR/target/app.log${NC}"
    echo -e "Per seguire i log: ${BLUE}tail -f $SCRIPT_DIR/target/app.log${NC}"
    echo ""
else
    echo -e "${RED}=============================================${NC}"
    echo -e "${RED}   ✗ ERRORE: L'applicazione non è partita${NC}"
    echo -e "${RED}=============================================${NC}"
    echo ""
    echo -e "Controlla i log: ${BLUE}cat $SCRIPT_DIR/target/app.log${NC}"
    exit 1
fi
