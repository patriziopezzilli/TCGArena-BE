#!/bin/bash

###############################################################################
# TCG Arena - Shop Population Script (PUBLIC ENDPOINT)
# Semplice script per popolare negozi su api.tcgarena.it
###############################################################################

# Configurazione
API_BASE_URL="https://api.tcgarena.it"
SECRET_KEY="tcgarena_shops_2026_secret"  # Chiave configurata sul server

# Colori
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo "================================================"
echo "  TCG Arena - Shop Population"
echo "================================================"
echo ""

# Step 1: Test Connessione
echo -e "${YELLOW}Step 1: Testing connection...${NC}"

STATUS=$(curl -s -X GET "${API_BASE_URL}/api/admin/shops/google-places-status")
echo "$STATUS" | jq '.' 2>/dev/null || echo "$STATUS"
echo ""

# Step 2: Dry Run Test
echo -e "${YELLOW}Step 2: Running dry-run test (50 requests)...${NC}"

DRY_RUN=$(curl -s -X POST "${API_BASE_URL}/api/admin/shops/populate-from-google?dryRun=true&maxRequests=50&skipPlaceDetails=true&apiKey=${SECRET_KEY}")

echo "$DRY_RUN" | jq '.' 2>/dev/null || echo "$DRY_RUN"
echo ""

# Verifica se dry run ha avuto successo
if echo "$DRY_RUN" | grep -q "error"; then
    echo -e "${RED}ERROR: Dry run failed!${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Dry run successful${NC}"
echo ""

# Chiedi conferma
read -p "Procedere con la popolazione completa? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operazione annullata."
    exit 0
fi

# Step 3: Popolazione Reale
echo ""
echo -e "${YELLOW}Step 3: Populating shops from Google Places...${NC}"
echo -e "${BLUE}Questo richiederà circa 20-25 minuti...${NC}"
echo ""

START_TIME=$(date +%s)

RESULT=$(curl -s -X POST "${API_BASE_URL}/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950&apiKey=${SECRET_KEY}")

END_TIME=$(date +%s)
DURATION=$((END_TIME - START_TIME))

echo ""
echo -e "${GREEN}✓ Completato in ${DURATION} secondi${NC}"
echo ""

# Mostra risultati
echo "================================================"
echo "  RISULTATI"
echo "================================================"
echo "$RESULT" | jq '.' 2>/dev/null || echo "$RESULT"
echo ""

# Estrai statistiche
TOTAL_FOUND=$(echo "$RESULT" | grep -o '"totalFound":[0-9]*' | cut -d':' -f2)
TOTAL_INSERTED=$(echo "$RESULT" | grep -o '"totalInserted":[0-9]*' | cut -d':' -f2)
API_CALLS=$(echo "$RESULT" | grep -o '"apiRequestsUsed":[0-9]*' | cut -d':' -f2)

echo "================================================"
echo "  SUMMARY"
echo "================================================"
echo -e "${GREEN}Negozi trovati:${NC} $TOTAL_FOUND"
echo -e "${GREEN}Negozi inseriti:${NC} $TOTAL_INSERTED"
echo -e "${BLUE}API calls usate:${NC} $API_CALLS"
echo -e "${YELLOW}Tempo impiegato:${NC} ${DURATION}s"
echo "================================================"
echo ""

echo -e "${GREEN}✓ Deployment completato!${NC}"
echo ""
echo "PROSSIMI PASSI:"
echo "1. Verifica i negozi nella dashboard admin"
echo "2. Attiva manualmente i negozi verificati"
echo "3. I negozi sono inseriti come 'inactive' e 'not verified'"
echo ""
