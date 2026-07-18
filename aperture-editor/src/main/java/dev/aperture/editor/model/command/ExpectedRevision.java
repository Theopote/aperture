package dev.aperture.editor.model.command;
public record ExpectedRevision(long objectRevision, long stateRevision) { public ExpectedRevision { if(objectRevision<0||stateRevision<0)throw new IllegalArgumentException("negative revision"); } }
