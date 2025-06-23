package digital.pragmatech.model.common;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Data;

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
