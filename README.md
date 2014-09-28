Distributed_BitCoin_Server-Client-Implementation
====================================

Implementing BitCoin type Distributed Server client Implementation Using Scala.


Explaining the Server Client Model :
==================================
The Server is the master which assigns the work to its clients , When the Client is ready it sends a message and gets work from the server ,completes the work and sends back the results to server . This kind of Distributed server client architecture is implemented in BitCoins . Servers assign works to Client which send back the results after performing the assignmed task .

In this implementation i have used <b>Akka framework</b> Which makes parallel computing very fast . The Work assigned by the server to the client is to return the strings whose sha-256 hashcode starts with a number of zeroes mentioned by the server .

Starting the Server :
====================

Compliation: scalac BitCoinServer.scala 
Execution : scala BitCoinServer ip-address number

Here ip adreess is the ip address of the Machini on which the server is running.

Example : scala BitCoinServer 192.100.156.20 5
the server starts running on the machine and it mines for the hashcode of the string which start with 5 zeroes . If any Client pings the server its assign work to the client and client return the results.


Starting the Client :
=====================
Compilation : scalac BitCoinClient.scala 
Execution : scala BitCoinClient server-ip-adrress

Example : scala BitCoinClient 192.100.156.20

The Client sends a message to a server which is running on 192.100.156.20 . The server asks client to give the string whose sha-256 hash code has 5 zeroes .The clients completes the task and send back the result to the server. 
