package dev.aperture.editor.imgui.style;
public record EditorStyleTokens(float spacingXs,float spacingS,float spacingM,float spacingL,float panelPadding,float rowHeight,int accent,int warning,int error,int success,int mutedText,int selectedRow,int hoverRow,int border) {
	public static EditorStyleTokens darkProfessional(float scale){if(scale<=0||!Float.isFinite(scale))throw new IllegalArgumentException("invalid scale");return new EditorStyleTokens(2*scale,4*scale,8*scale,12*scale,8*scale,22*scale,0xFFB87942,0xFF50B8E8,0xFF5555E8,0xFF72B35A,0xFF909090,0xFF704B32,0xFF40362F,0xFF353535);}
}
