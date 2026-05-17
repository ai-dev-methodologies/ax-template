package fixtures.page_response.fail_raw_list;

// FIXTURE: fail_raw_list
// EXPECTED_VIOLATION: UNBOUNDED_LIST_RESPONSE_WITHOUT_PAGINATION
// RULE: api-pagination-pageable.md (PRACTICES-API-001)
//
// This controller violates the rule: it returns an unbounded List<ItemResponse>
// instead of a Page<ItemResponse> / PageResponse<ItemResponse>. On a production
// table with millions of rows this exhausts heap and times out the connection pool.
// A guard asserting list endpoints return Page<T> or PageResponse<T> will FAIL
// against this fixture.

import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/items", produces = "application/json")
public class ItemController {

    private final ItemRepository itemRepository;

    public ItemController(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    // VIOLATION: returns raw List — no Pageable parameter, no size clamping
    @GetMapping
    public List<ItemResponse> listAll() {
        return itemRepository.findAll().stream()
                .map(i -> new ItemResponse(i.getId(), i.getName()))
                .toList();
    }

    record ItemResponse(Long id, String name) {}
}
