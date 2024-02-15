package sootup.java.bytecode.minimaltestsuite;


import java.io.File;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import categories.TestCategories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.*;
import sootup.core.model.Body;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.core.util.Utils;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Markus Schmidt,
 * @author Hasitha Rajapakse
 * @author Kaustubh Kelkar
 */
@Tag(TestCategories.JAVA_8_CATEGORY)
@ExtendWith(MinimalBytecodeTestSuiteBase.CustomTestWatcher.class)
public abstract class MinimalBytecodeTestSuiteBase {

  static final String baseDir = "../shared-test-resources/miniTestSuite";
  protected JavaIdentifierFactory identifierFactory = JavaIdentifierFactory.getInstance();

  public static class CustomTestWatcher implements AfterAllCallback, BeforeAllCallback, BeforeEachCallback {
    private String classPath = MinimalBytecodeTestSuiteBase.class.getSimpleName();
    private JavaView javaView;

    public static CustomTestWatcher customTestWatcher = new CustomTestWatcher();

    public static CustomTestWatcher getCustomTestWatcher() {
      return customTestWatcher;
    }

    /** Load View once for each test directory */

    public String getClassPath() {
      return classPath;
    }

    public JavaView getJavaView() {
      return javaView;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {

    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
      CustomTestWatcher customTestWatcher = new CustomTestWatcher();

    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) throws Exception {
      String prevClassDirName = getTestDirectoryName(getClassPath());
      classPath = extensionContext.getTestClass().get().getName();
      if (!prevClassDirName.equals(getTestDirectoryName(getClassPath()))) {
        javaView =
                new JavaView(
                        new JavaClassPathAnalysisInputLocation(
                                baseDir
                                        + File.separator
                                        + getTestDirectoryName(getClassPath())
                                        + File.separator
                                        + "binary"
                                        + File.separator));
      }
    }
  }

  public MethodSignature getMethodSignature() {
    fail("getMethodSignature() is used but not overridden");
    return null;
  }

  public List<String> expectedBodyStmts() {
    fail("expectedBodyStmts() is used but not overridden");
    return null;
  }

  /**
   * @return the name of the parent directory - assuming the directory structure is only one level
   *     deep
   */
  public static String getTestDirectoryName(String classPath) {
    String[] classPathArray = classPath.split("\\.");
    String testDirectoryName = "";
    if (classPathArray.length > 1) {
      testDirectoryName = classPathArray[classPathArray.length - 2];
    }
    return testDirectoryName;
  }

  /**
   * @return the name of the class - assuming the testname unit has "Test" appended to the
   *     respective name of the class
   */
  public String getClassName(String classPath) {
    String[] classPathArray = classPath.split("\\.");
    return classPathArray[classPathArray.length - 1].substring(
        0, classPathArray[classPathArray.length - 1].length() - 4);
  }

  protected JavaClassType getDeclaredClassSignature() {
    return identifierFactory.getClassType(getClassName(CustomTestWatcher.getCustomTestWatcher().classPath));
  }

  public JavaSootClass loadClass(ClassType clazz) {
    Optional<JavaSootClass> cs = CustomTestWatcher.getCustomTestWatcher().getJavaView().getClass(clazz);
    assertTrue(cs.isPresent(), "No matching class signature found");
    return cs.get();
  }

  public JavaSootMethod loadMethod(MethodSignature methodSignature) {
    JavaSootClass clazz = loadClass(methodSignature.getDeclClassType());
    Optional<JavaSootMethod> m = clazz.getMethod(methodSignature.getSubSignature());
    assertTrue(m.isPresent(), "No matching method signature found");
    return m.get();
  }

  public void assertJimpleStmts(SootMethod method, List<String> expectedStmts) {
    Body body = method.getBody();
    assertNotNull(body);
    List<String> actualStmts = Utils.bodyStmtsAsStrings(body);
    if (!expectedStmts.equals(actualStmts)) {
      System.out.println(Utils.generateJimpleTest(actualStmts));
      assertEquals(expectedStmts, actualStmts);
    }
  }

  public List<String> expectedBodyStmts(String... jimpleLines) {
    return Stream.of(jimpleLines).collect(Collectors.toList());
  }
}
