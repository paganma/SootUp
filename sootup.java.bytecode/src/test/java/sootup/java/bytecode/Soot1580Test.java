package sootup.java.bytecode;

import categories.Java8Test;
import java.util.Collections;

import categories.TestCategories;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootMethod;
import sootup.core.types.ClassType;
import sootup.java.bytecode.inputlocation.BytecodeClassLoadingOptions;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.views.JavaView;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag(TestCategories.JAVA_8_CATEGORY)
public class Soot1580Test {
  final String jar = "../shared-test-resources/soot-1580/jpush-android_v3.0.5.jar";

  @Test
  @Disabled("Localsplitter fails; bytecode itself is somehow strange")
  public void test() {
    AnalysisInputLocation inputLocation =
        new JavaClassPathAnalysisInputLocation(
            jar, null, BytecodeClassLoadingOptions.Default.getBodyInterceptors());

    JavaView view = new JavaView(Collections.singletonList(inputLocation));

    assertEquals(91, view.getClasses().size());

    ClassType clazzType =
        JavaIdentifierFactory.getInstance().getClassType("cn.jpush.android.data.f");

    assertTrue(view.getClass(clazzType).isPresent());

    view.getClass(clazzType).get().getMethods().forEach(SootMethod::getBody);
  }
}
