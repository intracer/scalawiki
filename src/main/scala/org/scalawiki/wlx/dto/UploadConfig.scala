package org.scalawiki.wlx.dto

import org.scalawiki.wlx.dto.lists.ListConfig

/**
  * @param campaign     name of upload campaign (https://commons.wikimedia.org/wiki/Commons:Upload_campaigns)
  * @param listTemplate name of template that monument lists consist of
  * @param fileTemplate name of template that marks a contest image with monument id
  * @param listConfig   configuration of monument list fields
  */
case class UploadConfig(campaign: String, listTemplate: String, fileTemplate: String, listConfig: ListConfig)