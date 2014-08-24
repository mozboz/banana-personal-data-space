package utils

import java.util
import java.util.UUID

import scala.util.continuations._


object Blubb {
  def go =
    reset {
      println("Welcome!")
      val first = ask("Please give me a number")
      val second = ask("Please enter another number")
      printf("The sum of your numbers is: %d\n", first + second)
    }

  val sessions = new util.HashMap[UUID, Int=>Unit]

  def ask(prompt: String): Int @cps[Unit] =
    shift {
      k: (Int => Unit) => {
        val id = UUID.randomUUID()
        printf("%s\nrespond with: submit(0x%x, ...)\n", prompt, id)
        sessions.put(id, k)
      }
    }
}