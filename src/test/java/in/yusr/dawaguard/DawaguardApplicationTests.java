package in.yusr.dawaguard;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "dawaguard.cdsco.sync-on-startup=false",
        "spring.datasource.url=jdbc:h2:mem:test",
        // Wires the webhook controller too; no messages are sent in tests.
        "dawaguard.whatsapp.enabled=true"
})
class DawaguardApplicationTests {

    @Test
    void contextLoads() {
    }
}
