package com.template;


import kotlin.jvm.functions.Function1;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCConnection;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.internal.InputStreamAndHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.FlowHandle;
import net.corda.core.messaging.FlowProgressHandle;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;

import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class DocumentMain {

    public static void main(String args[]){


       // OptionParser optionParser = new  OptionParser() ;

//        val host = NetworkHostAndPort("localhost", 10006)
//        println("Connecting to sender node ($host)")
//        CordaRPCClient(host).start("demo", "demo").use {
//            sender(it.proxy)
//        }


        NetworkHostAndPort host = new NetworkHostAndPort("localhost",10006) ;
        System.out.println("connecting to the sender node " +host);

        CordaRPCClient   cordaRPCClient =  new CordaRPCClient(host);
        cordaRPCClient.start("user1","test") ;
        cordaRPCClient.use("user1", "test", new Function1<CordaRPCConnection, Object>() {
            @Override
            public Object invoke(CordaRPCConnection cordaRPCConnection) {

                try {
                    rpcSender(cordaRPCConnection.getProxy(),1025);


                } catch (ExecutionException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    e.printStackTrace();
                }
                return null;
            }
        });




    }




    public static void rpcSender(CordaRPCOps rpc , Integer numOfClearBytes) throws ExecutionException, InterruptedException, NoSuchFieldException {

        if(numOfClearBytes == null || numOfClearBytes < 1024){
            numOfClearBytes = 1024 ;
        }
        byte b = 0 ;
       InputStreamAndHash inputStreamAndHash =  InputStreamAndHash.Companion.createInMemoryTestZip(numOfClearBytes,b);
       
       Executor executor = Executors.newScheduledThreadPool(2);
       
       try{
           
           
           sender(rpc,inputStreamAndHash.getInputStream(),inputStreamAndHash.getSha256(),executor);
       }finally {
           ((ScheduledExecutorService) executor).shutdown();
       }








    }




    private static void sender(CordaRPCOps rpc, InputStream inputStream, SecureHash.SHA256 hash, Executor executor) throws ExecutionException, InterruptedException, NoSuchFieldException {
//query Document state



        if(! rpc.attachmentExists(hash)){

            SecureHash id = rpc.uploadAttachment(inputStream);

            System.out.println ("added the document"+id);




            FlowProgressHandle<SignedTransaction> signedTransactionFlowHandle = rpc.startTrackedFlowDynamic(FileInitiateFlow.class,rpc.wellKnownPartyFromX500Name(CordaX500Name.parse("O=PartyB,L=New York,C=US")), "Added the doc ",id);


            FileState fileState = (FileState) signedTransactionFlowHandle.getReturnValue().get().getTx().getOutputStates().get(0);
             System.out.println("File state created "+fileState.toString());

           System.out.println("-----"+signedTransactionFlowHandle.getReturnValue().get().getId());



            //SecureHash newId = rpc.uploadAttachment(inputStream);

            System.out.println ("added the new  ____________document"+id);

            System.out.println ("############ END #####################");
         //   a(String docId, String docName, String docType, String docDescription)



            // upload new version of the doc

         // check the party B node .....

            NetworkHostAndPort hostB = new NetworkHostAndPort("localhost",10009) ; //for node A port is 10005
            System.out.println("connecting to the sender node " +hostB);

            CordaRPCClient cordaRPCClientA =  new CordaRPCClient(hostB);
            cordaRPCClientA.start("user1","test") ;
            cordaRPCClientA.use("user1", "test", new Function1<CordaRPCConnection, Object>() {
                @Override
                public Object invoke(CordaRPCConnection cordaRPCConnection) {

                    // secureId Passed is the id that has been uploaded via rpc upload ; which could be replaced here to check
                    Boolean attachmentExist =    cordaRPCConnection.getProxy().attachmentExists(id);

                    System.out.println("is Attachment exist or not "+attachmentExist);

                    return null;
                }
            });

            //queryDocumentState(null,rpc);

        }
    }
}
