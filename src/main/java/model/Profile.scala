package model

import collection.mutable

class Profile(name: String) {

  /**
   * Get all contexts
   * @return
   */
  def getContexts: mutable.HashMap[String, Context] = {
    if (contexts == null) {
      contexts = new mutable.HashMap[String, Context]
    }
    return this.contexts
  }

  /**
   * Get one context by name
   * @return
   */
  def getContextMetaData(name: String): ContextMetaData = {
    /*
    if (!contexts.contains(name)) {
      throw new Exception("contextName name " + name + " not found")
    }

    this.contexts(n ame)
    */
    new ContextMetaData()
  }

  def createContext(name: String) = {
    val c: Context = new Context(name)
    contexts.put(name, c)
  }

  def saveToJson(fileName: String) {
  }

  def loadFromJson(fileName: String) {
  }

  def addContextItem(contextName: String, itemName: String, itemValue: String) {
    if (!contextExists(contextName)) {
      throw new Exception("Context " + contextName + " does not exist")
    }
    contexts(contextName).setDataItem(itemName, itemValue)
  }

  def getContextItem(contextName: String, itemName: String) {
    if (!contextExists(contextName)) {
      throw new Exception("Context " + contextName + " does not exist")
    }
    contexts(contextName).getDataItem(itemName)
  }

  def contextExists(contextName: String): Boolean = contexts.contains(contextName)

  var contexts: mutable.HashMap[String, Context] = new mutable.HashMap[String, Context]()
}