# BUILD LOG — Mobile To-Do App (Kotlin Multiplatform)

Log de desenvolvimento contínuo, conforme exigido pelo PROMPT.md.
Entradas são **append-only**: correções viram novas entradas, nunca reescritas.

---

## Entrada 01 — 2026-09-24

### Prompt / Request

"Comece a implementação e use Kotlin Multiplatform."
Posteriormente: "Vamos padronizar uma coisa, este diretório onde estamos não será a raiz
do projeto e sim um diretório pai. Depois desse projeto finalizado iremos refazê-lo num
outro diretório filho na linguagem de programação flutter."

### Decision Summary

- **Framework:** Kotlin Multiplatform com **Compose Multiplatform** (UI compartilhada entre
  targets), alvo principal **Android** (iOS não é viável neste ambiente Linux sem Xcode).
- **Persistência:** **SQLDelight** 2.x — gera código Kotlin type-safe a partir de arquivos
  `.sql`; suporte nativo a multiplataforma com driver Android e JVM.
- **Notificações:** `AlarmManager` + `BroadcastReceiver` no módulo Android, com
  `NotificationManager` e canais; permissões checadas em runtime (API 33+: `POST_NOTIFICATIONS`).
- **Arquitetura:** MVVM simples — camada de dados (SQLDelight + repositórios), ViewModels
  com coroutines (`StateFlow`), telas Compose observando estado.
- **Navegação:** navegação manual por estado (lista ↔ editor ↔ categorias) — evita
  dependência de biblioteca de navegação, mantendo o projeto mínimo e auditável.
- **Alternativas consideradas:** Room (Android-only, descartado por não ser multiplatform),
  Koin para DI (descartado — escopo pequeno, factory manual é suficiente).
- Assumido: ambiente sem iOS; validação em emulador Android (todo_emulator, API 34).

### Actions Performed

- Criado diretório do projeto `todo-kmp/` (filho de Desenvolvimento/, como combinado —
  Desenvolvimento/ é diretório pai; haverá uma segunda implementação em Flutter).
- Criado `BUILD_LOG.md` (este arquivo) no início do projeto.
- Ferramentas instaladas no ambiente: Android SDK (cmdline-tools 11.0, platform-tools,
  platforms;android-34, build-tools 34.0.0, emulator + system image google_apis x86_64),
  AVD `todo_emulator` (Pixel 6). JDK 21 já presente.

### Result

- Estrutura inicial em progresso.

### Problems / Errors

- Durante a instalação das ferramentas, as execuções de comandos pela sessão do agente
  foram repetidamente interrompidas (provavelmente negações de permissão em comandos fora
  da raiz do projeto). Contornado com o usuário executando comandos via `!` e depois
  liberando execução dentro da raiz.
- `~/.bashrc` recebeu entrada quebrada de PATH ao colar comando multilinha; corrigido
  manualmente pelo usuário via vim.
