package org.scalawiki.dto.cmd.query.prop

import org.scalawiki.dto.cmd._
import org.scalawiki.dto.cmd.query.Module

/**
 * ?action=query&amp;prop=images
 *
 */
case class Images(override val params: ImagesParam[Any]*)
  extends Module[PropArg]("im", "images", "Gets a list of all images used on the provided pages.")
  with PropArg with ArgWithParams[ImagesParam[Any], PropArg]

/**
 * Marker trait for parameters used with prop=langlinks
 */
trait ImagesParam[+T] extends Parameter[T]

case class ImLimit(override val arg: String) extends StringParameter("imlimit",
  "How many images to return") with ImagesParam[String]


///**
// *  ?action=query&amp;prop=info&amp;llprop=
// *
// */
//case class LlProp(override val args: LlPropArg*) extends EnumParameter[LlPropArg]("llprop", "Which additional properties to get:") with LangLinksParam
//
//
///**
// *  Trait for inprop= arguments
// *
// */
//
//trait LlPropArg extends EnumArg[LlPropArg] { val param = InProp }
//
///**
// *  inprop= arguments
// *
// */
//object LlUrl extends EnumArgument[LlPropArg]("url", "Whether to get the full URL.") with LlPropArg
//
//
//ge links for 50 titles
//Parameters[edit]
//lllimit: How many langlinks to return. Default: 10. No more than 500 (5000 for bots) allowed. MW 1.13+
//llcontinue: When more results are available, use this to continue MW 1.13+
//llurl: Whether to get the full URL MW 1.17+
//llprop: Which additional properties to get for each interlanguage link MW 1.23+
//url: Adds the full URL
//langname: Adds the localised language name (best effort, use CLDR extension). Use llinlanguagecode to control the language
//autonym: Adds the native language name",
//lllang: Language code MW 1.18+
//lltitle: Link to search for. Must be used with lllang MW 1.18+
//lldir: The direction in which to list MW 1.19+
//llinlanguagecode: Language code for localised language names MW 1.23+