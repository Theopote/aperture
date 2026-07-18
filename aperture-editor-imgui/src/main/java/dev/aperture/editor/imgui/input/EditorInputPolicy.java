package dev.aperture.editor.imgui.input;
public record EditorInputPolicy(boolean wantMouse, boolean wantKeyboard, boolean textInputActive) {
	public boolean suppressMinecraftMouse(){return wantMouse;}
	public boolean suppressMinecraftKeyboard(){return wantKeyboard||textInputActive;}
	public boolean allowGlobalShortcut(){return !textInputActive;}
}
