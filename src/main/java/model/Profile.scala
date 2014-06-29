package model

import collection.mutable
import app.ProfileStorage

class Profile(name: String, storage: ProfileStorage) {

  /**
   * Get all contexts
   * @return
   */
  /*
  def getContexts: mutable.HashMap[String, Context] = {
    if (contexts == null) {
      contexts = new mutable.HashMap[String, Context]
    }
    return this.contexts
  }
    */
  /**
   * Get one context by name
   * @return
   */
  def getContext(contextName: String): ImmutableContext = {

    if (!contextExists(contextName)) {
      throw new Exception("contextName name " + name + " not found")
    }

    new ImmutableContext(contextName, name, contexts(contextName))
  }

  def createContext(contextName: String) = {
    val c: Context = new Context(contextName, name, storage)
    contexts.put(contextName, c)
  }

  def saveToJson(fileName: String) {
  }

  def loadFromJson(fileName: String) {
  }

  def addContentItem(context: ImmutableContext, itemName: String, itemValue: String) {
    addContentItem(context.key, itemName, itemValue)
  }

  def addContentItem(contextName: String, itemName: String, itemValue: String) {
    if (!contextExists(contextName)) {
      throw new Exception("Context " + contextName + " does not exist")
    }
    contexts(contextName).setDataItem(itemName, itemValue)
  }

  def getContentItem(contextName: String, itemName: String): String = {
    if (!contextExists(contextName)) {
      throw new Exception("Context " + contextName + " does not exist")
    }
    contexts(contextName).getDataItem(itemName)
  }

  def contextExists(contextName: String): Boolean = contexts.contains(contextName)

  def contentItemExists(contextName: String, contentItemName: String): Boolean = {
    if (!contextExists(contextName)) {
      throw new Exception("Context " + contextName + " does not exist")
    }
    contexts(contextName).itemExists(contentItemName)
  }

  val contexts = new mutable.HashMap[String, Context]()
}