package dev.aperture.editor.model.command;

@FunctionalInterface
public interface EditorCommandResultListener {
	void completed(EditorCommandSubmission result);
}
