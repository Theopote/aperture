package dev.aperture.client.editor.imgui;

/** Exception-safe ownership boundary around a foreign OpenGL renderer. */
final class RenderStateGuard implements AutoCloseable {
	private final MinecraftGlStateSnapshot snapshot=MinecraftGlStateSnapshot.capture();
	private boolean closed;
	private RenderStateGuard(){}
	static RenderStateGuard capture(){return new RenderStateGuard();}
	@Override public void close(){if(!closed){closed=true;snapshot.restore();}}
}
