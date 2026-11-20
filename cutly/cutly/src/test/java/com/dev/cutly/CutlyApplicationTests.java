package com.dev.cutly;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class CutlyApplicationTests {
	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	void contextLoads() {
	}

	@Test
	void employeeNotificationRoutesAreMappedOnceBySharedController() {
		assertSharedRoute(RequestMethod.GET, "/api/empleado/notificaciones");
		assertSharedRoute(RequestMethod.GET, "/api/empleado/notificaciones/no-leidas/contador");
		assertSharedRoute(RequestMethod.PATCH, "/api/empleado/notificaciones/{id}/leer");
		assertSharedRoute(RequestMethod.PATCH, "/api/empleado/notificaciones/leer-todas");
	}

	private void assertSharedRoute(RequestMethod method, String path) {
		var sharedMappings = handlerMapping.getHandlerMethods().entrySet().stream()
				.filter(entry -> entry.getValue().getBeanType().getSimpleName().equals("CentroNotificacionesController"))
				.map(entry -> entry.getKey().toString())
				.toList();
		var matches = handlerMapping.getHandlerMethods().entrySet().stream()
				.filter(entry -> entry.getKey().getPatternValues().contains(path))
				.filter(entry -> entry.getKey().getMethodsCondition().getMethods().contains(method))
				.toList();

		assertEquals(1, matches.size(), method + " " + path + " must have exactly one handler; found: " + sharedMappings);
		assertTrue(matches.get(0).getValue().getBeanType().getSimpleName().equals("CentroNotificacionesController"),
				method + " " + path + " must use the shared notification controller");
	}
}
