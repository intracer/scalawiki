package client.dto.cmd

import client.dto.cmd.query.Query

trait Parameter[+T] {
  def name: String
  def summary: String

  def pairs: Seq[(String, String)]
  def flatten: Seq[Parameter[Any]] = Seq(this)
}

abstract class EnumParameter[ARG <: EnumArg[ARG]](val name: String, val summary: String) extends Parameter[EnumArg[ARG]] {

  var allArgs: Seq[EnumArg[ARG]] = Seq.empty

  def args: Seq[EnumArg[ARG]] = Seq(arg)

  def arg: EnumArg[ARG] = args.head

  override def pairs: Seq[(String, String)] = {
    Seq(name -> args.map(_.name).mkString("|")) ++ args.flatMap(_.pairs)
  }

//  def byClass[T <: EnumArg[ARG]](clazz: Class[T]): Seq[T] = args collect {
//    case p if p.getClass == clazz => p
//  }

  def byType[X : Manifest]: Seq[X] =
    args.collect {
      case x if manifest[X].runtimeClass.isInstance(x) => x.asInstanceOf[X]
    }


  override def flatten = {
    //Seq(this) ++

//    val x = args.collect {
//      case awp: ArgWithParams[Parameter[Any], EnumArg[AnyRef]] => awp.params //.flatMap(_.flatten)
//    }

    Seq(this)
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

trait ArgWithParams[P <: Parameter[Any], T <: EnumArg[T]] extends EnumArg[T] {
  def params: Seq[P] = Seq.empty

//  def byClass[T <: P](clazz: Class[T]) = params collect {
//    case p if p.getClass == clazz => p
//  }

  def byType[X : Manifest]: Seq[X] =
    params.collect {
      case x if manifest[X].runtimeClass.isInstance(x) => x.asInstanceOf[X]
    }

 override def pairs: Seq[(String, String)] = params.flatMap(_.pairs)
}

trait EnumArg[+T <: EnumArg[T]] {
//  def param: EnumParameter[T]
  def name: String
  def summary: String

  def pairs: Seq[(String, String)] = Seq.empty
}

abstract class EnumArgument[T <: EnumArg[T]](val name: String, val summary: String) extends EnumArg[T]

trait ActionArg extends EnumArg[ActionArg] { /*val param = ActionParam*/ }
case class ActionParam(override val arg: ActionArg) extends EnumParameter[ActionArg]("action", "") {
  def query: Option[Query] = byType(manifest[Query]).headOption
}






