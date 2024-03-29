package org.sodeja.generator.java;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.sodeja.collections.CollectionUtils;

public class JavaClass implements JavaType, JavaAnnotatedElement, JavaAccessModifiable, 
		JavaGenericDeclaration {
	
	public static JavaClass createFromClass(Class<?> clazz) {
		JavaPackage javaPackage = JavaPackage.createFromDots(clazz.getPackage().getName());
		return new JavaClass(javaPackage, clazz.getSimpleName());
	}
	
	private JavaPackage _package;
	
	private List<JavaClass> imports;

	private List<JavaAnnotation> annotations;

	private JavaAccessModifier accessModifier = JavaAccessModifier.PUBLIC;
	private String name;
	private List<JavaTypeVariable> typeParameters;
	private boolean isArray;
	
	private JavaType parent;
	private List<JavaType> interfaces;
	
	private List<JavaMember> members;
	
	public JavaClass(JavaPackage _package, String name) {
		this._package = _package;
		this.name = name;
		this.typeParameters = new ArrayList<JavaTypeVariable>();
		
		this.imports = new ArrayList<JavaClass>();
		this.annotations = new ArrayList<JavaAnnotation>();
		this.interfaces = new ArrayList<JavaType>();
		this.members = new ArrayList<JavaMember>();
	}
	
	public JavaPackage getPackage() {
		return _package;
	}

	public List<JavaClass> getImports() {
		return imports;
	}

	public void addImport(JavaClass javaClass) {
		autoImport(javaClass);
	}

	public JavaAccessModifier getAccessModifier() {
		return accessModifier;
	}

	public String getName() {
		return name;
	}

	public List<JavaTypeVariable> getTypeParameters() {
		return Collections.unmodifiableList(typeParameters);
	}
	
	public void addTypeParameter(JavaTypeVariable param) {
		if(param.getBound() == null) {
			if(CollectionUtils.isEmpty(param.getAdditionalBounds())) {
				throw new IllegalArgumentException();
			}
		}
		
		autoImport(param);
		typeParameters.add(param);
	}

	public JavaType getParent() {
		return parent;
	}

	public void setParent(JavaClass parent) {
		if(parent.getClass() != JavaClass.class) {
			throw new IllegalArgumentException();
		}
		
		autoImport(parent);
		this.parent = parent;
	}
	
	public void setParent(JavaParameterizedType parent) {
		if(parent.getType().getClass() != JavaClass.class) {
			throw new IllegalArgumentException();
		}
		
		autoImport(parent);
		this.parent = parent;
	}
	
	public List<JavaType> getInterfaces() {
		return Collections.unmodifiableList(interfaces);
	}

	public void addInterface(JavaInterface inter) {
		autoImport(inter);
		interfaces.add(inter);
	}
	
	public void addInterface(JavaParameterizedType inter) {
		if(inter.getType().getClass() != JavaInterface.class) {
			throw new IllegalArgumentException();
		}
		
		autoImport(inter);
		interfaces.add(inter);
	}
	
	public List<JavaAnnotation> getAnnotations() {
		return annotations;
	}

	public void addAnnotation(JavaAnnotation annotation) {
		autoImport(annotation.getType());
		annotations.add(annotation);
	}

	public void addAnnotation(JavaClass clazz) {
		addAnnotation(new JavaAnnotation(clazz));
	}
	
	public void addAnnotation(JavaClass clazz, String value) {
		addAnnotation(new JavaAnnotation(clazz, value));
	}

	public List<JavaField> getFields() {
		List<JavaField> result = new ArrayList<JavaField>();
		for(JavaMember member : members) {
			if(member instanceof JavaField) {
				result.add((JavaField) member);
			}
		}
		
		return result;
	}

	public void addField(JavaField field) {
		autoImport(field);
		field.setOwner(this);
		members.add(field);
	}
	
	public List<JavaMethod> getMethods() {
		List<JavaMethod> result = new ArrayList<JavaMethod>();
		for(JavaMember member : members) {
			if(member instanceof JavaMethod) {
				result.add((JavaMethod) member);
			}
		}
		
		return result;
	}
	
	public void addMethod(JavaMethod method) {
		autoImport(method);
		method.setOwner(this);
		members.add(method);
	}

	public List<JavaConstructor> getConstructors() {
		List<JavaConstructor> result = new ArrayList<JavaConstructor>();
		for(JavaMember member : members) {
			if(member instanceof JavaConstructor) {
				result.add((JavaConstructor) member);
			}
		}
		
		return result;
	}
	
	public void addConstructor(JavaConstructor constructor) {
		autoImport(constructor);
		constructor.setOwner(this);
		members.add(constructor);
	}
	
	public boolean isSystem() {
		return _package.getFullName().equals("java.lang");
	}
	
	public boolean isArray() {
		return isArray;
	}

	public void setArray(boolean isArray) {
		this.isArray = isArray;
	}

	public String getFullName() {
		return String.format("%s.%s", getPackage().getFullName(), getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(! (obj instanceof JavaClass)) {
			return false;
		}
		
		JavaClass other = (JavaClass) obj;
		return name.equals(other.name) && _package.equals(other._package);
	}

	private void autoImport(JavaMethod method) {
		autoImportTypeParameters(method);
		autoImportMember(method);
		autoImport(method.getReturnType());
		autoImportParameters(method.getParameters());
	}

	private void autoImport(JavaField field) {
		autoImportMember(field);
		autoImport(field.getType());
	}
	
	private void autoImport(JavaConstructor constructor) {
		autoImportTypeParameters(constructor);
		autoImportMember(constructor);
		autoImportParameters(constructor.getParameters());
	}
	
	private void autoImportTypeParameters(JavaGenericDeclaration declaration) {
		for(JavaTypeVariable param : declaration.getTypeParameters()) {
			autoImport(param);
		}
	}
	
	private void autoImport(JavaTypeVariable param) {
		if(param.getBound() != null) {
			autoImport(param.getBound());
		}
		if(! CollectionUtils.isEmpty(param.getAdditionalBounds())) {
			autoImport(param.getAdditionalBounds());
		}
	}
	
	private void autoImportParameters(List<JavaMethodParameter> params) {
		for(JavaMethodParameter param : params) {
			autoImport(param.getType());
		}
	}
	
	private void autoImportMember(JavaMember member) {
		for(JavaAnnotation annotation : member.getAnnotations()) {
			autoImport(annotation.getType());
		}
	}
	
	private void autoImport(List<? extends JavaClass> clazzes) {
		for(JavaClass clazz : clazzes) {
			autoImport(clazz);
		}
	}
	
	private void autoImport(JavaType type) {
		if(type instanceof JavaClass) {
			autoImport((JavaClass) type);
		} else if(type instanceof JavaPrimitiveType) {
			return;
		} else if(type instanceof JavaTypeVariableReference) {
			return;
		} else if(type instanceof JavaWildcardType) {
			autoImport((JavaWildcardType) type);
		} else if(type instanceof JavaParameterizedType) {
			autoImport((JavaParameterizedType) type);
		} else if(type instanceof JavaArray) {
			autoImport(((JavaArray) type).getType());
		} else {
			throw new IllegalArgumentException();
		}
	}

	private void autoImport(JavaClass clazz) {
		if(clazz._package == null) {
			return;
		}
		
		if(this._package.getFullName().equals(clazz._package.getFullName())) {
			return;
		}
		
		for(JavaClass javaImport : imports) {
			if(javaImport.name == clazz.name) {
				return;
			}
		}
		imports.add(clazz);
	}

	private void autoImport(JavaWildcardType wildcard) {
		if(wildcard.getLowerBounds() != null) {
			autoImport(wildcard.getLowerBounds());
		}
		if(wildcard.getUpperBounds() != null) {
			autoImport(wildcard.getUpperBounds());
		}
	}
	
	private void autoImport(JavaParameterizedType type) {
		autoImport(type.getType());
		for(JavaType typeArgument : type.getTypeArguments()) {
			autoImport(typeArgument);
		}
	}
}
