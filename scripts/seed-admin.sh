#!/usr/bin/env bash
set -e

# ==============================================================================
# Script Utilitário: Verificar e Configurar Usuário Administrador
# ==============================================================================

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ENV_FILE="$ROOT_DIR/.env"

if [ -f "$ENV_FILE" ]; then
    echo "Carregando variáveis do arquivo $ENV_FILE..."
    export $(grep -v '^#' "$ENV_FILE" | xargs)
else
    echo "Aviso: Arquivo .env não encontrado em $ENV_FILE"
fi

echo "--- Configuração do Administrador ---"
echo "ADMIN_EMAIL: ${ADMIN_EMAIL:-Não definido}"
echo "ADMIN_FIRST_NAME: ${ADMIN_FIRST_NAME:-Admin}"
echo "ADMIN_LAST_NAME: ${ADMIN_LAST_NAME:-Sistema}"
echo "ADMIN_PASSWORD: ${ADMIN_PASSWORD:+'[CONFIGURADO]'}"

if [ -z "$ADMIN_EMAIL" ] || [ -z "$ADMIN_PASSWORD" ]; then
    echo "ERRO: ADMIN_EMAIL e ADMIN_PASSWORD devem estar definidos no .env!"
    exit 1
fi

echo "Configuração validada com sucesso! O seeder criará o usuário admin automaticamente ao iniciar a aplicação."
