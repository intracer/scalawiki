package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.lists.ListConfig

case class UploadConfig(campaign: String, listTemplate: String, fileTemplate: String, listConfig: ListConfig) {

}
