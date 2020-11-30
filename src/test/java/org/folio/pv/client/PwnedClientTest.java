package org.folio.pv.client;

import org.folio.pv.domain.dto.Password;
import org.folio.pv.service.PwnedPasswordsService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
//@ContextConfiguration(classes = {DbConfig.class})
@TestPropertySource("classpath:application-test.properties")
@ActiveProfiles("TestDB")
class PwnedClientTest {

  @Autowired
  private PwnedPasswordsService pwnedService;
  @Autowired
  private UserClient userClient;

  @Test
  void testPwndFeignClient() {
    Password pc = new Password();
    pc.setPassword("P@ssw0rd");

    boolean result = pwnedService.validate(pc);

    Assertions.assertFalse(result);
  }

  @Test
  void testUserClientNotNull() {
    Assertions.assertNotNull(userClient);
  }

  @Test
  void testUserFeignClient() {
    String userId = "invalidUUID";

    var response = userClient.getUserByQuery("id==" + userId);

    Assertions.assertNotNull(response);
  }
}