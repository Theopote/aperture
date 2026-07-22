package dev.aperture.client.editor.imgui;

import org.lwjgl.opengl.*;
import org.lwjgl.BufferUtils;

/** Immutable snapshot of OpenGL state that Minecraft expects to survive an ImGui submission. */
final class MinecraftGlStateSnapshot {
	private final int drawFramebuffer=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
	private final int readFramebuffer=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
	private final int drawBuffer=GL11.glGetInteger(GL11.GL_DRAW_BUFFER);
	private final int readBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER);
	private final int[] viewport=get4(GL11.GL_VIEWPORT),scissorBox=get4(GL11.GL_SCISSOR_BOX);
	private final boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST),blend=GL11.glIsEnabled(GL11.GL_BLEND),depth=GL11.glIsEnabled(GL11.GL_DEPTH_TEST),cull=GL11.glIsEnabled(GL11.GL_CULL_FACE);
	private final int blendSrcRgb=GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB),blendDstRgb=GL11.glGetInteger(GL14.GL_BLEND_DST_RGB),blendSrcAlpha=GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA),blendDstAlpha=GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
	private final int blendEquationRgb=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_RGB),blendEquationAlpha=GL11.glGetInteger(GL20.GL_BLEND_EQUATION_ALPHA);
	private final boolean depthMask=GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
	private final boolean[] colorMask=getColorMask();
	private final int program=GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM),activeTexture=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
	private final TextureUnitState activeUnit=TextureUnitState.capture(activeTexture),unitZero=activeTexture==GL13.GL_TEXTURE0?activeUnit:TextureUnitState.capture(GL13.GL_TEXTURE0);
	private final int vao=GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING),arrayBuffer=GL11.glGetInteger(GL15.GL_ARRAY_BUFFER_BINDING),elementBuffer=GL11.glGetInteger(GL15.GL_ELEMENT_ARRAY_BUFFER_BINDING),pixelPackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING),pixelUnpackBuffer=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
	private final int packAlignment=GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT),unpackAlignment=GL11.glGetInteger(GL11.GL_UNPACK_ALIGNMENT);
	private final int packRowLength=GL11.glGetInteger(GL12.GL_PACK_ROW_LENGTH),packSkipPixels=GL11.glGetInteger(GL12.GL_PACK_SKIP_PIXELS),packSkipRows=GL11.glGetInteger(GL12.GL_PACK_SKIP_ROWS);
	private final int unpackRowLength=GL11.glGetInteger(GL12.GL_UNPACK_ROW_LENGTH),unpackSkipPixels=GL11.glGetInteger(GL12.GL_UNPACK_SKIP_PIXELS),unpackSkipRows=GL11.glGetInteger(GL12.GL_UNPACK_SKIP_ROWS);
	private final int[] polygonMode=get2(GL11.GL_POLYGON_MODE);

	static MinecraftGlStateSnapshot capture(){return new MinecraftGlStateSnapshot();}
	void restore(){
		GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFramebuffer);GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFramebuffer);GL11.glDrawBuffer(drawBuffer);GL11.glReadBuffer(readBuffer);
		GL11.glViewport(viewport[0],viewport[1],viewport[2],viewport[3]);GL11.glScissor(scissorBox[0],scissorBox[1],scissorBox[2],scissorBox[3]);
		set(GL11.GL_SCISSOR_TEST,scissor);set(GL11.GL_BLEND,blend);set(GL11.GL_DEPTH_TEST,depth);set(GL11.GL_CULL_FACE,cull);
		GL14.glBlendFuncSeparate(blendSrcRgb,blendDstRgb,blendSrcAlpha,blendDstAlpha);GL20.glBlendEquationSeparate(blendEquationRgb,blendEquationAlpha);
		GL11.glDepthMask(depthMask);GL11.glColorMask(colorMask[0],colorMask[1],colorMask[2],colorMask[3]);GL20.glUseProgram(program);
		unitZero.restore();if(activeUnit!=unitZero)activeUnit.restore();GL13.glActiveTexture(activeTexture);
		GL30.glBindVertexArray(vao);GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER,arrayBuffer);GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER,elementBuffer);GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pixelPackBuffer);GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,pixelUnpackBuffer);
		GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,packAlignment);GL11.glPixelStorei(GL12.GL_PACK_ROW_LENGTH,packRowLength);GL11.glPixelStorei(GL12.GL_PACK_SKIP_PIXELS,packSkipPixels);GL11.glPixelStorei(GL12.GL_PACK_SKIP_ROWS,packSkipRows);
		GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT,unpackAlignment);GL11.glPixelStorei(GL12.GL_UNPACK_ROW_LENGTH,unpackRowLength);GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_PIXELS,unpackSkipPixels);GL11.glPixelStorei(GL12.GL_UNPACK_SKIP_ROWS,unpackSkipRows);
		GL11.glPolygonMode(GL11.GL_FRONT,polygonMode[0]);GL11.glPolygonMode(GL11.GL_BACK,polygonMode[1]);
	}
	private static void set(int capability,boolean enabled){if(enabled)GL11.glEnable(capability);else GL11.glDisable(capability);}
	private static int[] get4(int name){int[] value=new int[4];GL11.glGetIntegerv(name,value);return value;}
	private static int[] get2(int name){int[] value=new int[2];GL11.glGetIntegerv(name,value);return value;}
	private static boolean[] getColorMask(){var bytes=BufferUtils.createByteBuffer(4);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,bytes);return new boolean[]{bytes.get(0)!=0,bytes.get(1)!=0,bytes.get(2)!=0,bytes.get(3)!=0};}
	private record TextureUnitState(int unit,int texture2d,int sampler){
		static TextureUnitState capture(int unit){int previous=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);GL13.glActiveTexture(unit);var state=new TextureUnitState(unit,GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D),GL30.glGetIntegeri(GL33.GL_SAMPLER_BINDING,unit-GL13.GL_TEXTURE0));GL13.glActiveTexture(previous);return state;}
		void restore(){GL13.glActiveTexture(unit);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture2d);GL33.glBindSampler(unit-GL13.GL_TEXTURE0,sampler);}
	}
}
