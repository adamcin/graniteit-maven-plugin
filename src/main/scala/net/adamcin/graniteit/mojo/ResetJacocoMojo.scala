package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{Parameter, LifecyclePhase, Mojo}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}

@Mojo(name = "reset-jacoco",
  defaultPhase = LifecyclePhase.PRE_INTEGRATION_TEST,
  threadSafe = true)
class ResetJacocoMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  /**
   * set to true to specifically disable this goal
   */
  @Parameter(property = "graniteit.skip.reset-jacoco")
  val skip = false

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

    }
  }
}
