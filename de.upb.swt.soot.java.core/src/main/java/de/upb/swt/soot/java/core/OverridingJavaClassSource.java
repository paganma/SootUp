package de.upb.swt.soot.java.core;

import de.upb.swt.soot.core.frontend.ResolveException;
import de.upb.swt.soot.core.frontend.SootClassSource;
import de.upb.swt.soot.core.inputlocation.AnalysisInputLocation;
import de.upb.swt.soot.core.model.Modifier;
import de.upb.swt.soot.core.model.Position;
import de.upb.swt.soot.core.model.SootField;
import de.upb.swt.soot.core.model.SootMethod;
import de.upb.swt.soot.core.types.ClassType;
import de.upb.swt.soot.core.util.CollectionUtils;
import java.nio.file.Path;
import java.util.*;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Allows for replacing specific parts of a class, such as fields and methods or, allows to resolve
 * classes that are batchparsed like .java files using wala java source frontend or in tests where
 * all information is already existing.
 *
 * <p>When replacing specific parts of a class by default, it delegates to the {@link
 * SootClassSource} delegate provided in the constructor.
 *
 * <p>To alter the results of invocations to e.g. {@link #resolveFields()}, simply call {@link
 * #withFields(Collection)} to obtain a new {@link OverridingJavaClassSource}. The new instance will
 * then use the supplied value instead of calling {@link #resolveFields()} on the delegate.
 *
 * @author Christian Brüggemann, Hasitha Rajapakse
 */
@SuppressWarnings({"OptionalUsedAsFieldOrParameterType", "OptionalAssignedToNull"})
public class OverridingJavaClassSource extends JavaSootClassSource {

  @Nullable private final Collection<SootMethod> overriddenSootMethods;
  @Nullable private final Collection<SootField> overriddenSootFields;
  @Nullable private final Set<Modifier> overriddenModifiers;
  @Nullable private final Set<ClassType> overriddenInterfaces;
  @Nullable private final Optional<ClassType> overriddenSuperclass;
  @Nullable private final Optional<ClassType> overriddenOuterClass;
  @Nullable private final Position position;

  @Nullable private final JavaSootClassSource delegate;
  @Nullable private final Iterable<AnnotationType> annotations;
  // TODO: [ms] how to instantiate in a good way.. @Nullable private final Iterable<AnnotationType>
  // fieldAnnotations;

  public OverridingJavaClassSource(@Nonnull JavaSootClassSource delegate) {
    super(delegate);
    this.delegate = delegate;
    overriddenSootMethods = null;
    overriddenSootFields = null;
    overriddenModifiers = null;
    overriddenInterfaces = null;
    overriddenSuperclass = null;
    overriddenOuterClass = null;
    position = null;
    annotations = null;
  }

  private OverridingJavaClassSource(
      @Nullable Collection<SootMethod> overriddenSootMethods,
      @Nullable Collection<SootField> overriddenSootFields,
      @Nullable Set<Modifier> overriddenModifiers,
      @Nullable Set<ClassType> overriddenInterfaces,
      @Nullable Optional<ClassType> overriddenSuperclass,
      @Nullable Optional<ClassType> overriddenOuterClass,
      @Nullable Position position,
      @Nullable Iterable<AnnotationType> annotations,
      @Nonnull JavaSootClassSource delegate) {
    super(delegate);
    this.overriddenSootMethods = overriddenSootMethods;
    this.overriddenSootFields = overriddenSootFields;
    this.overriddenModifiers = overriddenModifiers;
    this.overriddenInterfaces = overriddenInterfaces;
    this.overriddenSuperclass = overriddenSuperclass;
    this.overriddenOuterClass = overriddenOuterClass;
    this.position = position;
    this.delegate = delegate;
    this.annotations = annotations;
  }

  /** Class source where all information already available */
  public OverridingJavaClassSource(
      @Nonnull AnalysisInputLocation srcNamespace,
      @Nonnull Path sourcePath,
      @Nonnull ClassType classType,
      @Nonnull ClassType superClass,
      @Nonnull Set<ClassType> interfaces,
      @Nonnull ClassType outerClass,
      @Nonnull Set<SootField> sootFields,
      @Nonnull Set<SootMethod> sootMethods,
      @Nonnull Position position,
      @Nonnull EnumSet<Modifier> modifiers,
      @Nonnull Iterable<AnnotationType> annotations) {
    super(srcNamespace, classType, sourcePath);

    this.delegate = null;
    this.overriddenSootMethods = sootMethods;
    this.overriddenSootFields = sootFields;
    this.overriddenModifiers = modifiers;
    this.overriddenInterfaces = interfaces;
    this.overriddenSuperclass = Optional.ofNullable(superClass);
    this.overriddenOuterClass = Optional.ofNullable(outerClass);
    this.position = position;
    this.annotations = annotations;
  }

  @Nonnull
  @Override
  public Collection<? extends SootMethod> resolveMethods() throws ResolveException {
    return overriddenSootMethods != null ? overriddenSootMethods : delegate.resolveMethods();
  }

  @Nonnull
  @Override
  public Collection<? extends SootField> resolveFields() throws ResolveException {
    return overriddenSootFields != null ? overriddenSootFields : delegate.resolveFields();
  }

  @Nonnull
  @Override
  public Set<Modifier> resolveModifiers() {
    return overriddenModifiers != null ? overriddenModifiers : delegate.resolveModifiers();
  }

  @Nonnull
  @Override
  public Set<ClassType> resolveInterfaces() {
    return overriddenInterfaces != null ? overriddenInterfaces : delegate.resolveInterfaces();
  }

  @Nonnull
  @Override
  public Optional<ClassType> resolveSuperclass() {
    return overriddenSuperclass != null ? overriddenSuperclass : delegate.resolveSuperclass();
  }

  @Nonnull
  @Override
  public Optional<ClassType> resolveOuterClass() {
    return overriddenOuterClass != null ? overriddenOuterClass : delegate.resolveOuterClass();
  }

  @Nonnull
  @Override
  public Position resolvePosition() {
    return position != null ? position : delegate.resolvePosition();
  }

  @Nonnull
  @Override
  public Iterable<AnnotationType> resolveAnnotations() {
    //  return annotations != null ? annotations : delegate.resolveAnnotations();
    // TODO: [ms] implement
    return null;
  }

  @Override
  public Iterable<AnnotationType> resolveMethodAnnotations() {
    // TODO: [ms] implement
    return null;
  }

  @Nonnull
  @Override
  public Iterable<AnnotationType> resolveFieldAnnotations() {
    //  return fieldAnnotations != null ? fieldAnnotations : delegate.resolveFieldAnnotations();
    // TODO: [ms] implement
    return null;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OverridingJavaClassSource that = (OverridingJavaClassSource) o;
    return Objects.equals(this.overriddenSuperclass, that.overriddenSuperclass)
        && Objects.equals(this.overriddenInterfaces, that.overriddenInterfaces)
        && Objects.equals(this.overriddenOuterClass, that.overriddenOuterClass)
        && Objects.equals(this.overriddenSootFields, that.overriddenSootFields)
        && Objects.equals(this.overriddenSootMethods, that.overriddenSootMethods)
        && Objects.equals(position, that.position)
        && Objects.equals(this.overriddenModifiers, that.overriddenModifiers)
        && Objects.equals(this.classSignature, that.classSignature);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        this.overriddenSuperclass,
        this.overriddenInterfaces,
        this.overriddenOuterClass,
        this.overriddenSootFields,
        this.overriddenSootMethods,
        this.position,
        this.overriddenModifiers,
        this.classSignature);
  }

  @Override
  public String toString() {
    return "frontend.OverridingClassSource{"
        + "superClass="
        + this.overriddenSuperclass
        + ", interfaces="
        + this.overriddenInterfaces
        + ", outerClass="
        + this.overriddenOuterClass
        + ", sootFields="
        + this.overriddenSootFields
        + ", sootMethods="
        + this.overriddenSootMethods
        + ", position="
        + this.position
        + ", modifiers="
        + this.overriddenModifiers
        + ", classType="
        + this.classSignature
        + '}';
  }

  @Nonnull
  public OverridingJavaClassSource withReplacedMethod(
      @Nonnull SootMethod toReplace, @Nonnull SootMethod replacement) {
    Set<SootMethod> newMethods = new HashSet<>(resolveMethods());
    CollectionUtils.replace(newMethods, toReplace, replacement);
    return withMethods(newMethods);
  }

  @Nonnull
  public OverridingJavaClassSource withMethods(
      @Nonnull Collection<SootMethod> overriddenSootMethods) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withReplacedField(
      @Nonnull SootField toReplace, @Nonnull SootField replacement) {
    Set<SootField> newFields = new HashSet<>(resolveFields());
    CollectionUtils.replace(newFields, toReplace, replacement);
    return withFields(newFields);
  }

  @Nonnull
  public OverridingJavaClassSource withFields(@Nonnull Collection<SootField> overriddenSootFields) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withModifiers(@Nonnull Set<Modifier> overriddenModifiers) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withInterfaces(@Nonnull Set<ClassType> overriddenInterfaces) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withSuperclass(
      @Nonnull Optional<ClassType> overriddenSuperclass) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withOuterClass(
      @Nonnull Optional<ClassType> overriddenOuterClass) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }

  @Nonnull
  public OverridingJavaClassSource withPosition(@Nullable Position position) {
    return new OverridingJavaClassSource(
        overriddenSootMethods,
        overriddenSootFields,
        overriddenModifiers,
        overriddenInterfaces,
        overriddenSuperclass,
        overriddenOuterClass,
        position,
        annotations,
        delegate);
  }
}
