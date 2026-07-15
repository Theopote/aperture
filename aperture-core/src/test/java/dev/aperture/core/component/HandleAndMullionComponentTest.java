package dev.aperture.core.component;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandleAndMullionComponentTest {
	@Test
	void mullionComponentDefaultsToMullionsSource() {
		var component = MullionComponent.fromSource("mullions", "parameter:mullions");

		assertEquals(ComponentKind.MULLION, component.kind());
		assertEquals("parameter:mullions", component.source());
	}

	@Test
	void handleComponentUsesHandleKind() {
		var component = HandleComponent.of("handle");

		assertEquals(ComponentKind.HANDLE, component.kind());
		assertEquals("handle", component.ref().id());
	}
}
