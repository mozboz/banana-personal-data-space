package actors.supervisors


trait MessageHandler {
  def throwExFromMessage(m:Message, additional:String = "") {
    throw new Exception("Error while processing the message with Id '" + m.messageId + "', Type '" + m.getClass.getName + "': " + additional)
  }
}
