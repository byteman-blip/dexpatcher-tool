/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.transform;

import lanchon.dexpatcher.core.util.Label;

import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.rewriter.RewriterModule;

public abstract class DexTransform extends BaseDexTransform {

	private static final boolean LOG_TRANSFORMED_DEFINING_CLASS = false;

	protected class MemberContext {
		protected final String definingClass;
		public MemberContext(String definingClass) {
			this.definingClass = definingClass;
		}
	}

	public DexTransform(TransformLogger logger, String logPrefix) {
		super(logger, logPrefix);
	}

	public abstract RewriterModule getRewriterModule();

	public final StringBuilder getMessageHeaderForClass(String descriptor) {
		StringBuilder sb = getMessageHeader();
		sb.append("type '").append(Label.fromClassDescriptor(descriptor)).append("': ");
		return sb;
	}

	public final StringBuilder getMessageHeaderForMember(String definingClass) {
		StringBuilder sb = getMessageHeader();
		sb.append("type '").append(Label.fromClassDescriptor(definingClass));
		if (LOG_TRANSFORMED_DEFINING_CLASS) {
			String rewrittenDefiningClass = getTransformedDefiningClass(definingClass);
			if (rewrittenDefiningClass != null && !rewrittenDefiningClass.equals(definingClass)) {
				sb.append("' -> '").append(Label.fromClassDescriptor(rewrittenDefiningClass));
			}
		}
		sb.append("': ");
		return sb;
	}

	public final StringBuilder getMessageHeaderForField(FieldReference field) {
		StringBuilder sb = getMessageHeaderForMember(field.getDefiningClass());
		sb.append("field '").append(Label.ofField(field)).append("': ");
		return sb;
	}

	public final StringBuilder getMessageHeaderForMethod(MethodReference method) {
		StringBuilder sb = getMessageHeaderForMember(method.getDefiningClass());
		sb.append("method '").append(Label.ofMethod(method)).append("': ");
		return sb;
	}

	protected abstract String getTransformedDefiningClass(String definingClass);

}
