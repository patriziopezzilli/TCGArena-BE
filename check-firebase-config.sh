#!/bin/bash

# Script per verificare la configurazione Firebase
echo "🔍 Verifica Configurazione Firebase"
echo "===================================="
echo ""

# Controlla se il file esiste
if [ -f "/root/TCGArena-BE/firebase-service-account.json" ]; then
    echo "✅ File trovato: /root/TCGArena-BE/firebase-service-account.json"
    echo ""
    
    # Verifica che sia un JSON valido
    echo "📋 Validazione JSON:"
    if python3 -c "import json; json.load(open('/root/TCGArena-BE/firebase-service-account.json'))" 2>/dev/null; then
        echo "✅ JSON valido"
    else
        echo "❌ JSON NON valido o malformato!"
        echo ""
        echo "Prime 5 righe del file:"
        head -5 /root/TCGArena-BE/firebase-service-account.json
        exit 1
    fi
    
    echo ""
    echo "📋 Informazioni Service Account:"
    python3 << 'EOF'
import json
with open('/root/TCGArena-BE/firebase-service-account.json') as f:
    data = json.load(f)
    print(f"Project ID: {data.get('project_id', 'N/A')}")
    print(f"Client Email: {data.get('client_email', 'N/A')}")
    print(f"Type: {data.get('type', 'N/A')}")
    print(f"Private Key ID: {data.get('private_key_id', 'N/A')[:20]}...")
EOF
    
    echo ""
    echo "📅 Data modifica file:"
    ls -lh /root/TCGArena-BE/firebase-service-account.json
    
else
    echo "❌ File NON trovato: /root/TCGArena-BE/firebase-service-account.json"
    echo ""
    echo "📍 File trovati nella directory:"
    ls -la /root/TCGArena-BE/ | grep -i firebase || echo "Nessun file Firebase trovato"
fi

echo ""
echo "=================================="
echo "🔧 Come Risolvere:"
echo "=================================="
echo "1. Vai su: https://console.firebase.google.com/"
echo "2. Seleziona progetto TCG Arena"
echo "3. Settings ⚙️ → Service accounts"
echo "4. Clicca 'Generate new private key'"
echo "5. Scarica il file"
echo "6. Caricalo su: /root/TCGArena-BE/firebase-service-account.json"
echo "7. Riavvia l'applicazione"
echo ""
