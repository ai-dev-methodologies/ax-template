package fixtures.base_service.pass;

// FIXTURE: pass
// PATTERN: service extends BaseService with constructor injection — PASSES PRACTICES-CORE-001

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ItemService extends com.example.app.services.BaseService {

    // CORRECT: private final field + constructor injection
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Transactional
    public void createItem(String name) {
        var item = itemRepository.save(new Item(name));
        auditEvent("item.created", item.getId().toString());
    }
}
