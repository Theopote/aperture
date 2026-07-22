package dev.aperture.editor.model.read;
/** Capability-derived runtime intent exposed to frontends without leaking mutable capabilities. */
public record RuntimeActionDescriptor(String id,String label,boolean enabled,String group,String icon,Severity severity,String tooltip,boolean confirmation,boolean pending,String disabledReason){
	public enum Severity{NORMAL,WARNING,DANGER}
	public RuntimeActionDescriptor(String id,String label,boolean enabled){this(id,label,enabled,"General","",Severity.NORMAL,"",false,false,enabled?"":"Unavailable in current state");}
	public RuntimeActionDescriptor{if(id==null||id.isBlank())throw new IllegalArgumentException("id is required");if(label==null||label.isBlank())throw new IllegalArgumentException("label is required");group=group==null||group.isBlank()?"General":group;icon=icon==null?"":icon;severity=severity==null?Severity.NORMAL:severity;tooltip=tooltip==null?"":tooltip;disabledReason=disabledReason==null?"":disabledReason;}
}