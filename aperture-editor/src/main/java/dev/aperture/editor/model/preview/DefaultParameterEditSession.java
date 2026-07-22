package dev.aperture.editor.model.preview;
import dev.aperture.editor.model.command.*;
import dev.aperture.parameter.ParameterValue;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import java.util.Objects;
public final class DefaultParameterEditSession implements ParameterEditSession {
	private final ArchitecturalObjectId id; private final String key; private final ParameterValue authoritative; private ParameterValue preview;
	private final ExpectedRevision revision; private final PreviewCoordinator previews; private final EditorCommandGateway gateway; private boolean active=true;
	public DefaultParameterEditSession(ArchitecturalObjectId id,String key,ParameterValue authoritative,ExpectedRevision revision,PreviewCoordinator previews,EditorCommandGateway gateway){this.id=Objects.requireNonNull(id);this.key=Objects.requireNonNull(key);this.authoritative=Objects.requireNonNull(authoritative);this.preview=authoritative;this.revision=Objects.requireNonNull(revision);this.previews=Objects.requireNonNull(previews);this.gateway=Objects.requireNonNull(gateway);}
	public ArchitecturalObjectId objectId(){return id;} public String parameterKey(){return key;} public ParameterValue authoritativeValue(){return authoritative;} public ParameterValue previewValue(){return preview;}
	public void updatePreview(ParameterValue value){ensureActive();preview=Objects.requireNonNull(value);previews.put(id,key,value);}
	public EditorCommandSubmission commit(){ensureActive();active=false;var result=gateway.submit(new SetParameterArchitecturalCommand(id,key,preview),revision);if(result.status()==EditorCommandSubmission.Status.PENDING)previews.associate(result.commandId(),id,key);else previews.clear(id,key);return result;}
	public void cancel(){if(active){active=false;previews.clear(id,key);}} public boolean active(){return active;} private void ensureActive(){if(!active)throw new IllegalStateException("edit session is closed");}
}
