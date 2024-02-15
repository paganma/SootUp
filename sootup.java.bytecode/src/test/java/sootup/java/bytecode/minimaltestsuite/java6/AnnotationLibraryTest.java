package sootup.java.bytecode.minimaltestsuite.java6;


import categories.Java8Test;
import java.io.PrintWriter;
import java.io.StringWriter;

import categories.TestCategories;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import sootup.core.model.ClassModifier;
import sootup.core.model.SootClass;
import sootup.core.util.printer.JimplePrinter;
import sootup.java.bytecode.minimaltestsuite.MinimalBytecodeTestSuiteBase;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** @author Kaustubh Kelkar */
@Tag(TestCategories.JAVA_8_CATEGORY)
public class AnnotationLibraryTest extends MinimalBytecodeTestSuiteBase {

  // TODO: [bh] annotation methods lose default values

  @Test
  public void testAnnotationDeclaration() {
    SootClass sootClass = loadClass(getDeclaredClassSignature());
    JimplePrinter p = new JimplePrinter(JimplePrinter.Option.LegacyMode);
    StringWriter out = new StringWriter();
    p.printTo(sootClass, new PrintWriter(out));
    assertTrue(ClassModifier.isAnnotation(sootClass.getModifiers()));
  }

  // TODO: [ms] add test for more annotation declarations e.g. inheritance

}
