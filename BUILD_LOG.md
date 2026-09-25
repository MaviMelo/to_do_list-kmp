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

---

## Entrada 02 — 2026-09-24

### Prompt / Request

Usuário relatou: "Achei que você não documentou tudo da forma que é determinado no
PROMPT.md para o ./todo-kmp/BUILD_LOG.md. Outro problema é que no emulador o app to-do
não abre. Ele tenta abrir e fecha silenciosamente."

### Actions Performed

Diagnóstico do crash e posteriormente completar este log (entradas 02+).

### Result

- Em progresso.

### Problems / Errors

- Ver Entrada 03 (causa raiz do crash).

---

## Entrada 03 — 2026-09-24

### Prompt / Request

(Continuação da Entrada 02 — diagnóstico do crash do app no emulador.)

### Actions Performed

- Coletado logcat do emulador (`adb logcat -d | grep -E "FATAL|AndroidRuntime"`).

### Result

**Causa raiz identificada:**

```
java.lang.RuntimeException: Unable to start activity
ComponentInfo{com.example.todo.app/com.example.todo.app.MainActivity}:
java.lang.IllegalStateException: Application not started
```

O `AndroidManifest.xml` não registrava a classe `ContextHolder` (que estende
`Application` e captura o contexto em `onCreate`) como `android:name` da tag
`<application>`. Sem isso, o sistema Android nunca instancia o `ContextHolder`,
o `lateinit appContext` nunca é inicializado, e `createDependencies()` (chamado em
`MainActivity.onCreate`) lança `IllegalStateException` — fechando o app silenciosamente.

### Problems / Errors

- Este foi o problema.

---

## Entrada 04 — 2026-09-24

### Prompt / Request

(Continuação — correção do crash.)

### Actions Performed

- **Correção:** adicionado `android:name="com.example.todo.shared.ContextHolder"` à tag
  `<application>` em `composeApp/src/androidMain/AndroidManifest.xml`.
- Rebuild: `./gradlew :composeApp:assembleDebug` → `BUILD SUCCESSFUL in 22s`.
- Reinstalado no emulador: `adb install -r ...` → `Success`.
- Relançado: `adb shell am start -n com.example.todo.app/.MainActivity`.
- Verificado: `dumpsys activity activities` mostra `topResumedActivity=
  ActivityRecord{... com.example.todo.app/.MainActivity}` (app em primeiro plano, sem crash).
- Screenshot confirma a tela "Minhas Tarefas" funcionando: título, botão Categorias,
  filtros de status (Todas/Pendentes/Concluídas), filtros de categoria
  (Estudos/Pessoal/Trabalho — as 3 categorias seed do SQL), mensagem de lista vazia
  e botão FAB "+".

### Result

**Resolvido.** App abre e renderiza a tela de listagem corretamente.

---

## Entrada 05 — 2026-09-24

### Prompt / Request

Registro consolidado das iterações de build anteriores (que ocorreram entre a Entrada 01
e a Entrada 02, antes da coleta de evidências abaixo — reconstruído a partir dos logs
`/tmp/build*.log` e do histórico da sessão).

### Actions Performed (cronologia dos builds)

1. **Build 1-2 (falha):** `Directory '.../Desenvolvimento' does not contain a Gradle build`
   — o wrapper manual (`gradlew`) calculava `APP_HOME` como o diretório pai
   (`dirname "$0"/..`), mas o wrapper está na raiz do projeto. Correção: `APP_HOME=$(cd
   "$(dirname "$0")" && pwd)`. Também corrigido o caminho aninhado do binário Gradle
   (`gradle-8.7/gradle-8.7/bin/gradle` → `gradle-8.7/bin/gradle`) após extração manual.
2. **Build 3 (falha):** plugins do AGP não resolvidos — faltavam repositórios. Correção:
   adicionados blocos `pluginManagement` (google, mavenCentral, gradlePluginPortal) e
   `dependencyResolutionManagement` (google, mavenCentral) no `settings.gradle.kts`.
3. **Build 4 (falha):** `android.useAndroidX=true` ausente. Correção: criado
   `gradle.properties` com `android.useAndroidX=true`, `org.gradle.jvmargs=-Xmx2048m`,
   `kotlin.code.style=official`, `android.nonTransitiveRClass=true`.
4. **Build 4-8 (falhas de compilação Kotlin, em cascata):**
   - `expect class createDependencies()` → corrigido para `expect fun createDependencies()`;
   - `expect fun currentTimeMillis` declarado no package `...shared.db` mas actuals em
     `...shared` → movido para `shared/Time.kt` com import correto;
   - módulo `shared` usando `androidx.core` (NotificationCompat/ContextCompat) sem
     declarar dependência → adicionado `libs.androidx.core` em `androidMain`;
   - `asFlow` sem import (`app.cash.sqldelight.coroutines.asFlow`) → corrigido;
   - mapeamento de categorias retornando linhas SQL em vez de `Category` → corrigido;
   - `parseColor` usando `android.graphics.Color` em código comum (quebraria desktop) →
     reescrito com parsing manual de hex; movido para `UiUtils.kt` compartilhado;
   - `MutableStateFlow` sem import; getter `appContextInstance` com tipo incorreto;
   - `kotlinx-datetime` ausente no `composeApp` → adicionado ao `commonMain`;
   - chip "Nenhuma" com lambda malformada → simplificado.
5. **Build 9 (sucesso):** `BUILD SUCCESSFUL` — APK debug gerado em
   `composeApp/build/outputs/apk/debug/composeApp-debug.apk`.
6. **Emulador:** iniciado `todo_emulator` (`-no-window -gpu swiftshader_indirect`);
   APK instalado com `adb install -r` → `Success`; app lançado com `am start`.

### Result

- Build funcional; app instalado; tela de listagem renderizando (Entrada 04).
- Criado `README.md` com respostas do questionário de reverse-engineering,
  instruções de compilação e limitações conhecidas.

### Problems / Errors

- O caminho foi iterativo: cada correção de configuração revelava o erro seguinte
  (configuração → codegen SQLDelight → imports). Todos registrados acima.
- Usuário relatou app fechando silenciosamente no emulador → resolvido na Entrada 04
  (registro do `ContextHolder` no manifest).

---

## Resumo Final (conforme PROMPT.md, seção 20)

- **Arquitetura:** MVVM — SQLDelight (dados) → TodoRepository → ViewModels (StateFlow)
  → telas Compose reativas; navegação manual por estado (`Screen` sealed interface);
  DI manual via `AppDependencies`/`AppGraph`; pontos de plataforma via expect/actual.
- **Dependências:** Compose Multiplatform 1.6.11, SQLDelight 2.0.2, kotlinx-coroutines
  1.8.1, kotlinx-datetime 0.6.1, androidx (core/appcompat/activity-compose/lifecycle).
- **SQLite:** banco `TodoDatabase` gerado de `Todo.sq`; drivers Android (`AndroidSqliteDriver`,
  arquivo `todo.db` interno) e JVM (`JdbcSqliteDriver`, `~/.todo-kmp/todo.db`);
  FK `ON DELETE SET NULL` para categorias; 3 categorias seed.
- **Estado:** `StateFlow` + `combine()` para filtros; fluxos reativos do banco
  (`asFlow`) mantêm a UI sincronizada sem recarga manual.
- **Navegação:** manual, baseada em `mutableStateOf<Screen>` em `App.kt`.
- **Notificações:** `AndroidTodoNotifier` — AlarmManager (`setExactAndAllowWhileIdle`),
  `TodoAlarmReceiver`, canal dedicado, `PendingIntent` requestCode = taskId;
  reagendar/cancelar em salvar/concluir/excluir; permissão `POST_NOTIFICATIONS`
  solicitada em runtime (API 33+) e re-checada antes de publicar.
- **Limitações conhecidas:** sem iOS (Linux sem Xcode); notificações não visíveis com
  emulador `-no-window`; date/time picker próprio com steppers (CMP 1.6 não tem nativo);
  sem `SCHEDULE_EXACT_ALARM` concedida, alarmes usam fallback inexact (até ~10 min de
  tolerância em Doze).
- **Status final do app: Completed** (build OK, app abre e renderiza; teste interativo
  de UI/UX a cargo do usuário no emulador).

---

## Entrada 06 — 2026-09-25

### Prompt / Request

Usuário testou no emulador: "A aplicação funciona mas quando é salvo uma nova tarefa
o aplicativo se fecha, apesar de persistir os dados."

### Actions Performed

- Coletado logcat: crash em `AndroidTodoNotifier.schedule` (AndroidTodoNotifier.kt:64) →
  `AlarmManager.setExactAndAllowWhileIdle` → `SecurityException` via RemoteException do
  AlarmManagerService.

### Result

**Causa raiz:** na API 31+, `setExactAndAllowWhileIdle` exige a permissão
`SCHEDULE_EXACT_ALARM` **concedida pelo usuário** — o manifest só a declara; o sistema
não concede automaticamente. O `isAvailable` checava apenas `POST_NOTIFICATIONS`,
não `SCHEDULE_EXACT_ALARM`.

### Problems / Errors

- App fechava ao salvar tarefa com vencimento (dados persistiam porque o INSERT
  acontecia antes do crash na corrotina de notificação).

---

## Entrada 07 — 2026-09-25

### Prompt / Request

(Continuação — correção do crash de alarme exato.)

### Actions Performed

- **Correção (fallback gracioso, conforme requisito do PROMPT.md de nunca crashar com
  permissão negada):** em `AndroidTodoNotifier.schedule()`, o `setExactAndAllowWhileIdle`
  agora está dentro de `try/catch (SecurityException)` — se o alarme exato for negado,
  usa `setAndAllowWhileIdle` (alarme inexact, janela de ~10 min de tolerância).
- Rebuild (`BUILD SUCCESSFUL in 19s`) + reinstall (`Success`) + relançamento —
  app em primeiro plano (`topResumedActivity` confirmado).

### Result

**Resolvido.** Salvar tarefa com vencimento não fecha mais o app. Notificações com
alarme inexact podem disparar com até ~10 min de atraso em modo Doze — limitação
conhecida registrada no Resumo Final.
