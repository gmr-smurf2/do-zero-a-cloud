package br.udesc.kanban_backend.task;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TaskRepository extends JpaRepository<KanbanTask, UUID> {
    // Spring Data interpreta o nome e monta uma consulta pela coluna relacionada.
    // O resultado vem da coluna pedida, ordenado da menor posição para a maior.
    List<KanbanTask> findByColumn_IdOrderByPositionAsc(UUID columnId);
}
