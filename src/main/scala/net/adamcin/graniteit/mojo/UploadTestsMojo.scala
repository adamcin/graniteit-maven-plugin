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

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.{ResolutionScope, Parameter, Mojo, LifecyclePhase}
import scala.Left
import scala.Right
import java.util.Collections
import scala.collection.JavaConverters._
import scalax.io.Resource
import net.adamcin.graniteit._
import scala.Some

/**
 * Upload integration test dependencies, including content-packages and OSGi bundles,
 * to the configured integration test server.
 * @since 0.6.0
 */
@Mojo(name = "upload-tests",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  requiresDependencyResolution = ResolutionScope.TEST,
  threadSafe = true)
class UploadTestsMojo
  extends BaseITMojo
  with RequiresProject
  with ResolvesArtifacts
  with OutputParameters
  with UploadsPackages
  with UploadsBundles {

  /**
   * Set to true to skip execution of this mojo
   */
  @Parameter(property = "graniteit.skip.upload-tests")
  val skip = false

  /**
   * List of artifactIds matching test package dependencies
   */
  @Parameter
  var testPackages = Collections.emptyList[String]

  def testPackageArtifacts = resolveByArtifactIds(testPackages.asScala.toSet)

  /**
   * List of artifactIds matching OSGi bundle dependencies
   */
  @Parameter
  var testBundles = Collections.emptyList[String]

  def testBundleArtifacts = resolveByArtifactIds(testBundles.asScala.toSet)

  lazy val uploadTestsChecksum = {
    val calc = new ChecksumCalculator
    testPackages.asScala.foreach { calc.add }
    testBundles.asScala.foreach { calc.add }
    calc.calculate()
  }

  override def execute() {
    super.execute()

    skipWithTestsOrExecute(skip) {
      if (!testPackages.isEmpty) {
        getLog.info("uploading test packages...")

        testPackageArtifacts.foreach(
          (packageArtifact) => {
            val shouldForce = force || inputFileModified(uploadTestsSha,
              List(packageArtifact.getFile))
            uploadPackageArtifact(packageArtifact)(shouldForce)
          }
        )
      }

      if (!testBundles.isEmpty) {
        val shouldForceUploadBundles = !uploadTestsSha.exists() ||
          Resource.fromFile(uploadTestsSha).string != uploadTestsChecksum

        getLog.info("uploading test bundles...")
        testBundleArtifacts.foreach {
          (artifact) => Option(artifact.getFile) match {
            case None => throw new MojoExecutionException("failed to resolve artifact: " + artifact.getId)
            case Some(bundle) => {
              if (shouldForceUploadBundles || inputFileModified(uploadTestsSha, List(bundle))) {
                uploadTestBundle(bundle) match {
                  case Left(ex: Throwable) => throw ex
                  case Right(messages) => messages.foreach { getLog.info(_) }
                }
              }
            }
          }
        }
      }

      overwriteFile(uploadTestsSha, uploadTestsChecksum)
    }
  }
}