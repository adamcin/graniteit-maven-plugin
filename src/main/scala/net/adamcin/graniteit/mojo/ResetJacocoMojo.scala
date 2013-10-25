package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{LifecyclePhase, Mojo}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}

/**
 * Created with IntelliJ IDEA.
 * User: madamcin
 * Date: 10/25/13
 * Time: 3:24 PM
 * To change this template use File | Settings | File Templates.
 */
@Mojo(name = "reset-jacoco",
  defaultPhase = LifecyclePhase.INTEGRATION_TEST,
  threadSafe = true)
class ResetJacocoMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {



}
