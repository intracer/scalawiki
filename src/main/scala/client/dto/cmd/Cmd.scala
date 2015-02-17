package client.dto.cmd

trait Parameter[+T] {
  def name: String
  def summary: String

  def pairs: Seq[(String, String)]
}

case class EnumParameter[ARG <: EnumArg[ARG]](name: String, summary: String) extends Parameter[EnumArg[ARG]] {

  var allArgs: Seq[EnumArg[ARG]] = Seq.empty

  var args: Seq[EnumArg[ARG]] = Seq.empty

  def apply(args: ARG*): this.type = {
    this.args = args
    this
  }

  override def pairs: Seq[(String, String)] = {
    Seq(name -> args.map(_.name).mkString("|")) ++ args.flatMap(_.pairs)
  }
}

abstract class ListParameter[T] extends Parameter[T] {

  var args: Seq[T] = Seq.empty

  def apply(args: T*): this.type = {
    this.args = args
    this
  }

  override def pairs: Seq[(String, String)] = {
    Seq(name -> args.mkString("|"))
  }
}

abstract class SingleParameter[T] extends Parameter[T] {

  var arg: T = _

  def apply(arg: T): this.type = {
    this.arg = arg
    this
  }

  override def pairs: Seq[(String, String)] = {
    Seq(name -> arg.toString)
  }
}

case class StringListParameter(name: String, summary: String) extends ListParameter[String]
case class IntListParameter(name: String, summary: String) extends ListParameter[Int]

case class StringParameter(name: String, summary: String) extends SingleParameter[String]
case class IntParameter(name: String, summary: String) extends SingleParameter[Int]

trait ArgWithParams[P <: Parameter[AnyRef], T <: EnumArg[T]] extends EnumArg[T] {
  var params: Seq[P] = Seq.empty

  def apply(params: P*):this.type = {
    this.params ++= params
    this
  }

 override def pairs: Seq[(String, String)] = params.flatMap(_.pairs)
}

trait EnumArg[T <: EnumArg[T]] {
  def param: EnumParameter[T]
  def name: String
  def summary: String

  def pairs: Seq[(String, String)] = Seq.empty
}

abstract class EnumArgument[T <: EnumArg[T]](val name: String, val summary: String) extends EnumArg[T]

trait ActionArg extends EnumArg[ActionArg] { val param = ActionParam }
object ActionParam extends EnumParameter[ActionArg]("action", "")






