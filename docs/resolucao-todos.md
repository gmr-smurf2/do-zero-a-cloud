# Resolução dos TODOs

Este material mostra uma solução possível para os exercícios do backend. Copie o conteúdo indicado para o arquivo correspondente. Os comentários explicam o caminho da requisição:

```text
Controller -> Service -> Repository -> PostgreSQL
```

Os exemplos usam laços e condições simples. Os DTOs, repositórios auxiliares e classes de domínio já existentes continuam sendo usados.

## Checkpoint 1 — listar quadros

### `backend/src/main/java/br/udesc/kanban_backend/board/BoardController.java`

Troque somente o método `list()`:

```java
@GetMapping
public List<BoardResponse> list() {
    // O controller recebe a chamada HTTP e delega a consulta ao service.
    return boardService.list();
}
```

### `backend/src/main/java/br/udesc/kanban_backend/board/BoardService.java`

Troque o método `list()` para percorrer as entidades e montar a lista de respostas. Este método também evita streams, que aparecem na versão inicial do projeto.

```java
@Transactional(readOnly = true)
public List<BoardResponse> list() {
    // Busca os quadros já ordenados alfabeticamente pelo repository.
    List<Board> boards = boardRepository.findAllByOrderByNameAsc();
    List<BoardResponse> responses = new ArrayList<>();

    // Converte uma entidade por vez para o formato público da API.
    for (Board board : boards) {
        BoardResponse response = toResponse(board);
        responses.add(response);
    }

    return responses;
}
```

Adicione este import junto aos demais imports:

```java
import java.util.ArrayList;
```

`BoardRepository` já possui `findAllByOrderByNameAsc()`. Não é necessário alterá-lo.

## Checkpoint 2 — listar e criar colunas

### `backend/src/main/java/br/udesc/kanban_backend/column/ColumnController.java`

Troque os dois métodos com TODO:

```java
@GetMapping("/from/{boardId}")
public List<ColumnResponse> listByBoard(@PathVariable UUID boardId) {
    // O ID vem da URL; a regra de consulta fica no service.
    return columnService.listByBoard(boardId);
}

@PostMapping
public ColumnResponse create(@Valid @RequestBody ColumnRequest request) {
    // @Valid verifica as regras do DTO antes da chamada ao service.
    return columnService.create(request);
}
```

### `backend/src/main/java/br/udesc/kanban_backend/column/ColumnService.java`

Troque os métodos `listByBoard` e `create`:

```java
@Transactional(readOnly = true)
public List<ColumnResponse> listByBoard(UUID boardId) {
    // Confirma que o quadro existe. Se não existir, boardService lança 404.
    boardService.findBoard(boardId);

    // O repository já devolve as colunas ordenadas pela posição.
    List<BoardColumn> columns = columnRepository
            .findByBoard_IdOrderByPositionAsc(boardId);
    List<ColumnResponse> responses = new ArrayList<>();

    // Transforma cada entidade em um objeto que pode ser enviado como JSON.
    for (BoardColumn column : columns) {
        ColumnResponse response = toResponse(column);
        responses.add(response);
    }

    return responses;
}

@Transactional
public ColumnResponse create(ColumnRequest request) {
    // Procura o quadro que receberá a nova coluna.
    Board board = findBoard(request.boardId());

    // Remove espaços acidentais antes e depois do nome.
    String name = request.name().trim();

    // Cria a entidade e a salva no banco.
    BoardColumn column = new BoardColumn(name, request.position(), board);
    BoardColumn savedColumn = columnRepository.save(column);

    // Retorna os dados da coluna criada.
    return toResponse(savedColumn);
}
```

Adicione este import no início de `ColumnService.java`:

```java
import java.util.ArrayList;
```

`List` e `UUID` já são importados. `ColumnRepository` já contém a consulta ordenada usada acima.

## Checkpoint 3 — tarefas

### 3a. Mapeamento da entidade

Em `backend/src/main/java/br/udesc/kanban_backend/task/KanbanTask.java`, substitua as anotações TODO:

Adicione este import junto aos imports da classe:

```java
import jakarta.validation.constraints.PositiveOrZero;
```

Depois, substitua as anotações dos campos:

```java
@Getter
@Column(nullable = false, length = 120)
private String name;

@Getter
@PositiveOrZero
@Column(name = "position", nullable = false)
private int position;
```

O nome da coluna física segue literalmente o TODO (`position`). `@PositiveOrZero` também documenta a regra na entidade; o DTO já verifica que uma posição enviada pela API é zero ou maior.

### 3b. Repository

Em `backend/src/main/java/br/udesc/kanban_backend/task/TaskRepository.java`, substitua o comentário TODO por:

```java
// Spring Data interpreta o nome e monta uma consulta pela coluna relacionada.
// O resultado vem da coluna pedida, ordenado da menor posição para a maior.
List<KanbanTask> findByColumn_IdOrderByPositionAsc(UUID columnId);
```

### 3c. Construir e atualizar uma tarefa

Em `KanbanTask.java`, substitua os corpos dos métodos construtor e `update`. Mantenha o método `getTags()` entre eles: o `TaskService` depende dele para montar a resposta. O trecho completo dessa parte da classe fica assim:

```java
public KanbanTask(
        String name,
        int position,
        Instant createdAt,
        Instant dueDate,
        boolean completed,
        List<String> tags,
        BoardColumn column
) {
    // Cada tarefa recebe um identificador novo.
    this.id = UUID.randomUUID();
    this.name = name;
    this.position = position;
    this.createdAt = createdAt;
    this.dueDate = dueDate;
    this.completed = completed;

    // Copia a lista para que alterações feitas pelo chamador não mudem a tarefa.
    this.tags = new ArrayList<>(tags);
    this.column = column;
}

public List<String> getTags() {
    return List.copyOf(tags);
}

public void update(
        String name,
        int position,
        Instant dueDate,
        boolean completed,
        List<String> tags,
        BoardColumn column
) {
    // Atualiza os campos que podem mudar.
    this.name = name;
    this.position = position;
    this.dueDate = dueDate;
    this.completed = completed;

    // Mantém a lista interna e substitui o conteúdo das tags.
    this.tags.clear();
    this.tags.addAll(tags);

    this.column = column;

    // createdAt não é alterado: representa quando a tarefa foi criada.
}
```

### 3d. Controller de tarefas

Em `backend/src/main/java/br/udesc/kanban_backend/task/TaskController.java`, substitua os quatro métodos marcados com TODO (listagem, criação, atualização e exclusão):

```java
@GetMapping("/from/{columnId}")
public List<TaskResponse> listByColumn(@PathVariable UUID columnId) {
    return taskService.listByColumn(columnId);
}

@PostMapping("/from/{columnId}")
public TaskResponse create(
        @PathVariable UUID columnId,
        @Valid @RequestBody CreateTaskRequest request
) {
    return taskService.create(columnId, request);
}

@PutMapping("/{taskId}")
public TaskResponse update(
        @PathVariable UUID taskId,
        @Valid @RequestBody UpdateTaskRequest request
) {
    return taskService.update(taskId, request);
}

@DeleteMapping("/{taskId}")
public ResponseEntity<StatusResponse> delete(@PathVariable UUID taskId) {
    taskService.delete(taskId);
    return ResponseEntity.ok(StatusResponse.ok());
}
```

Mantenha os imports que a classe já tem. Os parâmetros `@Valid` fazem o Spring verificar os campos obrigatórios e seus limites antes de entrar no método.

### 3e. Service de tarefas

Em `backend/src/main/java/br/udesc/kanban_backend/task/TaskService.java`, substitua os quatro métodos marcados com TODO:

```java
@Transactional(readOnly = true)
public List<TaskResponse> listByColumn(UUID columnId) {
    // A busca também verifica se a coluna existe.
    findColumn(columnId);

    List<KanbanTask> tasks = taskRepository
            .findByColumn_IdOrderByPositionAsc(columnId);
    List<TaskResponse> responses = new ArrayList<>();

    for (KanbanTask task : tasks) {
        TaskResponse response = toResponse(task);
        responses.add(response);
    }

    return responses;
}

@Transactional
public TaskResponse create(UUID columnId, CreateTaskRequest request) {
    // A URL é a fonte principal do ID. Se o JSON também informar o ID,
    // os dois valores precisam ser iguais.
    if (request.columnId() != null && !columnId.equals(request.columnId())) {
        throw new BadRequestException(
                "O columnId do body deve ser igual ao columnId da URL"
        );
    }

    BoardColumn column = findColumn(columnId);

    // Se o aluno não enviar createdAt, usamos o relógio UTC configurado.
    Instant createdAt = request.createdAt();
    if (createdAt == null) {
        createdAt = clock.instant();
    }

    validateDueDate(createdAt, request.dueDate());
    List<String> tags = normalizeTags(request.tags());

    // Boolean pode ser null: ausência significa false na criação.
    boolean completed = Boolean.TRUE.equals(request.completed());

    KanbanTask task = new KanbanTask(
            request.name().trim(),
            request.position(),
            createdAt,
            request.dueDate(),
            completed,
            tags,
            column
    );

    KanbanTask savedTask = taskRepository.save(task);
    return toResponse(savedTask);
}

@Transactional
public TaskResponse update(UUID taskId, UpdateTaskRequest request) {
    KanbanTask task = findTask(taskId);

    // A data original pode ser omitida ou repetida, mas não trocada.
    if (request.createdAt() != null
            && !request.createdAt().equals(task.getCreatedAt())) {
        throw new BadRequestException("createdAt não pode ser alterado");
    }

    BoardColumn column = findColumn(request.columnId());
    validateDueDate(task.getCreatedAt(), request.dueDate());
    List<String> tags = normalizeTags(request.tags());

    task.update(
            request.name().trim(),
            request.position(),
            request.dueDate(),
            request.completed(),
            tags,
            column
    );

    // A entidade está gerenciada pela transação; o JPA grava as mudanças.
    return toResponse(task);
}

@Transactional
public void delete(UUID taskId) {
    // Verifica existência para devolver 404 caso não exista.
    KanbanTask task = findTask(taskId);
    taskRepository.delete(task);
}
```

O método `delete` completa o TODO extra. `ArrayList`, `List`, `Instant`, `UUID`, `BoardColumn` e as exceções já fazem parte dos imports da classe.

O service já contém os métodos `normalizeTags`, `validateDueDate`, `findColumn`, `findTask` e `toResponse`. Eles tratam espaços e tags repetidas, rejeitam vencimentos anteriores à criação e transformam a entidade em resposta.

## Conferir o resultado

1. Inicie PostgreSQL e o backend conforme o README.
2. No frontend, abra **Configurações → Auditoria da API**.
3. Rode os checkpoints **Board**, **Column** e **Task** em ordem.
4. Se algum passo falhar, confira primeiro se o banco está ligado e depois leia a mensagem do passo que falhou.

O exercício de containers para iniciar os três serviços localmente está em [`exercicio-containers.md`](exercicio-containers.md).
