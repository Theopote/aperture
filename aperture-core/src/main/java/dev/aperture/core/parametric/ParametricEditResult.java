package dev.aperture.core.parametric;

import dev.aperture.core.validation.ValidationIssue;

import java.util.ArrayList;
import java.util.List;

public record ParametricEditResult(
	boolean success,
	List<String> changed,
	List<ValidationIssue> issues
) {
	public ParametricEditResult {
		changed = List.copyOf(changed);
		issues = List.copyOf(issues);
	}

	public static ParametricEditResult ok(String name) {
		return new ParametricEditResult(true, List.of(name), List.of());
	}

	public static ParametricEditResult okMany(List<String> names) {
		return new ParametricEditResult(true, List.copyOf(names), List.of());
	}

	public static ParametricEditResult failed(List<ValidationIssue> issues) {
		return new ParametricEditResult(false, List.of(), List.copyOf(issues));
	}

	public static ParametricEditResult failed(String code, String message) {
		return failed(List.of(ValidationIssue.error(code, message)));
	}

	public static Builder builder() {
		return new Builder();
	}

	public static final class Builder {
		private final List<String> changed = new ArrayList<>();
		private final List<ValidationIssue> issues = new ArrayList<>();

		public Builder changed(String name) {
			changed.add(name);
			return this;
		}

		public Builder issue(ValidationIssue issue) {
			issues.add(issue);
			return this;
		}

		public ParametricEditResult build() {
			return new ParametricEditResult(issues.isEmpty(), changed, issues);
		}
	}
}
