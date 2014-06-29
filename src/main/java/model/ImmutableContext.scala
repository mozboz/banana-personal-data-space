package model

import javax.xml.bind.annotation._
import collection.mutable
import collection.parallel.immutable
import collection.immutable.HashSet

class ImmutableContext(val key: String, originProfile: String, context: Context) {

  val aggregatedProfiles: HashSet[String] = HashSet[String]() ++ context.aggregatedProfiles

  val dataKeys: HashSet[String] = HashSet[String]() ++ context.data.keySet
  // var data = new mutable.HashMap[String, String]() =

  def containsKey(key: String) = dataKeys.contains(key)


}