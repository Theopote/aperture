package dev.aperture.geometry.profile;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileScalerTest {
	@Test
	void scalesProfileToTargetFrameWidth() {
		ProfileCurve source = ProfileCurve.rectangle(0, 0, 50, 80);
		ProfileCurve scaled = ProfileScaler.scaleToWidth(source, 60);

		assertEquals(60, scaled.bounds().width(), 0.01);
		assertEquals(96, scaled.bounds().depth(), 0.01);
	}

	@Test
	void scalesProfileToIndependentWidthAndDepth() {
		ProfileCurve source = BuiltinProfiles.frameRect(50, 50);
		ProfileCurve scaled = ProfileScaler.scaleToSize(source, 60, 40);

		assertEquals(60, scaled.bounds().width(), 0.01);
		assertEquals(40, scaled.bounds().depth(), 0.01);
	}
}
