package com.knowledgebase.context

import com.knowledgebase.clients.KnowledgeBaseThriftClientComponent

class ComponentProvider extends KnowledgeBaseThriftClientComponent {
  lazy val knowledgeBaseThriftClient = new KnowledgeBaseThriftClient("localhost", 8080)
}
