# non-blocking-server

Just a little test. How could be a non-blocking server.

Some interesting ideas from https://jenkov.com/tutorials/java-nio/non-blocking-server.html

An example of client for this server is 

Connection protocol:

Client                                                        Server
------                                                        ------
                        Connect
          --------------------------------------------------> Server creates a welcome message
                Public key + client socket Id in a WELCOME message
          <-------------------------------------------------
client generates
SecretKey (private) with
client socket Id, random
integer and shared client-server 
integer.  

Client sends SECRET: generated random and SecretKey in byte[]
               encrypted with public key.
           -------------------------------------------------> Server calculates MAC (key for
                                                              MAC with: server socket Id + 
                                                              shared client-server integer +
                                                              random client integer.
All exchanged messages now will be sent with stablished client private key and the MAC will be check.
             Server sends calculated MAC (CHECK_WELCOME message)
          <-------------------------------------------------
Client compares
his MAC with received MAC.
If it fails, stops connection.

More interaction requires loggin:
Client sends User name + password with LOG_IN or NEW_USER message.
           ------------------------------------------------->
             Server sends ACK or NACK and the client is logged or not.
          <-------------------------------------------------
                                                              
