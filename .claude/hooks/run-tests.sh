#!/usr/bin/env bash
# Hook Stop do Claude Code: se o agente mexeu em código ou build, roda `./gradlew test`
# antes de deixá-lo encerrar o turno. Vermelho devolve a saída ao agente (exit 2).
set -uo pipefail

input="$(cat)"
cd "${CLAUDE_PROJECT_DIR:-$(git rev-parse --show-toplevel)}" || exit 0

# Segunda parada seguida após um bloqueio: não bloqueia de novo, para não entrar em laço.
if grep -q '"stop_hook_active"[[:space:]]*:[[:space:]]*true' <<< "$input"; then
  exit 0
fi

# Só roda se houver mudança (staged, unstaged ou nova) em arquivo que afeta o build.
if ! git status --porcelain --untracked-files=all | grep -qE '\.(kt|kts|toml|xml|properties)$'; then
  exit 0
fi

export JAVA_HOME="${JAVA_HOME_17:-$HOME/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home}"

if ! output="$(./gradlew test --quiet 2>&1)"; then
  {
    echo "./gradlew test está vermelho. Corrija antes de encerrar:"
    # O Gradle só aponta o relatório HTML; os nomes e as mensagens estão nos XMLs do JUnit.
    find . -path '*/build/test-results/*' -name '*.xml' -exec grep -l '<failure' {} + 2>/dev/null |
      xargs grep -h -B1 '<failure' 2>/dev/null |
      sed -nE 's/.*<testcase name="([^"]*)" classname="([^"]*)".*/- \2 > \1/p; s/.*<failure message="([^"]*)".*/    \1/p' |
      cut -c1-300
    tail -n 40 <<< "$output"
  } >&2
  exit 2
fi
exit 0
