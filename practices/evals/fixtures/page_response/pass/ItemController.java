package fixtures.page_response.pass;

// FIXTURE: pass
// PATTERN: list endpoint returns PageResponse<T> with Pageable — PASSES PRACTICES-API-001

import com.example.app.dto.PageResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequestMapping(value = "/api/items", produces = "application/json")
public class ItemController extends com.example.app.controllers.BaseController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // CORRECT: returns PageResponse<ItemResponse> with Pageable param + default size clamp
    @GetMapping
    public ResponseEntity<PageResponse<ItemResponse>> list(
            @PageableDefault(size = 20, max = 100) Pageable pageable) {
        return ResponseEntity.ok(
                PageResponse.from(
                        itemRepository.findAllActive(pageable)
                                .map(i -> new ItemResponse(i.getId(), i.getName()))));
    }

    record ItemResponse(Long id, String name) {}
}
