# TO DO LIST - KMP

Aplicativo mobile de lista de tarefas (to-do) com persistência em SQLite, categorias,
filtros e notificações locais agendadas. Implementado em **Kotlin Multiplatform** com
**Compose Multiplatform**, conforme a especificação em `../PROMPT.md`.

> Diário de desenvolvimento: veja [BUILD_LOG.md](BUILD_LOG.md) (append-only, parte da avaliação).

---

# Mobile App Reverse-Engineering Questions

Respostas baseadas no código real de `todo-kmp/`.

---

## 1. Project Structure

O projeto é multi-módulo Gradle com dois módulos:

- **`shared/`** — tudo que é comum entre plataformas (Kotlin Multiplatform):
  - **Modelos:** `TodoTask.kt` (data class `TodoTask`) e `Category.kt` (data class `Category`) em `shared/src/commonMain/kotlin/com/example/todo/shared/`;
  - **SQLite:** `shared/src/commonMain/sqldelight/com/example/todo/shared/db/Todo.sq` (schema + queries SQL) — o SQLDelight gera o código Kotlin (`TodoDatabase`, `Task`, `Category`) em tempo de compilação;
  - **Repositório:** `TodoRepository.kt` — todo o CRUD;
  - **ViewModels:** `TodoViewModel.kt`, `TaskEditorViewModel.kt`, `CategoriesViewModel.kt`;
  - **Notificações (abstração):** `TodoNotifier.kt` (interface) e `AppDependencies.kt` (`expect fun createDependencies()`).
- **`composeApp/`** — app Compose Multiplatform:
  - **Telas/UI:** `composeApp/src/commonMain/kotlin/com/example/todo/app/ui/` — `App.kt` (navegação), `TaskListScreen.kt`, `TaskEditorScreen.kt`, `CategoriesScreen.kt`, `AppGraph.kt`, `UiUtils.kt`;
  - **Entry point Android:** `androidMain/.../MainActivity.kt`; desktop: `jvmMain/.../main.kt`;
  - **Notificações (implementação Android):** `shared/src/androidMain/.../AndroidTodoNotifier.kt` (AlarmManager + BroadcastReceiver) e o receiver registrado no `AndroidManifest.xml`.

Organização: camada de dados (SQLDelight) → repositório → ViewModels → telas Compose; os pontos específicos de plataforma (driver do banco, notificações, permissões) ficam em `androidMain`/`jvmMain` via `expect/actual`.

---

## 2. Architecture and State

**MVVM** com **StateFlow** (Kotlin Coroutines) e UI reativa Compose.

- Cada ViewModel (`TodoViewModel`, `TaskEditorViewModel`, `CategoriesViewModel`) expõe `StateFlow`s (ex.: `tasks`, `categories`, `filter` em `TodoViewModel.kt`).
- As telas Compose coletam os fluxos com `collectAsState()` — qualquer mudança no banco recompõe a UI automaticamente.
- **Criar/editar tarefa:** `TaskEditorViewModel.save()` grava no repositório; como a lista vem de um fluxo do próprio banco (`asFlow()` do SQLDelight, que emite a cada mudança na tabela), a lista se atualiza sozinha.
- **Marcar como concluída:** `TodoViewModel.toggleCompleted(task)` chama `repository.setCompleted()`; o fluxo reemite e a UI risca o item.
- Filtros ficam em `MutableStateFlow<TaskFilter>` e são combinados com os fluxos do banco via `combine()`.

Padrão reconhecível: **MVVM + unidirecional (dados → estado → UI)**, sem bibliotecas de estado externas.

---

## 3. SQLite Persistence

- **Criação do banco:** `createDependencies()` — versão Android em `shared/src/androidMain/.../Dependencies.android.kt` usa `AndroidSqliteDriver(TodoDatabase.Schema, context, "todo.db")` (arquivo `todo.db` no armazenamento interno do app); versão desktop em `Dependencies.jvm.kt` usa `JdbcSqliteDriver` em `~/.todo-kmp/todo.db`.
- **Armazenamento:** tabelas `Task` e `Category` definidas em `Todo.sq`, com FK `categoryId → Category(id) ON DELETE SET NULL` e 3 categorias iniciais (Pessoal, Trabalho, Estudos).
- **CRUD:** todo em `TodoRepository.kt` — `insertTask`, `updateTask`, `setCompleted`, `deleteTask`, `taskById`, `tasks()` (leitura reativa), e para categorias `insertCategory`, `updateCategory`, `deleteCategory`, `countTasksInCategory`. As queries SQL nomeadas (`selectAllTasks:`, `insertTask:`, etc.) estão em `Todo.sq`.

---

## 4. Follow One Operation

Rastreando a criação de uma nova tarefa:

1. Usuário toca **+** → `App.kt` navega para `Screen.TaskEditor(null)` → `TaskEditorScreen` → `TaskEditorViewModel.load(null)`;
2. Usuário digita título/descrição, escolhe categoria/vencimento → toca **Salvar** → `TaskEditorViewModel.save()`:
   - valida título (se vazio, marca `titleError` e não salva);
   - chama `repository.insertTask(...)` → `Todo.sq:insertTask` → **INSERT na tabela Task do SQLite**;
   - se `dueDateTime != null` e notificações disponíveis (`dep.notifier.isAvailable`): chama `dep.notifier.schedule(newId, título, descrição, due)` → `AndroidTodoNotifier.schedule()` registra um **alarme exato** (`setExactAndAllowWhileIdle`) com `PendingIntent` cujo `requestCode` é o **ID da tarefa**;
3. O INSERT dispara reemissão do fluxo `selectAllTasks().asFlow()` → `TodoViewModel.tasks` (combinado com filtros) → `collectAsState()` na `TaskListScreen` → **a tarefa aparece na lista automaticamente** ao voltar.

---

## 5. Navigation

- **Por estado manual**, sem biblioteca de navegação: `App.kt` mantém `var currentScreen by remember { mutableStateOf<Screen>(...) }` com a sealed interface `Screen { TaskList; TaskEditor(taskId?); Categories }`.
- **Lista → editor:** `onEditTask(taskId)` (null = nova tarefa).
- **Dados passados:** apenas o **task ID** (`Long?`). O editor busca o objeto completo do banco via `TaskEditorViewModel.load(taskId)` → `repository.taskById(id)`. Categorias são carregadas à parte pelo próprio editor.

---

## 6. Notifications

Implementadas em `AndroidTodoNotifier.kt` (AlarmManager + `TodoAlarmReceiver` + canal "Lembretes de tarefas"):

- **Agendamento:** `schedule(taskId, title, body, dueMillis)` cria `PendingIntent.getBroadcast` com `requestCode = taskId.toInt()` (associa notificação ↔ tarefa) e registra `setExactAndAllowWhileIdle(RTC_WAKEUP, dueMillis, ...)`. O receiver `TodoAlarmReceiver.onReceive` monta a notificação (`NotificationCompat`) e publica com `nm.notify(taskId.toInt(), ...)`.
- **Mudança de vencimento:** `TaskEditorViewModel.save()` reagenda (`schedule` com `FLAG_UPDATE_CURRENT` sobrescreve o alarme anterior) se a data for futura, ou cancela se removida/passada.
- **Concluída:** `TodoViewModel.toggleCompleted` chama `notifier.cancel(taskId)` (e reagenda se reaberta).
- **Excluída:** `deleteTask` (no ViewModel e em `AppGraph.taskDeleter`) cancela a notificação antes de remover.
- **Permissão:** API 33+ exige `POST_NOTIFICATIONS` — `MainActivity.requestNotificationPermissionIfNeeded()` solicita em runtime; `isAvailable` checa antes de agendar; o receiver re-checa antes de publicar (nunca crasha se negada).

---

## 7. Agent Decisions

Decisões importantes feitas pelo agente que **não** foram especificadas no assignment:

1. **Kotlin Multiplatform + Compose Multiplatform** (UI compartilhada) com alvo Android + desktop JVM — o assignment só pedia "app mobile"; arquitetura, linguagem e estrutura eram livres. iOS foi descartado (ambiente Linux sem Xcode).
2. **SQLDelight** como abstração SQLite (código type-safe gerado de `.sql`) — alternativa considerada: Room (descartado por ser Android-only).
3. **Navegação manual por estado** (sealed class `Screen`) em vez de biblioteca (Jetpack Navigation / Voyager) — escopo pequeno, sem dependências extras.
4. **DI manual** via `AppDependencies`/`AppGraph` em vez de Koin/Hilt.
5. **MVVM com StateFlow** e fluxos reativos direto do banco (SQLDelight `asFlow`) — UI sempre sincronizada sem recarga manual.
6. **DatePicker/TimePicker próprios** (steppers em `TaskEditorScreen.kt`) — o Compose Multiplatform 1.6 não tem date/time picker multiplataforma nativos.

---

## 8. BUILD_LOG Analysis

Exemplos de `BUILD_LOG.md` (Entrada 01 + correções subsequentes):

- **Problema:** o build falhou em sequência — caminho errado no wrapper (`APP_HOME` apontava para o diretório pai), repositórios de plugins ausentes no `settings.gradle.kts`, `android.useAndroidX=true` faltante, e vários erros de compilação Kotlin (imports, `expect fun` vs `expect class`).
- **Como tentou resolver:** correções incrementais a cada build, sempre registrando a iteração.
- **Primeira solução funcionou?** Não — cada correção revelava o erro seguinte (diagnóstico em `/tmp/build*.log`).
- **O que foi feito no fim:** `BUILD SUCCESSFUL in 28s` após ~9 iterações; APK instalado no emulador (`todo_emulator`) com `adb install`.

**O que o build log ajudou a entender:** o **processo** — que o caminho até o build funcional foi iterativo e com múltiplos erros em cascata (configuração → codegen SQLDelight → imports), que decisões (SQLDelight, MVVM, navegação manual) foram tomadas com justificativa no início, e que houve obstáculos de ambiente (execuções interrompidas, `.bashrc` quebrado) contornados com o usuário. Olhando só o código final, nada disso é visível — o código final parece "ter nascido pronto", quando na verdade passou por ~9 builds e várias correções registradas.

---

## Como compilar e rodar

```bash
# Android (APK debug)
export ANDROID_HOME=$HOME/Android/Sdk
./gradlew :composeApp:assembleDebug
adb install -r composeApp/build/outputs/apk/debug/composeApp-debug.apk
adb shell am start -n com.example.todo.app/.MainActivity

# Desktop (JVM) — útil para testar a UI sem emulador
./gradlew :composeApp:run
```

## Limitações conhecidas

- iOS não é suportado neste ambiente (Linux sem Xcode), embora a estrutura KMP o permita.
- Notificações exigem a janela do emulador aberta (`-no-window` não as exibe visualmente).
- Seletor de data/hora implementado com steppers simples (o Compose Multiplatform 1.6 não tem pickers nativos multiplataforma).
