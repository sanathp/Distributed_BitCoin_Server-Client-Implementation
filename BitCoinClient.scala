import akka.actor._
import com.typesafe.config.ConfigFactory
import java.net.InetAddress
import scala.collection.mutable.ArrayBuffer
import scala.util.control.Breaks
import scala.util.Random
import java.util.UUID
import akka.routing.RoundRobinRouter
import java.security.MessageDigest


sealed trait BitCoin
case class BitCoinMaster(NumberOfZeroes:Int) extends BitCoin
case class BitCoinWorker(stringlen: Int,numberOfZeroes :Int) extends BitCoin
case class ReplyToMaster(output: ArrayBuffer[String] ) extends BitCoin
case class PrintResult(input: ArrayBuffer[String]) extends BitCoin
case class LocalMessage(Buffer:ArrayBuffer[String]) extends BitCoin
case class RemoteMessage(Buffer:ArrayBuffer[String]) extends BitCoin
case class AllocateWork(NoOfZeroes:Int) extends BitCoin


object BitCoinClient {

  def main(args: Array[String]) {
    
    //set the configuration
    val config = ConfigFactory.parseString(
      """akka{
		  		actor{
		  			provider = "akka.remote.RemoteActorRefProvider"
		  		}
		  		remote{
                   enabled-transports = ["akka.remote.netty.tcp"]
		  			netty.tcp{
						hostname = "127.0.0.1"
						port = 0
					}
				}     
    	}""")

    implicit val system = ActorSystem("ClientSystem", ConfigFactory.load(config))
    val SendResultActor = system.actorOf(Props(new SendResultActor(args(0))), name = "SendResultActor")
   
    val listener = system.actorOf(Props(new Listener(SendResultActor)), name = "listener")
    val master =  system.actorOf(Props(new Master(2,25, listener)),name = "master")
    
    val GetWorkActor = system.actorOf(Props(new GetWorkActor(args(0),master)), name = "GetWorkActor") 
    
    //get the work from the server
     GetWorkActor ! "GETWORKFROMSERVER" 
    
     
  }
}
 //the actor which gets work from the server
class GetWorkActor(ip: String , master:ActorRef) extends Actor {
 
  val server = context.actorFor("akka.tcp://BitCoinServer@" + ip + ":5150/user/AssignWorkActor")

  def receive = {
    case "GETWORKFROMSERVER" =>
      print("s")
       server ! "AssignWorkToMe"
    case AllocateWork(noOfZeroes) =>
       println("Received Work from server to generate Bitcoins which start with " + noOfZeroes +" Zeroes")
       master ! BitCoinMaster(noOfZeroes)
  }
}

//the actor which sends results to the server after completing work
class SendResultActor(ip: String) extends Actor {
  println("akka.tcp://BitCoinServer@" + ip + ":5150/user/AssignWorkActor")
  // create the remote actor
  val remote = context.actorFor("akka.tcp://BitCoinServer@" + ip + ":5150/user/AssignWorkActor")

  def receive = {
    case LocalMessage(input) =>
       remote ! RemoteMessage(input)
       println("Sent Result to Server") 
       //close the system after sending result to server
      context.system.shutdown()
    case _ =>
      println("Unknown message received")
  }
}

//Master
class Master(WorkersCount: Int, MessageCount: Int, listener: ActorRef) extends Actor {

  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var nrOfResults: Int = _
  val start: Long = System.currentTimeMillis

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")

  def receive = {

    case BitCoinMaster(numberOfZeros) =>
      for (i <- 0 until MessageCount) workerRouter ! BitCoinWorker(10 + i % 10,numberOfZeros)
    case ReplyToMaster(output) =>
      result ++= output
      nrOfResults += 1
      if (nrOfResults == MessageCount) {
        // Send the result to the listener
        listener ! PrintResult(result)
        
        context.stop(self)
      }
  }

}

class Worker extends Actor {

  def receive = {

    case BitCoinWorker(stringlen,numberOfZeroes) => sender ! {
      ReplyToMaster(mineBitCoins(stringlen,numberOfZeroes))
    }

  }
 
  //A Function which  generates a alphanumeric
 def randomAlphaNumericStringGenerator(length: Int): String = {
    val chars = ('a' to 'z') ++ ('A' to 'Z') ++ ('0' to '9')
    randomStringGeneratorFromGivenList(length, chars)
  }
 
 //A Function for random String Generator
  def randomStringGenerator(length: Int) = {
    val r = new scala.util.Random
    val sb = new StringBuilder
    for (i <- 1 to length) {
      sb.append(r.nextPrintableChar)
    }
    sb.toString
  }
 

 def randomStringGeneratorFromGivenList(length: Int, chars: Seq[Char]): String = {
    val sb = new StringBuilder
    for (i <- 1 to length) {
      val randomNum = util.Random.nextInt(chars.length)
      sb.append(chars(randomNum))
    }
    sb.toString
  }
 
  //The mine function which mines for the bitcoins
  def mineBitCoins(stringlen: Int,numberOfZeroes: Int): ArrayBuffer[String] = {
    
    var ShaHashOutput = new ArrayBuffer[String]() 
       
     val md: MessageDigest = MessageDigest.getInstance("SHA-256");
     val sha = MessageDigest.getInstance("SHA-256")
    
     //Function generates sha-256 hash code for a given string
     def hex_digest(s: String): String = {
    	  sha.digest(s.getBytes)
    		.foldLeft("")((s: String, b: Byte) => s +
                  Character.forDigit((b & 0xf0) >> 4, 16) +
                  Character.forDigit(b & 0x0f, 16))
      	}    
     
       for (l <- 0 until 100000) {
      
    
     //add gatorlink to all input values 
     var inputvalue: String = "sanath"
       if(l%2==0)
       {
         inputvalue+=randomAlphaNumericStringGenerator(stringlen) 
       }
       else if(l%5==0)
       {
         inputvalue+=UUID.randomUUID().toString();
       }
       else
       {
         inputvalue+=randomStringGenerator(stringlen) 
       }
     
      var ShaHashCode:String=null

        ShaHashCode= hex_digest(inputvalue)
        
        //generate a string with number of zeroes given as input
        var zeroString:String="0"
        for( a <- 2 to numberOfZeroes){
          
         zeroString=zeroString+'0';
        }
        
        //add teh input value and hash code to a string if the hashcode starts with a given number of zeros
        if(ShaHashCode.startsWith(zeroString)){
   
          ShaHashOutput += inputvalue+"    :   "+ShaHashCode
        }
    
    }
    //return the output   
    ShaHashOutput
  }
}

class Listener(SendResultActor: ActorRef) extends Actor {
  def receive = {
    case PrintResult(input) =>
      {
        println("Assigned Work Completed")
        SendResultActor ! LocalMessage(input)
      }
      
  }
}
