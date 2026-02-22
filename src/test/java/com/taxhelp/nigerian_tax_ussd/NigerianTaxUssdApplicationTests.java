package com.taxhelp.nigerian_tax_ussd;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration"
})
class NigerianTaxUssdApplicationTests {

    @MockBean
    private RedisTemplate<String, Object> redisTemplate;

	@Test
	void contextLoads() {
	}

}
