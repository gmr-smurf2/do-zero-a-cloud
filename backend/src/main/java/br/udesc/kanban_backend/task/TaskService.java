package br.udesc.kanban_backend.task;

import br.udesc.kanban_backend.column.BoardColumn;
import br.udesc.kanban_backend.column.ColumnService;
import br.udesc.kanban_backend.shared.BadRequestException;
import br.udesc.kanban_backend.shared.ResourceNotFoundException;
import br.udesc.kanban_backend.task.dto.CreateTaskRequest;
import br.udesc.kanban_backend.task.dto.TaskResponse;
import br.udesc.kanban_backend.task.dto.UpdateTaskRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final ColumnService columnService;
    private final Clock clock;

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

    private List<String> normalizeTags(List<String> tags) {
        if (tags == null) {
            return List.of();
        }

        Set<String> uniqueTags = new LinkedHashSet<>();
        List<String> normalizedTags = new ArrayList<>();
        for (String tag : tags) {
            String normalized = tag.trim();
            if (!uniqueTags.add(normalized)) {
                throw new BadRequestException("Tags duplicadas não são permitidas: %s".formatted(normalized));
            }
            normalizedTags.add(normalized);
        }
        return normalizedTags;
    }

    private void validateDueDate(Instant createdAt, Instant dueDate) {
        if (dueDate != null && dueDate.isBefore(createdAt)) {
            throw new BadRequestException("dueDate não pode ser anterior a createdAt");
        }
    }

    private BoardColumn findColumn(UUID columnId) {
        return columnService.findColumn(columnId);
    }

    private KanbanTask findTask(UUID taskId) {
        return taskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Tarefa %s não encontrada".formatted(taskId)
                ));
    }

    private static TaskResponse toResponse(KanbanTask task) {
        return new TaskResponse(
                task.getId(),
                task.getName(),
                task.getPosition(),
                task.getCreatedAt(),
                task.getDueDate(),
                task.isCompleted(),
                task.getTags(),
                task.getColumn().getId()
        );
    }
}
