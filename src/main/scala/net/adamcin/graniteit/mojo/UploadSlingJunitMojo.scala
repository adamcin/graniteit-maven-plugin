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

import org.apache.maven.plugins.annotations.{Parameter, ResolutionScope, LifecyclePhase, Mojo}
import net.adamcin.graniteit._
import org.apache.maven.plugin.MojoExecutionException
import scalax.io.Resource
import org.apache.maven.artifact.Artifact

/**
 * Upload the sling junit framework bundles necessary for SlingRemoteTest execution,
 * including: org.apache.sling.junit.core, org.apache.sling.junit.remote,
 * and org.apache.sling.junit.scriptable
 * @since 0.6.0
 */
@Mojo(name = "upload-sling-junit",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  requiresDependencyResolution = ResolutionScope.TEST,
  threadSafe = true)
class UploadSlingJunitMojo
  extends BaseSlingJunitMojo
  with OutputParameters
  with UploadsBundles {

  /**
   * Set to true to skip execution of this mojo
   */
  @Parameter(property = "graniteit.skip.upload-sling-junit")
  val skip = false

  lazy val uploadSlingJunitChecksum = {
    val calc = new ChecksumCalculator
    slingJunitArtifacts.foreach {
      (a: Artifact) => calc.add(a.getFile)
    }
    calc.calculate()
  }

  override def execute() {
    super.execute()

    skipWithTestsOrExecute(skip) {

      val shouldForceUploadBundles = !uploadSlingJunitSha.exists() ||
        Resource.fromFile(uploadSlingJunitSha).string != uploadSlingJunitChecksum

      getLog.info("uploading Sling Junit Framework bundles...")
      slingJunitArtifacts.foreach {
        (artifact) => Option(artifact.getFile) match {
          case None => throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
          case Some(bundle) => {
            if (shouldForceUploadBundles ||
              inputFileModified(uploadSlingJunitSha, List(bundle))) {
              uploadTestBundle(bundle) match {
                case Left(ex) => throw ex
                case Right(messages) => messages.foreach {
                  getLog.info(_)
                }
              }
            }
          }
        }
      }

      overwriteFile(uploadSlingJunitSha, uploadSlingJunitChecksum)
    }
  }
}
