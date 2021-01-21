package curso.common.model.events;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ValidateOrderResult {
    private UUID orderId;
    private Boolean isValid;
}
