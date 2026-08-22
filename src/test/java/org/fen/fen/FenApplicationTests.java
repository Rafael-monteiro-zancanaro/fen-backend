package org.fen.fen;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "fen.security.jwt.secret=0123456789abcdef0123456789abcdef",
        "fen.security.jwt.expiration=8h"
})
class FenApplicationTests {

    @Test
    void contextLoads() {
    }

}
