#!/bin/bash

# Script per verificare la configurazione Firebase
echo "🔍 Verifica Configurazione Firebase"
echo "===================================="
echo ""

# Controlla se il file esiste
if [ -f "/root/TCGArena-BE/firebase-service-account.json" ]; then
    echo "✅ File trovato: /root/TCGArena-BE/firebase-service-account.json"
    echo ""
    
    # Estrai informazioni (senza mostrare chiavi private)
    echo "📋 Informazioni Service Account:"
    cat /root/TCGArena-BE/firebase-service-account.json | jq -r '
        "Project ID: " + .project_id,
        "Client Email: " + .client_email,
        "Type: " + .type,
        "Auth URI: " + .auth_uri
    ' 2>/dev/null || echo "⚠️  Impossibile parsare JSON (potrebbe essere malformato)"
    
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
