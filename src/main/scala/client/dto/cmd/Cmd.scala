package client.dto.cmd

trait Parameter {
  def name: String
  def summary: String
}

case class EnumParameter[ARG <: EnumArg[ARG]](name: String, summary: String) extends Parameter {

  var allArgs: Seq[EnumArg[ARG]] = Seq.empty

  var args: Seq[EnumArg[ARG]] = Seq.empty

  def apply(args: ARG*): this.type = {
    this.args = args
    this
  }

}

trait EnumArg[T <: EnumArg[T]] {
  def param: EnumParameter[T]
  def name: String
  def summary: String
}

abstract class EnumArgument[T <: EnumArg[T]](val name: String, val summary: String) extends EnumArg[T] {
  var params: Seq[Parameter] = Seq.empty
}

object PropParam extends EnumParameter[PropArg]("prop", "")
object ListParam extends EnumParameter[ListArg]("list", "")
object MetaParam extends EnumParameter[MetaArg]("meta", "")


trait PropArg extends EnumArg[PropArg] { val param = PropParam }
trait ListArg extends EnumArg[ListArg] { val param = ListParam }
trait MetaArg extends EnumArg[MetaArg] { val param = MetaParam }

object Revisions extends  EnumArgument[PropArg]("revisions", "Get revision information.") with PropArg

trait InfoParam extends Parameter
object Info extends EnumArgument[PropArg]("info", "Get basic page information.") with PropArg {
  def apply(params: InfoParam*) = {
    this.params ++= params
    this
  }
}

object InProp extends EnumParameter[InPropArg]("inprop", "") with InfoParam
trait InPropArg extends EnumArg[InPropArg] { val param = InProp }

object Protection extends EnumArgument[InPropArg]("protection", "List the protection level of each page.") with InPropArg
object TalkId extends EnumArgument[InPropArg]("talkid", "The page ID of the talk page for each non-talk page.") with InPropArg
object Watched extends EnumArgument[InPropArg]("watched", "List the watched status of each page.") with InPropArg
object Watchers extends EnumArgument[InPropArg]("watchers", "The page ID of the talk page for each non-talk page.") with InPropArg
object NotificationTimestamp extends EnumArgument[InPropArg]("notificationtimestamp", "The watchlist notification timestamp of each page.") with InPropArg
object SubjectId extends EnumArgument[InPropArg]("subjectid", "The page ID of the parent page for each talk page.") with InPropArg
object Url extends EnumArgument[InPropArg]("url", "Gives a full URL, an edit URL, and the canonical URL for each page.") with InPropArg
object Readable extends EnumArgument[InPropArg]("readable", "Whether the user can read this page.") with InPropArg
object Preload extends EnumArgument[InPropArg]("preload", "Gives the text returned by EditFormPreloadText.") with InPropArg
object DisplayTitle extends EnumArgument[InPropArg]("displaytitle", "Gives the way the page title is actually displayed.") with InPropArg

object QueryTest {

  def main(args: Array[String]) {

    val prop = PropParam(Info(InProp(SubjectId)), Revisions)

    println(prop)
  }
}




