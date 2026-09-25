package br.udesc.kanban_backend.task;

import br.udesc.kanban_backend.column.BoardColumn;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "kanban_tasks")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KanbanTask {

    @Getter
    @Id
    private UUID id;

    @Getter
    @Column(nullable = false, length = 120)
    private String name;
    
    @Getter
    @PositiveOrZero
    @Column(name = "position", nullable = false)
    private int position;

    @Getter
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Getter
    @Column(name = "due_date")
    private Instant dueDate;

    @Getter
    @Column(nullable = false)
    private boolean completed;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "task_tags", joinColumns = @JoinColumn(name = "task_id"))
    @OrderColumn(name = "tag_order")
    @Column(name = "tag", nullable = false, length = 40)
    private List<String> tags = new ArrayList<>();

    @Getter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "column_id", nullable = false)
    private BoardColumn column;

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
   
}
