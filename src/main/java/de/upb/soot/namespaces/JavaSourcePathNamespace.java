package de.upb.soot.namespaces;

import de.upb.soot.frontends.ClassSource;
import de.upb.soot.frontends.ResolveException;
import de.upb.soot.frontends.java.WalaClassLoader;
import de.upb.soot.frontends.java.WalaJavaClassProvider;
import de.upb.soot.signatures.SignatureFactory;
import de.upb.soot.types.JavaClassType;
import de.upb.soot.types.TypeFactory;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * An implementation of the {@link INamespace} interface for the Java source code path.
 *
 * @author Linghui Luo
 */
public class JavaSourcePathNamespace extends AbstractNamespace {

  @Nonnull private final Set<String> sourcePaths;

  /**
   * Create a {@link JavaSourcePathNamespace} which locates java source code in the given source
   * path.
   *
   * @param sourcePaths the source code path to search in
   */
  public JavaSourcePathNamespace(@Nonnull Set<String> sourcePaths) {
    super(new WalaJavaClassProvider());

    this.sourcePaths = sourcePaths;
  }

  @Override
  @Nonnull
  public Collection<ClassSource> getClassSources(
      @Nonnull SignatureFactory signatureFactory, TypeFactory typeFactory) {
    return new WalaClassLoader(sourcePaths).getClassSources();
  }

  @Override
  @Nonnull
  public Optional<ClassSource> getClassSource(@Nonnull JavaClassType type) {
    for (String path : sourcePaths) {
      try {
        return Optional.of(getClassProvider().createClassSource(this, Paths.get(path), type));
      } catch (ResolveException ignored) {
        // TODO This is really ugly. Maybe we can make createClassSource return an optional /
        // nullable?
      }
    }
    return Optional.empty();
  }
}
