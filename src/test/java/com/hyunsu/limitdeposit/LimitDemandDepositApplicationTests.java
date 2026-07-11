package com.hyunsu.limitdeposit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// [Claude] dev 프로파일(localhost:5432)이 아닌 Testcontainers로 컨텍스트를 띄워야
// [Claude] 로컬 Postgres 미기동 상태에서도 전체 스위트가 깨지지 않는다
@SpringBootTest
@ActiveProfiles("test")
class LimitDemandDepositApplicationTests {

	@Test
	void contextLoads() {
	}

}
