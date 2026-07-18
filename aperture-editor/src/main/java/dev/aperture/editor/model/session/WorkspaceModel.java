package dev.aperture.editor.model.session;
import java.util.Map;
public interface WorkspaceModel { Map<String,Boolean> windowVisibility(); void setWindowVisible(String id,boolean visible); void reset(); }
