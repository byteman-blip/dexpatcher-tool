/*
 * DexPatcher - Copyright 2015-2019 Rodrigo Balerdi
 * (GNU General Public License version 3 or later)
 *
 * DexPatcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 */

package lanchon.dexpatcher.transform.codec;

import lanchon.dexpatcher.transform.util.wrapper.WrapperRewriterModule;

import org.jf.dexlib2.DebugItemType;
import org.jf.dexlib2.ValueType;
import org.jf.dexlib2.base.reference.BaseStringReference;
import org.jf.dexlib2.base.value.BaseStringEncodedValue;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.MethodParameter;
import org.jf.dexlib2.iface.debug.DebugItem;
import org.jf.dexlib2.iface.debug.EndLocal;
import org.jf.dexlib2.iface.debug.RestartLocal;
import org.jf.dexlib2.iface.debug.StartLocal;
import org.jf.dexlib2.iface.reference.FieldReference;
import org.jf.dexlib2.iface.reference.MethodReference;
import org.jf.dexlib2.iface.reference.StringReference;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.dexlib2.rewriter.AnnotationElementRewriter;
import org.jf.dexlib2.rewriter.DebugItemRewriter;
import org.jf.dexlib2.rewriter.DexRewriter;
import org.jf.dexlib2.rewriter.EncodedValueRewriter;
import org.jf.dexlib2.rewriter.FieldReferenceRewriter;
import org.jf.dexlib2.rewriter.MethodParameterRewriter;
import org.jf.dexlib2.rewriter.MethodReferenceRewriter;
import org.jf.dexlib2.rewriter.Rewriter;
import org.jf.dexlib2.rewriter.RewriterModule;
import org.jf.dexlib2.rewriter.Rewriters;

public class DexCodecModule extends RewriterModule {

	public enum ItemType {
		BINARY_CLASS_NAME("type"),
		FIELD_NAME("field"),
		METHOD_NAME("method"),
		PARAMETER_NAME("parameter"),
		LOCAL_VARIABLE_NAME("local variable"),
		ANNOTATION_ELEMENT_NAME("annotation element name"),
		ANNOTATION_ELEMENT_STRING_VALUE("annotation element value");

		public final String label;

		ItemType(String label) {
			this.label = label;
		}
	}

	public interface ItemRewriter {
		String rewriteItem(String definingClass, ItemType itemType, String value);
	}

	protected final ItemRewriter itemRewriter;

	public DexCodecModule(ItemRewriter itemRewriter) {
		this.itemRewriter = itemRewriter;
	}

	public final String rewriteBinaryClassName(String value) {
		return itemRewriter.rewriteItem(null, ItemType.BINARY_CLASS_NAME, value);
	}

	public final String rewriteFieldName(String definingClass, String value) {
		return itemRewriter.rewriteItem(definingClass, ItemType.FIELD_NAME, value);
	}

	public final String rewriteMethodName(String definingClass, String value) {
		return itemRewriter.rewriteItem(definingClass, ItemType.METHOD_NAME, value);
	}

	public final String rewriteParameterName(String value) {
		return itemRewriter.rewriteItem(null, ItemType.PARAMETER_NAME, value);
	}

	public final String rewriteLocalVariableName(String value) {
		return itemRewriter.rewriteItem(null, ItemType.LOCAL_VARIABLE_NAME, value);
	}

	public final String rewriteAnnotationElementName(String value) {
		return itemRewriter.rewriteItem(null, ItemType.ANNOTATION_ELEMENT_NAME, value);
	}

	public final String rewriteAnnotationElementStringValue(String value) {
		return itemRewriter.rewriteItem(null, ItemType.ANNOTATION_ELEMENT_STRING_VALUE, value);
	}

	@Override
	public Rewriter<String> getTypeRewriter(Rewriters rewriters) {
		return new BinaryClassNameRewriter() {
			@Override
			public String rewriteBinaryClassName(String binaryClassName) {
				return DexCodecModule.this.rewriteBinaryClassName(binaryClassName);
			}
		};
	}

	@Override
	public Rewriter<FieldReference> getFieldReferenceRewriter(Rewriters rewriters) {
		return new FieldReferenceRewriter(rewriters) {
			@Override
			public FieldReference rewrite(FieldReference fieldReference) {
				return new RewrittenFieldReference(fieldReference) {
					@Override
					public String getName() {
						return rewriteFieldName(fieldReference.getDefiningClass(), fieldReference.getName());
					}
				};
			}
		};
	}

	@Override
	public Rewriter<MethodReference> getMethodReferenceRewriter(Rewriters rewriters) {
		return new MethodReferenceRewriter(rewriters) {
			@Override
			public MethodReference rewrite(MethodReference methodReference) {
				return new RewrittenMethodReference(methodReference) {
					@Override
					public String getName() {
						return rewriteMethodName(methodReference.getDefiningClass(), methodReference.getName());
					}
				};
			}
		};
	}

	@Override
	public Rewriter<MethodParameter> getMethodParameterRewriter(Rewriters rewriters) {
		return new MethodParameterRewriter(rewriters) {
			@Override
			public MethodParameter rewrite(MethodParameter methodParameter) {
				return new RewrittenMethodParameter(methodParameter) {
					@Override
					public String getName() {
						return rewriteParameterName(methodParameter.getName());
					}
				};
			}

		};
	}

	@Override
	public Rewriter<DebugItem> getDebugItemRewriter(Rewriters rewriters) {
		return new DebugItemRewriter(rewriters) {
			@Override
			public DebugItem rewrite(DebugItem value) {
				switch (value.getDebugItemType()) {
					case DebugItemType.START_LOCAL:
						return new RewrittenStartLocal((StartLocal) value) {
							@Override
							public String getName() {
								return rewriteLocalVariableName(debugItem.getName());
							}
							@Override
							public StringReference getNameReference() {
								final StringReference ref = debugItem.getNameReference();
								if (ref == null) return null;
								return new BaseStringReference() {
									@Override public String getString() {
										return rewriteLocalVariableName(ref.getString());
									}
								};

							}
						};
					case DebugItemType.END_LOCAL:
						return new RewrittenEndLocal((EndLocal) value) {
							@Override
							public String getName() {
								return rewriteLocalVariableName(debugItem.getName());
							}
						};
					case DebugItemType.RESTART_LOCAL:
						return new RewrittenRestartLocal((RestartLocal) value) {
							@Override
							public String getName() {
								return rewriteLocalVariableName(debugItem.getName());
							}
						};
					default:
						return super.rewrite(value);
				}
			}
		};
	}

	protected static class AnnotationElementCodecModule extends WrapperRewriterModule<DexCodecModule> {
		public AnnotationElementCodecModule(DexCodecModule wrappedModule) {
			super(wrappedModule);
		}
		@Override
		public Rewriter<EncodedValue> getEncodedValueRewriter(Rewriters rewriters) {
			return new EncodedValueRewriter(rewriters) {
				@Override
				public EncodedValue rewrite(EncodedValue value) {
					switch (value.getValueType()) {
						case ValueType.STRING:
							final StringEncodedValue stringValue = (StringEncodedValue) value;
							return new BaseStringEncodedValue() {
								@Override
								public String getValue() {
									return wrappedModule.rewriteAnnotationElementStringValue(stringValue.getValue());
								}
							};
						default:
							return super.rewrite(value);
					}
				}
			};
		}
	}

	protected final DexRewriter annotationElementCodec = new DexRewriter(new AnnotationElementCodecModule(this));

	@Override
	public Rewriter<AnnotationElement> getAnnotationElementRewriter(Rewriters rewriters) {
		return new AnnotationElementRewriter(rewriters) {
			@Override
			public AnnotationElement rewrite(AnnotationElement annotationElement) {
				return new RewrittenAnnotationElement(annotationElement) {
					@Override
					public String getName() {
						return rewriteAnnotationElementName(annotationElement.getName());
					}
					@Override
					public EncodedValue getValue() {
						return annotationElementCodec.getEncodedValueRewriter().rewrite(annotationElement.getValue());
					}
				};
			}
		};
	}

}
