package dev.aperture.editor.model.session;
import java.util.*;
public final class DefaultWorkspaceModel implements WorkspaceModel { private final Map<String,Boolean> windows=new LinkedHashMap<>(); public DefaultWorkspaceModel(){reset();} public Map<String,Boolean> windowVisibility(){return Map.copyOf(windows);} public void setWindowVisible(String id,boolean visible){windows.put(id,visible);} public void reset(){windows.clear();List.of("outliner","inspector","runtime","history","diagnostics","viewport").forEach(x->windows.put(x,true));} }
