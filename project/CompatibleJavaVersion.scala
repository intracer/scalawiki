import sbt.{VersionNumberCompatibility, VersionNumber}

/** Java specification version compatibility rule. */
object CompatibleJavaVersion extends VersionNumberCompatibility {
  def name = "Java specification compatibility"

  def isCompatible(current: VersionNumber, required: VersionNumber) =
    current.numbers.zip(required.numbers).forall(n => n._1 >= n._2)

  def apply(current: VersionNumber, required: VersionNumber) =
    isCompatible(current, required)

}
