package br.udesc.kanban_backend.column;

import br.udesc.kanban_backend.board.Board;
import br.udesc.kanban_backend.board.BoardService;
import br.udesc.kanban_backend.column.dto.ColumnRequest;
import br.udesc.kanban_backend.column.dto.ColumnResponse;
import br.udesc.kanban_backend.shared.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ColumnService {

    private final ColumnRepository columnRepository;
    private final BoardService boardService;

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

    @Transactional
    public ColumnResponse update(UUID columnId, ColumnRequest request) {
        BoardColumn column = findColumn(columnId);
        Board board = findBoard(request.boardId());
        column.update(request.name().trim(), request.position(), board);
        return toResponse(column);
    }

    @Transactional
    public void delete(UUID columnId) {
        BoardColumn column = findColumn(columnId);
        columnRepository.delete(column);
    }

    private Board findBoard(UUID boardId) {
        return boardService.findBoard(boardId);
    }

    @Transactional(readOnly = true)
    public BoardColumn findColumn(UUID columnId) {
        return columnRepository.findById(columnId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Coluna %s não encontrada".formatted(columnId)
                ));
    }

    private static ColumnResponse toResponse(BoardColumn column) {
        return new ColumnResponse(
                column.getId(),
                column.getName(),
                column.getPosition(),
                column.getBoard().getId()
        );
    }
}
