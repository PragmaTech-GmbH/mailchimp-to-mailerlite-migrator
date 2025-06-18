package digital.pragmatech.model.common;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class Category {
    private String id;
    private String name;
    private String parentId;
    private String displayOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}