package org.fen.fen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "fen.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "fen.security.jwt.expiration=8h",
        "spring.datasource.url=jdbc:h2:mem:application-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1"
})
@ActiveProfiles("test")
class FenApplicationTests {

    @Test
    void contextLoads() {
    }

}
