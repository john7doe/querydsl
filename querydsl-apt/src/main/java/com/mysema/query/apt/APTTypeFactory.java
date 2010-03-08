/*
 * Copyright (c) 2009 Mysema Ltd.
 * All rights reserved.
 * 
 */
package com.mysema.query.apt;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.WildcardType;

import com.mysema.query.codegen.EntityType;
import com.mysema.query.codegen.SimpleType;
import com.mysema.query.codegen.Type;
import com.mysema.query.codegen.TypeCategory;
import com.mysema.query.codegen.TypeExtends;
import com.mysema.query.codegen.TypeFactory;
import com.mysema.query.codegen.TypeSuper;
import com.mysema.query.codegen.Types;

/**
 * APTTypeModelFactory is a factory for APT inspection based TypeModel creation
 * 
 * @author tiwe
 *
 */
public final class APTTypeFactory {
    
    @Nullable
    private static Class<?> safeClassForName(String name){
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private final Map<List<String>,Type> cache = new HashMap<List<String>,Type>();
    
    private final Configuration configuration;
    
    private final Type defaultValue;
    
    private final List<Class<? extends Annotation>> entityAnnotations;
    
    private final Map<List<String>,EntityType> entityTypeCache = new HashMap<List<String>,EntityType>();
    
    private final ProcessingEnvironment env;
    
    private final TypeFactory factory;
    
    private final TypeElement numberType, comparableType;
    
    public APTTypeFactory(ProcessingEnvironment env, Configuration configuration, 
            TypeFactory factory, List<Class<? extends Annotation>> annotations){
        this.env = env;
        this.configuration = configuration;
        this.factory = factory;
        this.defaultValue = factory.create(Object.class);       
        this.entityAnnotations = annotations;
        this.numberType = env.getElementUtils().getTypeElement(Number.class.getName());
        this.comparableType = env.getElementUtils().getTypeElement(Comparable.class.getName());        
    }
    
    private Type create(TypeElement typeElement, TypeCategory category, List<? extends TypeMirror> typeArgs) {
        String name = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        String packageName = env.getElementUtils().getPackageOf(typeElement).getQualifiedName().toString();
        Type[] params = new Type[typeArgs.size()];
        for (int i = 0; i < params.length; i++){
            params[i] = create(typeArgs.get(i));
        }        
        return new SimpleType(category, 
            name, packageName, simpleName, 
            typeElement.getModifiers().contains(Modifier.FINAL), 
            params);
    }

    @Nullable
    public Type create(TypeMirror type){
        List<String> key = createKey(type, true, true);  
        if (entityTypeCache.containsKey(key)){
            return entityTypeCache.get(key);
            
        }else if (cache.containsKey(key)){
            return cache.get(key);
            
        }else{
            cache.put(key, null);
            Type typeModel = handle(type);
            if (typeModel != null && typeModel.getCategory() == TypeCategory.ENTITY){
                EntityType entityType = createEntityType(type);
                cache.put(key, entityType);
                return entityType;
            }else{
                cache.put(key, typeModel);
                return typeModel;
            }                        
        }        
    }

    private Type createClassType(DeclaredType t, TypeElement typeElement) {   
        // entity type
        for (Class<? extends Annotation> entityAnn : entityAnnotations){
            if (typeElement.getAnnotation(entityAnn) != null){
                return create(typeElement, TypeCategory.ENTITY,  t.getTypeArguments());
            }
        }        
        
        // other
        String name = typeElement.getQualifiedName().toString();
        TypeCategory typeCategory = TypeCategory.get(name);
        
        if (typeCategory != TypeCategory.NUMERIC
                && isAssignable(typeElement, comparableType)
                && isSubType(typeElement, numberType)){
            typeCategory = TypeCategory.NUMERIC;
            
        }else if (!typeCategory.isSubCategoryOf(TypeCategory.COMPARABLE)
                && isAssignable(typeElement, comparableType)){
            typeCategory = TypeCategory.COMPARABLE;
        }
        return create(typeElement, typeCategory, t.getTypeArguments());
    }

    @Nullable
    public EntityType createEntityType(TypeMirror type){
        List<String> key = createKey(type, false, true);
        if (entityTypeCache.containsKey(key)){
            return entityTypeCache.get(key);
        
        }else{            
            entityTypeCache.put(key, null);
            Type value = handle(type);
            if (value != null){                
                EntityType entityModel = new EntityType(configuration.getNamePrefix(), value);
                entityTypeCache.put(key, entityModel);
                cache.put(createKey(type, true, true), entityModel);
                for (EntityType superType : getSupertypes(type, value)){
                    entityModel.getSuperTypes().add(superType);
                }
                return entityModel;
            }else{
                return null;
            }
        }
    }
    
    private Type createEnumType(DeclaredType t, TypeElement typeElement) {
        for (Class<? extends Annotation> entityAnn : entityAnnotations){
            if (typeElement.getAnnotation(entityAnn) != null){
                return create(typeElement, TypeCategory.ENTITY, t.getTypeArguments());
            }
        }  
        
        // fallback
        return create(typeElement, TypeCategory.SIMPLE, t.getTypeArguments());
    }
    
    private Type createInterfaceType(DeclaredType t, TypeElement typeElement) {
        // entity type
        for (Class<? extends Annotation> entityAnn : entityAnnotations){
            if (typeElement.getAnnotation(entityAnn) != null){
                return create(typeElement, TypeCategory.ENTITY, t.getTypeArguments());
            }
        }       
                
        String name = typeElement.getQualifiedName().toString();
        String simpleName = typeElement.getSimpleName().toString();
        Iterator<? extends TypeMirror> i = t.getTypeArguments().iterator();
        Class<?> cl = safeClassForName(name);
        if (cl == null) { // class not available
            return create(typeElement, TypeCategory.get(name), t.getTypeArguments());
            
        }else if (Map.class.isAssignableFrom(cl)){
            if (!i.hasNext()){
                throw new TypeArgumentsException(simpleName);
            }                    
            return factory.createMapType(create(i.next()), create(i.next()));

        } else if (List.class.isAssignableFrom(cl)) {
            if (!i.hasNext()){
                throw new TypeArgumentsException(simpleName);
            }                    
            return factory.createListType(create(i.next()));
            
        } else if (Set.class.isAssignableFrom(cl)) {
            if (!i.hasNext()){
                throw new TypeArgumentsException(simpleName);
            }                    
            return factory.createSetType(create(i.next()));
            
            
        } else if (Collection.class.isAssignableFrom(cl)) {
            if (!i.hasNext()){
                throw new TypeArgumentsException(simpleName);
            }                    
            return factory.createCollectionType(create(i.next()));
        
        }else{
            return create(typeElement, TypeCategory.get(name), t.getTypeArguments());
        }
    }

    private List<String> createKey(TypeMirror type, boolean useTypeArgs, boolean deep){
        List<String> key = new ArrayList<String>();
        String name = type.toString();
        if (name.contains("<")){
            name = name.substring(0, name.indexOf('<'));
        }        
        key.add(name);
        
        if (type.getKind() == TypeKind.TYPEVAR){
            TypeVariable t = (TypeVariable)type;
            if (t.getUpperBound() != null){
                key.addAll(createKey(t.getUpperBound(), useTypeArgs, false));
            }            
            if (t.getLowerBound() != null){
                key.addAll(createKey(t.getLowerBound(), useTypeArgs, false));
            }
            
        }else if (type.getKind() == TypeKind.WILDCARD){
            WildcardType t = (WildcardType)type;
            if (t.getExtendsBound() != null){
                key.addAll(createKey(t.getExtendsBound(), useTypeArgs, false));
            }
            if (t.getSuperBound() != null){
                key.addAll(createKey(t.getSuperBound(), useTypeArgs, false));
            }
            
        }else if (type.getKind() == TypeKind.DECLARED){                    
            DeclaredType t = (DeclaredType)type;
            if (useTypeArgs){
                for (TypeMirror arg : t.getTypeArguments()){
                    if (deep){
                        key.addAll(createKey(arg, useTypeArgs, false));
                    }else{
                        key.add(arg.toString());
                    }
                }    
            }            
        }
        return key;        
    }

    private Set<EntityType> getSupertypes(TypeMirror t, Type value) {                 
        TypeMirror type = normalize(t);        
        Set<EntityType> superTypes = Collections.emptySet();
        if (type.getKind() == TypeKind.DECLARED){
            DeclaredType declaredType = (DeclaredType)type;
            TypeElement e = (TypeElement)declaredType.asElement();
            if (e.getKind() == ElementKind.CLASS){
                if (e.getSuperclass().getKind() != TypeKind.NONE){    
                    TypeMirror supertype = normalize(e.getSuperclass());
                    Type superClass = create(supertype);
                    if (!superClass.getFullName().startsWith("java")){
                        superTypes = Collections.singleton(createEntityType(supertype));    
                    }                        
                }                
            }else{
                superTypes = new HashSet<EntityType>(e.getInterfaces().size());
                for (TypeMirror mirror : e.getInterfaces()){
                    EntityType iface = createEntityType(mirror);
                    if (!iface.getFullName().startsWith("java")){
                        superTypes.add(iface);
                    }
                }
            }
            
        }else{
            throw new IllegalArgumentException("Unsupported type kind " + type.getKind());
        }
        return superTypes;
    }

    private TypeMirror normalize(TypeMirror type) {
        if (type.getKind() == TypeKind.TYPEVAR){
            TypeVariable typeVar = (TypeVariable)type;
            if (typeVar.getUpperBound() != null){
                type = typeVar.getUpperBound();
            }
        }else if (type.getKind() == TypeKind.WILDCARD){
            WildcardType wildcard = (WildcardType)type;
            if (wildcard.getExtendsBound() != null){
                type = wildcard.getExtendsBound();
            }
        }
        return type;
    }

    @Nullable
    private Type handle(TypeMirror type) {
        if (type instanceof DeclaredType){
            DeclaredType t = (DeclaredType)type;
            if (t.asElement() instanceof TypeElement){
                TypeElement typeElement = (TypeElement)t.asElement();
                switch(typeElement.getKind()){
                case ENUM:      return createEnumType(t, typeElement);
                case CLASS:     return createClassType(t, typeElement);
                case INTERFACE: return createInterfaceType(t, typeElement);
                default: throw new IllegalArgumentException("Illegal type " + typeElement);
                }            
            }else{
                throw new IllegalArgumentException("Unsupported element type " + t.asElement());
            }
            
        }else if (type instanceof TypeVariable){
            TypeVariable t = (TypeVariable)type;
            String varName = t.toString();
            if (t.getUpperBound() != null){
                return new TypeExtends(varName, handle(t.getUpperBound()));
            }else if (t.getLowerBound() != null && !(t.getLowerBound() instanceof NullType)){
                return new TypeSuper(varName, handle(t.getLowerBound()));
            }else{
                return null;
            }              
            
        }else if (type instanceof WildcardType){
            WildcardType t = (WildcardType)type;
            if (t.getExtendsBound() != null){
                return new TypeExtends(handle(t.getExtendsBound()));
            }else if (t.getSuperBound() != null){
                return new TypeSuper(handle(t.getSuperBound()));
            }else{            
                return null;
            }
            
        }else if (type instanceof ArrayType){
            ArrayType t = (ArrayType)type;
            return create(t.getComponentType()).asArrayType();
            
        }else if (type instanceof PrimitiveType){
            PrimitiveType t = (PrimitiveType)type;
            switch (t.getKind()) {
            case BOOLEAN: return Types.BOOLEAN;
            case BYTE: return Types.BYTE;
            case CHAR: return Types.CHAR;
            case DOUBLE: return Types.DOUBLE;
            case FLOAT: return Types.FLOAT;
            case INT: return Types.INT;
            case LONG: return Types.LONG;
            case SHORT: return Types.SHORT;
            }
            throw new IllegalArgumentException("Unsupported type " + t.getKind());

            
        }else if (type instanceof NoType){
            return defaultValue;

        }else{
            return null;    
        }        
    }

    private boolean isAssignable(TypeElement type1, TypeElement type2) {
        TypeMirror t1 = type1.asType();
        TypeMirror t2 = env.getTypeUtils().erasure(type2.asType());
        return env.getTypeUtils().isAssignable(t1, t2);
    }

    
    private boolean isSubType(TypeElement type1, TypeElement type2) {
        return env.getTypeUtils().isSubtype(type1.asType(), type2.asType());
    }

    
}