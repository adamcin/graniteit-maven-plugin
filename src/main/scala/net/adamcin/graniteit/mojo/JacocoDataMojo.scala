/*
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 *
 * For more information, please refer to <http://unlicense.org/>
 */

package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{Mojo, LifecyclePhase, Parameter}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}
import dispatch._, Defaults._
import org.apache.maven.plugin.MojoFailureException
import java.io.File

/**
 * Downloads the jacoco runtime data from the server using Sling JacocoServlet,
 * which was introduced in org.apache.sling.junit.core:1.0.9-SNAPSHOT
 * @since 0.8.0
 */
@Mojo(name = "jacoco-data",
  defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
  threadSafe = true)
class JacocoDataMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  /**
   * Set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.jacoco-exec")
  val skip = false

  /**
   * Specify the filename of the downloaded Jacoco exec data
   */
  @Parameter(defaultValue = "${project.build.directory}/remote-jacoco.exec")
  var jacocoDataFile: File = null

  /**
   * Specify the classifier used when attaching the jacoco data artifact to
   * the project
   */
  @Parameter(defaultValue = "jacoco-data")
  val jacocoDataClassifier: String = null

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

      ifJacocoAvailable {
        if (jacocoDataFile.exists) {
          jacocoDataFile.delete()
        }

        val req = urlForPath(jacocoExecPath)
        expedite(req, Http(req > as.File(jacocoDataFile)).either)._2 match {
          case Right(resp) => {
            val jacocoExecArtifact = repositorySystem.createArtifactWithClassifier(
              project.getGroupId,
              project.getArtifactId,
              project.getVersion,
              "exec",
              jacocoDataClassifier
            )
            jacocoExecArtifact.setFile(jacocoDataFile)
            project.addAttachedArtifact(jacocoExecArtifact)
            getLog.info("Jacoco Runtime Data successfully retrieved.")
          }
          case Left(ex: Throwable) => {
            jacocoDataFile.delete()
            if (failOnJacocoError) {
              throw new MojoFailureException("Failed to retrieve Jacoco runtime data. " + ex.getMessage)
            } else {
              getLog.warn("Failed to retrieve Jacoco runtime data. " + ex.getMessage)
            }
          }
        }
      }
    }
  }
}
