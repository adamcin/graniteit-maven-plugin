package net.adamcin.graniteit.mojo

import net.adamcin.graniteit.HttpParameters
import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo, Parameter}
import dispatch._, Defaults._
import org.apache.maven.plugin.{MojoExecutionException, MojoFailureException}

@Mojo(name = "init-sling-junit",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  threadSafe = true)
class InitSlingJunitMojo
  extends BaseSlingJunitMojo
  with HttpParameters {

  /**
   * set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.init-sling-junit")
  val skip = false

  /**
   * Specify the parent path of the newly created sling junit test resource
   */
  @Parameter(defaultValue = "/test/sling")
  val testResourceParentPath: String = null

  /**
   * Specify the value of the sling:resourceType property on the test resource
   */
  @Parameter(defaultValue = "sling/junit/testing")
  val testResourceType: String = null

  /**
   * Specify the Sling request selectors and extension to build the servlet path.
   */
  @Parameter(defaultValue = ".junit")
  val testResourcePathInfo: String = null

  /**
   * Specify the System property which will be set with the sling junit servlet path
   * for use by integration test classes executed by the SlingAnnotationsTestRunner
   */
  @Parameter(defaultValue = "sling.junit.path")
  val slingJunitServletPathProperty: String = null

  /**
   * Set to explicity override the value of the sling junit servlet path property, such
   * as when tests should be executed by the non-Sling Server-Side Test execution servlet
   * Setting this property disables the creation of a sling junit test resource.
   */
  @Parameter
  val slingJunitServletPath: String = null

  /**
   * Number of seconds to wait while pinging the sling junit servlet for the expected response
   */
  @Parameter(defaultValue = "60")
  val slingJunitTimeout = 60

  /**
   * Set to specify the expected response content for the sling junit servlet check request
   */
  @Parameter(defaultValue = "SlingJUnitServlet")
  val slingJunitServletExpectedContent: String = null

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {
      val servletPath: String = Option(slingJunitServletPath) match {
        case None => createResource(testResourceParentPath, testResourceType) match {
          case Right(path) => path + testResourcePathInfo
          case Left(ex) => throw ex
        }
        case Some(path) => path
      }

      session.getUserProperties.setProperty(slingJunitServletPathProperty, servletPath)

      val until = System.currentTimeMillis() + (slingJunitTimeout * 1000)
      if (!waitForResponse(0)(until, () => urlForPath(servletPath).subject OK as.String, checkContent)) {
        throw new MojoFailureException("Server failed to respond as expected within " + slingJunitTimeout + " seconds.")
      }
    }
  }

  def createResource(parentPath: String, resourceType: String): Either[Throwable, String] = {
    if (!skipMkdirs) {
      mkdirs(parentPath)
    }

    val resourcePath = parentPath.replaceAll("[/]?[*]?$", "/*")

    val req = urlForPath(resourcePath) << Map(
      "./sling:resourceType" -> resourceType,
      "./groupId" -> project.getGroupId,
      "./artifactId" -> project.getArtifactId,
      "./version" -> project.getVersion
    )
    val resp = expedite(req, Http(req))._2

    if (isSlingPostSuccess(req, resp)) {
      Right(resp.getHeader("location"))
    } else {
      Left(new MojoExecutionException("Failed to create sling junit resource under path: " + parentPath))
    }
  }

  def checkContent(response: Future[String]): Future[Boolean] = {
    response.fold(
      (ex) => {
        getLog.info("server ready check exception: " + ex.getMessage)
        false
      },
      (content) => if (content contains slingJunitServletExpectedContent) {
        getLog.info("sling junit servlet ready check succeeded")
        true
      } else {
        getLog.info("server ready check failed to return expected content")
        false
      }
    )
  }
}
