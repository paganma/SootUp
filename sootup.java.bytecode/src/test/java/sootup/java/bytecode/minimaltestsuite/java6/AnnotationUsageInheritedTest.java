package sootup.java.bytecode.minimaltestsuite.java6;


import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.signatures.PackageName;
import sootup.java.bytecode.minimaltestsuite.MinimalBytecodeTestSuiteBase;
import sootup.java.core.AnnotationUsage;
import sootup.java.core.JavaSootClass;
import sootup.java.core.language.JavaJimple;
import sootup.java.core.types.AnnotationType;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MinimalBytecodeTestSuiteBase.CustomTestWatcher.class)
public class AnnotationUsageInheritedTest extends MinimalBytecodeTestSuiteBase {

  @Test
  public void testInheritedAnnotationOnClass() {
    JavaSootClass sootClass = loadClass(getDeclaredClassSignature());
    Map<String, Object> annotationParamMap = new HashMap<>();

    annotationParamMap.put("sthBlue", IntConstant.getInstance(42));
    annotationParamMap.put("author", JavaJimple.getInstance().newStringConstant("GeorgeLucas"));

    assertEquals(
        Arrays.asList(
            new AnnotationUsage(
                new AnnotationType("OnClass", new PackageName(""), true), annotationParamMap)),
        sootClass.getAnnotations(Optional.of(CustomTestWatcher.getCustomTestWatcher().getJavaView())));
  }
}
