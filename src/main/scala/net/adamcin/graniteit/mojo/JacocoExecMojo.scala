package net.adamcin.graniteit.mojo

import org.apache.maven.plugins.annotations.{ResolutionScope, Mojo, LifecyclePhase, Parameter}
import net.adamcin.graniteit.{SlingJacocoParameters, HttpParameters}

@Mojo(name = "jacoco-exec",
  defaultPhase = LifecyclePhase.POST_INTEGRATION_TEST,
  threadSafe = true)
class JacocoExecMojo
  extends BaseSlingJunitMojo
  with HttpParameters
  with SlingJacocoParameters {

  @Parameter(property = "graniteit.skip.jacoco-exec")
  val skip = false

  override def execute(): Unit = {
    skipWithTestsOrExecute(skip) {

    }
  }
}
