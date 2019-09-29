package de.upb.soot.core.frontend;

/*-
 * #%L
 * Soot
 * %%
 * Copyright (C) 22.05.2018 Manuel Benz
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 *
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import de.upb.soot.core.inputlocation.AnalysisInputLocation;
import de.upb.soot.core.inputlocation.FileType;
import de.upb.soot.core.types.JavaClassType;
import java.nio.file.Path;

/**
 * Responsible for creating {@link ClassSource}es based on the handled file type (.class, .jimple,
 * .java, .dex, etc).
 *
 * @author Manuel Benz
 */
public interface ClassProvider {

  AbstractClassSource createClassSource(
      AnalysisInputLocation srcNamespace, Path sourcePath, JavaClassType classSignature);

  /** Returns the file type that is handled by this provider, e.g. class, jimple, java */
  FileType getHandledFileType();
}
