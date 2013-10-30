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
