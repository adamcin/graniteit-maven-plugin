package net.adamcin.graniteit.mojo

import net.adamcin.graniteit.DetectsSlingJunitDependency

/**
 * Base Sling Junit Mojo class
 * @since 0.6.0
 */
class BaseSlingJunitMojo
  extends BaseITMojo
  with DetectsSlingJunitDependency {

  override def skipWithTestsOrExecute(skip: Boolean)(body: => Unit) {
    if (slingJunitSupportEnabled) {
      super.skipWithTestsOrExecute(skip)(body)
    } else {
      getLog.info("Set slingJunitVersion or add an explicit Sling Junit Framework dependency to enable this goal.")
    }
  }
}
