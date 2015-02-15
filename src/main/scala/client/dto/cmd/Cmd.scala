package client.dto.cmd

import client.dto.cmd.query.prop._

trait Parameter {
  def name: String
  def summary: String

  def pairs: Seq[(String, String)]
}

case class EnumParameter[ARG <: EnumArg[ARG]](name: String, summary: String) extends Parameter {

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

trait ArgWithParams[P <: Parameter, T <: EnumArg[T]] extends EnumArg[T] {
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

object ListParam extends EnumParameter[ListArg]("list", "")
object MetaParam extends EnumParameter[MetaArg]("meta", "")

trait ListArg extends EnumArg[ListArg] { val param = ListParam }
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }

object QueryTest {


  def main(args: Array[String]) {

    val prop = PropParam(Info(InProp(SubjectId)), Revisions)

    println(prop.pairs)
  }
}




