import akka.actor._
import akka.routing.RoundRobinRouter
import scala.collection.mutable.ArrayBuffer
import java.util.UUID
import java.net.InetAddress
import com.typesafe.config.ConfigFactory
import scala.util.Random
import scala.util.control._
import scala.collection.mutable.ArrayBuffer
import java.security.MessageDigest

sealed trait BitCoin
case class BitCoinMaster(NumberOfZeroes:Int) extends BitCoin
case class BitCoinWorker(stringlen: Int,numberOfZeroes :Int) extends BitCoin
case class ReplyToMaster(output: ArrayBuffer[String] ) extends BitCoin
case class PrintResult(input: ArrayBuffer[String]) extends BitCoin
case class LocalMessage(Buffer:ArrayBuffer[String]) extends BitCoin
case class RemoteMessage(Buffer:ArrayBuffer[String]) extends BitCoin
case class AllocateWork(NoOfZeroes:Int) extends BitCoin


object BitCoinServer {
  def main(args: Array[String]) {
   
    if(args.length==2)
   { 
   //set the variables in config factory using the arguments
    val configfactory = ConfigFactory.parseString(
      """ 
     akka{ 
    		actor{ 
    			provider = "akka.remote.RemoteActorRefProvider" 
    		} 
    		remote{ 
                enabled-transports = ["akka.remote.netty.tcp"] 
            netty.tcp{ 
    			hostname = """+args(0)+""" 
    			port = 5150 
    		} 
      }      
    }""")
    
    
   //get the number of zeroes value from 2nd argument
   val NoOfZeroes = Integer.parseInt(args(1))
   val system = ActorSystem("BitCoinServer", ConfigFactory.load(configfactory))
   
   val AssignWorkActor = system.actorOf(Props(new AssignWorkActor(NoOfZeroes)), name = "AssignWorkActor")
   
   //start the Assign work actor which assigns work to a client
   AssignWorkActor ! "STARTSERVER"
   
   //create a listener which prints server
   val listener = system.actorOf(Props[Listener], name = "listener")

    // create the master
   val master = system.actorOf(Props(new Master(2, 25,NoOfZeroes, listener)), name = "master")

    // start the calculation
   master ! BitCoinMaster
   
   }
   else
   {
     println("exact two arguemnts are Required . First Argument is the ip address of the server , second argument is the number of starting zeroes in a Bitcoin")
     println("Program is terminating Now")
     System.exit(0)
 
   }
    
  }
}


class Master(WorkersCount: Int, MessageCount: Int,NumberOfZeros:Int, listener: ActorRef) extends Actor {
  //ArrayBuffer to store results
  var result: ArrayBuffer[String] = new ArrayBuffer[String]()
  var nrOfResults: Int = _

  val workerRouter = context.actorOf(
    Props[Worker].withRouter(RoundRobinRouter(WorkersCount)), name = "workerRouter")

  def receive = {
	 
    //Start the worker
    case BitCoinMaster =>
      for (i <- 0 until MessageCount) workerRouter ! BitCoinWorker(11 + i % 10,NumberOfZeros)
    //Sent the result to listener
    case ReplyToMaster(output) =>
      result ++= output
      nrOfResults += 1
      if (nrOfResults == MessageCount) {
        listener ! PrintResult(result)
        println("Server completed its work")
        
      }
  }

}

//The actor which assigns work to the Client
class AssignWorkActor(NumberOfZeroes : Int) extends Actor {
  def receive = {
    case "AssignWorkToMe" => {
      sender ! AllocateWork(NumberOfZeroes)
    }
    //Receive Message from client
    case RemoteMessage(buffer) => {
      println("Bitcoins Recevied from client "+buffer.length)
      for (i <- 0 until buffer.length) {
        println(buffer(i))
      }
    }
    case "STARTSERVER" =>{
      println("Server Started")
    }
    case _ => {
      println("Unknown Message Recevied at Server")
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

class Listener extends Actor {
  def receive = {
    case PrintResult(input) =>
      {
       
    	  println("printing the Bit Coins generated by the Server")
    	  //print the results
    	  for(i <- 0 until input.length){
           println(input(i))
        }  
      }
  }
}
