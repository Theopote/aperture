package dev.aperture.editor.model.inspector;
public record InspectorProperty(String key,String label,Control control,Object value,String unit,boolean readOnly) { public enum Control { NUMBER, BOOLEAN, ENUM, STRING, VECTOR, TRANSFORM, REFERENCE, MATERIAL, TEXT, DIAGNOSTIC } }
