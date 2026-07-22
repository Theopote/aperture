package dev.aperture.editor.interaction;

import dev.aperture.editor.model.command.EditorCommandSubmission;
import dev.aperture.editor.model.command.ExpectedRevision;
import dev.aperture.editor.model.preview.DefaultParameterEditSession;
import dev.aperture.editor.model.preview.ParameterEditSession;
import dev.aperture.editor.model.preview.PreviewCoordinator;
import dev.aperture.editor.model.read.SyncStatus;
import dev.aperture.editor.model.read.ObjectEditorView;
import dev.aperture.editor.model.session.EditorSession;
import dev.aperture.parameter.ParameterType;
import dev.aperture.parameter.ParameterValue;

import java.util.Objects;
import java.util.Optional;

/** Owns value math and the preview/commit lifecycle for a linear parameter drag. */
public final class LinearParameterDragSession {
	private static final double EPSILON = .001;
	private final ParameterEditSession edit;
	private final ManipulatorDescriptor descriptor;
	private final PreviewCoordinator previews;
	private final double initialValue;
	private double previewValue;
	private ToolInteractionState state = ToolInteractionState.DRAGGING;
	private EditorCommandSubmission submission;

	private LinearParameterDragSession(ParameterEditSession edit, ManipulatorDescriptor descriptor, PreviewCoordinator previews) {
		this.edit = Objects.requireNonNull(edit);
		this.descriptor = Objects.requireNonNull(descriptor);
		this.previews = Objects.requireNonNull(previews);
		edit.authoritativeValue().validateType(descriptor.unit());
		this.initialValue = edit.authoritativeValue().asNumber();
		this.previewValue = initialValue;
	}

	public static Optional<LinearParameterDragSession> begin(EditorSession session, ObjectEditorView view,
		ManipulatorDescriptor descriptor) {
		ParameterValue value = view.parameters().get(descriptor.parameterKey()).orElse(null);
		if (value == null || value.type() != descriptor.unit()) return Optional.empty();
		var edit = new DefaultParameterEditSession(view.objectId(), descriptor.parameterKey(), value,
			new ExpectedRevision(view.objectRevision(), view.stateRevision()), session.preview(), session.commands());
		return Optional.of(new LinearParameterDragSession(edit, descriptor, session.preview()));
	}

	public double updateDelta(double delta, boolean fineSnap, boolean snapDisabled) {
		ensureDragging();
		double signedDelta = descriptor.direction() == ManipulatorDescriptor.DirectionPolicy.POSITIVE ? delta : -delta;
		double raw = initialValue + signedDelta;
		double increment = fineSnap ? descriptor.fineSnapIncrement() : descriptor.snapIncrement();
		double snapped = snapDisabled ? raw : Math.round(raw / increment) * increment;
		double constrained = Math.max(descriptor.minimum().orElse(defaultMinimum()),
			Math.min(descriptor.maximum().orElse(Double.MAX_VALUE), snapped));
		if (Math.abs(constrained - previewValue) >= EPSILON) {
			previewValue = constrained;
			edit.updatePreview(valueOf(descriptor.unit(), constrained));
		}
		return previewValue;
	}

	public Optional<EditorCommandSubmission> finish() {
		if (state != ToolInteractionState.DRAGGING) return Optional.ofNullable(submission);
		if (Math.abs(previewValue - initialValue) < EPSILON) {
			edit.cancel();
			state = ToolInteractionState.CANCELLED;
			return Optional.empty();
		}
		submission = edit.commit();
		state = switch (submission.status()) {
			case PENDING -> ToolInteractionState.PENDING;
			case ACCEPTED -> ToolInteractionState.ACCEPTED_WAITING_REPLICA;
			case REJECTED -> ToolInteractionState.REJECTED;
			case REVISION_CONFLICT -> ToolInteractionState.CONFLICT;
		};
		return Optional.of(submission);
	}

	public void cancel() {
		if (state != ToolInteractionState.DRAGGING) return;
		edit.cancel();
		state = ToolInteractionState.CANCELLED;
	}

	public void refresh(ObjectEditorView view) {
		if (submission == null) return;
		var previewState = previews.state(submission.commandId()).orElse(null);
		if (previewState != null) state = switch (previewState) {
			case PENDING, EDITING -> ToolInteractionState.PENDING;
			case ACCEPTED_WAITING_REPLICA -> ToolInteractionState.ACCEPTED_WAITING_REPLICA;
			case REJECTED -> ToolInteractionState.REJECTED;
			case CONFLICT, RESYNCING -> ToolInteractionState.CONFLICT;
			case COMPLETED -> ToolInteractionState.IDLE;
		};
		if (state == ToolInteractionState.ACCEPTED_WAITING_REPLICA
			&& view.syncStatus() == SyncStatus.SYNCHRONIZED
			&& view.parameters().get(descriptor.parameterKey()).map(ParameterValue::asNumber)
				.filter(value -> Math.abs(value - previewValue) < EPSILON).isPresent()) {
			state = ToolInteractionState.IDLE;
			previews.dismiss(submission.commandId());
		}
	}

	public void dismiss() {
		if (submission != null) previews.dismiss(submission.commandId());
		state = ToolInteractionState.IDLE;
	}

	public double initialValue() { return initialValue; }
	public double previewValue() { return previewValue; }
	public ToolInteractionState state() { return state; }

	private double defaultMinimum() {
		return descriptor.unit() == ParameterType.LENGTH || descriptor.unit() == ParameterType.COUNT ? 0.0 : -Double.MAX_VALUE;
	}

	private static ParameterValue valueOf(ParameterType type, double value) {
		return switch (type) {
			case LENGTH -> ParameterValue.length(value);
			case ANGLE -> ParameterValue.angle(value);
			case COUNT -> ParameterValue.count((int) Math.round(value));
			case NUMBER -> ParameterValue.number(value);
			default -> throw new IllegalArgumentException("Linear manipulation does not support " + type);
		};
	}

	private void ensureDragging() {
		if (state != ToolInteractionState.DRAGGING) throw new IllegalStateException("drag session is " + state);
	}
}
