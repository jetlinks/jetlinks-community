package org.jetlinks.community.network.manager.debug;

import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.jetlinks.community.gateway.external.SubscribeRequest;
import org.jetlinks.community.test.web.TestAuthentication;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import static org.junit.jupiter.api.Assertions.assertThrows;

class DebugAuthenticationHandlerTest {

  @Test
  void handle() {
      SubscribeRequest request = new SubscribeRequest();
      request.setId("test");
      TestAuthentication authentication = new TestAuthentication("test");

      request.setAuthentication(authentication);
      Executable executable = ()-> DebugAuthenticationHandler.handle(request);
      assertThrows(AccessDenyException.class,executable);

      authentication.addPermission("network-config","save");
      DebugAuthenticationHandler.handle(request);
  }
}