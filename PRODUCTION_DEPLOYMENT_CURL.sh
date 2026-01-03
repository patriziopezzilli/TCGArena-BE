#!/bin/bash

###############################################################################
# TCG Arena - Shop Population Deployment Script
# 
# Esegue la popolazione dei negozi TCG su api.tcgarena.it
# NOTA: Richiede autenticazione ADMIN
###############################################################################

# Configurazione
API_BASE_URL="https://api.tcgarena.it"
ADMIN_EMAIL="admin@tcgarena.com"  # Modifica con email admin
ADMIN_PASSWORD=""  # Inserisci password admin

# Colori per output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "================================================"
echo "  TCG Arena - Shop Population Script"
echo "================================================"
echo ""

# Step 1: Login come Admin per ottenere JWT token
echo -e "${YELLOW}Step 1: Authenticating as admin...${NC}"

if [ -z "$ADMIN_PASSWORD" ]; then
    echo -e "${RED}ERROR: ADMIN_PASSWORD non configurata!${NC}"
    echo "Modifica questo script e inserisci la password admin"
    exit 1
fi

LOGIN_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/api/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\"}")

# Estrai JWT token dalla response
JWT_TOKEN=$(echo $LOGIN_RESPONSE | grep -o '"token":"[^"]*' | cut -d'"' -f4)

if [ -z "$JWT_TOKEN" ]; then
    echo -e "${RED}ERROR: Login fallito!${NC}"
    echo "Response: $LOGIN_RESPONSE"
    exit 1
fi

echo -e "${GREEN}✓ Login successful${NC}"
echo "JWT Token: ${JWT_TOKEN:0:20}..."
echo ""

# Step 2: Test con Dry Run
echo -e "${YELLOW}Step 2: Testing with Dry Run (max 50 requests)...${NC}"

DRY_RUN_RESPONSE=$(curl -s -X POST "${API_BASE_URL}/api/admin/shops/populate-from-google?dryRun=true&maxRequests=50&skipPlaceDetails=true" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json")

echo "Dry Run Response:"
echo $DRY_RUN_RESPONSE | jq '.' 2>/dev/null || echo $DRY_RUN_RESPONSE
echo ""

# Chiedi conferma
read -p "Procedere con la popolazione reale? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "Operazione annullata."
    exit 0
fi

# Step 3: Popolazione Reale (Solo Nearby Search per risparmiare quota)
echo -e "${YELLOW}Step 3: Populating shops (Nearby Search only)...${NC}"
echo "Questo potrebbe richiedere diversi minuti..."
echo ""

RESPONSE=$(curl -s -X POST "${API_BASE_URL}/api/admin/shops/populate-from-google?dryRun=false&skipPlaceDetails=true&maxRequests=950" \
  -H "Authorization: Bearer ${JWT_TOKEN}" \
  -H "Content-Type: application/json")

echo -e "${GREEN}✓ Population completed${NC}"
echo ""
echo "================================================"
echo "  RISULTATI"
echo "================================================"
echo $RESPONSE | jq '.' 2>/dev/null || echo $RESPONSE
echo ""

# Estrai statistiche
TOTAL_FOUND=$(echo $RESPONSE | grep -o '"totalFound":[0-9]*' | cut -d':' -f2)
TOTAL_INSERTED=$(echo $RESPONSE | grep -o '"totalInserted":[0-9]*' | cut -d':' -f2)
API_CALLS=$(echo $RESPONSE | grep -o '"apiRequestsUsed":[0-9]*' | cut -d':' -f2)

echo "================================================"
echo "  SUMMARY"
echo "================================================"
echo "Negozi trovati: $TOTAL_FOUND"
echo "Negozi inseriti: $TOTAL_INSERTED"
echo "API calls usate: $API_CALLS"
echo "================================================"
echo ""

# Step 4: Verifica negozi inseriti
echo -e "${YELLOW}Step 4: Verifying inserted shops...${NC}"

SHOPS_COUNT=$(curl -s -X GET "${API_BASE_URL}/api/admin/shops" \
  -H "Authorization: Bearer ${JWT_TOKEN}" | jq '. | length' 2>/dev/null)

echo "Totale negozi nel database: $SHOPS_COUNT"
echo ""

echo -e "${GREEN}✓ Deployment completato!${NC}"
echo ""
echo "PROSSIMI PASSI:"
echo "1. Verifica i negozi inseriti nella dashboard admin"
echo "2. Attiva manualmente i negozi verificati"
echo "3. Considera di arricchire i dati con Place Details in un secondo momento"
echo ""
