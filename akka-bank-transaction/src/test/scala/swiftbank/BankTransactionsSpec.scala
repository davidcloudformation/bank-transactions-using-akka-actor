package swiftbank
import java.util.UUID

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, FSM, Props}
import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import swiftbank.AccountSupervisorActor.{AccountCreateCmd, AccountRouteRequest}
import swiftbank.TransactionActor.{BankId, Transaction}

import akka.pattern.ask
import akka.pattern.pipe

import akka.util.Timeout

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success}

class BankTransactionsSpec extends TestKit(ActorSystem("AskSpec"))
  with ImplicitSender with WordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A create account scenario" should {
    createAccountTestSuite(Props[AccountSupervisorActor],"supervisorCreateAccount")
  }

  def createAccountTestSuite(props: Props, name:String):Unit = {
        "Create one user scenario" in {
          val supervisor = system.actorOf(props,name)
          val userName = "David"
          val startingBalance  = 100.00
          val command = AccountCreateCmd(userName,startingBalance)
          supervisor ! command
          expectMsg("Created Account")
        }

  }

  "A bank transactions scenario" should {
    bankTransactionTestSuite(Props[AccountSupervisorActor],"supervisorBankTransactions")
  }

  def bankTransactionTestSuite(props: Props, name:String):Unit = {
    "Create two users scenario and deposit money scenario" in {
      val supervisor = system.actorOf(props,name)
      val userName1 = "David"
      val startingBalance1  = 100.00
      val command = AccountCreateCmd(userName1,startingBalance1)
      supervisor ! command

      val userName2 = "Dante"
      val startingBalance2  = 100.00
      val command2 = AccountCreateCmd(userName2,startingBalance2)
      supervisor ! command2

      /// transfer money from David to Dante
      val transaction = Transaction(UUID.randomUUID(),BankId(1),BankId(2),12)

      import scala.concurrent.duration._
      implicit val timeout: Timeout = Timeout(10.seconds)
      implicit val executionContext: ExecutionContext = ExecutionContext.global
      val routeRequest = (supervisor ? AccountRouteRequest(BankId(1))).mapTo[ActorRef]

      routeRequest onComplete{
        case Success(actorRef) =>
          println(s"Transaction $transaction to actor ref $actorRef")
          actorRef ! transaction
        case Failure(exception) => println(s"Transaction $transaction has failed with this $exception")
      }
    }

  }

}