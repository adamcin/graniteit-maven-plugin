package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{ResolutionScope, Mojo, LifecyclePhase, Parameter}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 10/25/13
 * Time: 3:26 PM
 * To change this template use File | Settings | File Templates.
 */
@Mojo(name = "jacoco-exec",
  defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
  threadSafe = true)
class JacocoExecMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  @Parameter(property = "graniteit.skip.jacoco-exec")
  val skip = false

}
