package dev.aperture.editor.model.read;

import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.parameter.ParameterSet;
import dev.aperture.runtime.model.object.ArchitecturalObjectId;
import dev.aperture.runtime.model.replication.ClientReplicaStore;
import java.util.*;

/** Read-only UI projection: authoritative replicas plus a non-authoritative preview overlay. */
public final class ReplicaEditorReadModel implements EditorReadModel {
	private final ClientReplicaStore replicas;
	private final PreviewCoordinator previews;
	private final DiagnosticsModel diagnostics;
	public ReplicaEditorReadModel(ClientReplicaStore replicas, PreviewCoordinator previews, DiagnosticsModel diagnostics) {
		this.replicas=Objects.requireNonNull(replicas); this.previews=Objects.requireNonNull(previews); this.diagnostics=Objects.requireNonNull(diagnostics);
	}
	public Optional<ObjectEditorView> object(ArchitecturalObjectId id) { return replicas.replica(id).map(replica -> {
		var instance=replica.instance(); var values=new LinkedHashMap<>(instance.parameterOverrides().asMap());
		values.replaceAll((key,value)->previews.value(id,key).orElse(value));
		var status=values.equals(instance.parameterOverrides().asMap())?SyncStatus.SYNCHRONIZED:SyncStatus.PREVIEW;
		return new ObjectEditorView(id,instance.typeId(),instance.familyId(),instance.metadata().getOrDefault("displayName",instance.typeId().toString()),
			instance.transform(),instance.hostBindings(),ParameterSet.of(values),replica.state(),instance.revision(),replica.state().revision().value(),status,diagnostics.forObject(id));
	}); }
	public List<ObjectSummary> visibleObjects(){ return replicas.replicas().keySet().stream().sorted(Comparator.comparing(Object::toString)).map(id->object(id).orElseThrow()).map(v->new ObjectSummary(v.objectId(),v.typeId(),v.displayName(),v.syncStatus(),v.diagnostics().stream().anyMatch(d->d.severity()==EditorDiagnostic.Severity.ERROR))).toList(); }
	public List<EditorDiagnostic> diagnostics(ArchitecturalObjectId id){return diagnostics.forObject(id);}
}
