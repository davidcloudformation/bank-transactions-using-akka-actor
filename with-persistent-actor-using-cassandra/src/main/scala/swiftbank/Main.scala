package swiftbank

import java.util.UUID

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

//////
import swiftbank.AccountSupervisorActor.{AccountCreateCmd, AccountRouteRequest}
import swiftbank.TransactionActor.{BankId, Transaction}


object Main extends {
  def main(args: Array[String]): Unit = {
    val system = ActorSystem("AkkaSystem")
    val supervisor = system.actorOf(Props[AccountSupervisorActor],"supervisor")

    while(true){
      println("Type 1 create actor , 2 create transaction")
      println(" OR cntrl+c to exit .....")
      println()
      val option:Int = scala.io.StdIn.readInt()

      if(option ==1){ // 1 create User
        println("Enter name")
        val name :String = scala.io.StdIn.readLine()
        println("Enter money")
        val money:Double = scala.io.StdIn.readDouble()
        val command = AccountCreateCmd(name,money)
        supervisor ! command

      }else{ // 2 create Transaction
        println("Please input bank id number for sender.")
        val senderId:Int = scala.io.StdIn.readInt()
        println("Please input bank id number for receiver.")
        val receiverId:Int = scala.io.StdIn.readInt()
        println("Please input money to send.")
        val money:Double = scala.io.StdIn.readDouble()
        val transaction = Transaction(UUID.randomUUID(),BankId(senderId),BankId(receiverId),money)
        import scala.concurrent.duration._

        implicit val timeout: Timeout = Timeout(10.seconds)
        implicit val executionContext: ExecutionContext = ExecutionContext.global

        val routeRequest = (supervisor ? AccountRouteRequest(BankId(senderId))).mapTo[ActorRef]

        routeRequest.onComplete{
          case Success(actorref) =>
            println(s"Transaction $transaction to actor ref $actorref")
            actorref ! transaction
          case Failure(exception) => println(s"Transaction $transaction has failed with this $exception")
        }

      }

    }
  }
}
