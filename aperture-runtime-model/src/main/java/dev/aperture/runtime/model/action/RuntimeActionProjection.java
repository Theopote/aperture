package dev.aperture.runtime.model.action;
/** Family-owned, immutable runtime action projected for clients and tools. */
public record RuntimeActionProjection(String id,String label,boolean enabled,String group,String icon,Severity severity,String tooltip,boolean confirmation,boolean pending,String disabledReason){
	public enum Severity{NORMAL,WARNING,DANGER}
	public RuntimeActionProjection(String id,String label,boolean enabled){this(id,label,enabled,"General","",Severity.NORMAL,"",false,false,enabled?"":"Unavailable in current state");}
	public RuntimeActionProjection{if(id==null||id.isBlank())throw new IllegalArgumentException("id is required");if(label==null||label.isBlank())throw new IllegalArgumentException("label is required");group=group==null||group.isBlank()?"General":group;icon=icon==null?"":icon;severity=severity==null?Severity.NORMAL:severity;tooltip=tooltip==null?"":tooltip;disabledReason=disabledReason==null?"":disabledReason;}
}