package fixtures.base_service.fail_field_injection;

// FIXTURE: fail_field_injection
// EXPECTED_VIOLATION: FIELD_INJECTION_INSTEAD_OF_CONSTRUCTOR
// RULE: core-constructor-injection.md (PRACTICES-CORE-001)
//
// This service violates the rule: it uses @Autowired field injection instead of
// constructor injection with final fields. Field injection hides dependencies,
// prevents immutability, and makes plain-JUnit tests impossible without a
// Spring container.
// An archunit rule asserting final fields + no @Autowired fields will FAIL
// against this fixture.

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ItemService {

    // VIOLATION: @Autowired field injection — not final, hidden dependency
    @Autowired
    private ItemRepository itemRepository;

    public void doWork() {
        itemRepository.findAll();
    }
}
