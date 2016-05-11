package hw09.test;

import hw09.*;

import java.io.*;
import java.lang.Thread.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.*;
import java.util.Random;

import junit.framework.TestCase;

import org.junit.Test;

public class MultithreadedServerTests extends TestCase {
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;
    private static Account[] accounts;
            
    protected static void dumpAccounts() {
	    // output values:
	    for (int i = A; i <= Z; i++) {
	       System.out.print("    ");
	       if (i < 10) System.out.print("0");
	       System.out.print(i + " ");
	       System.out.print(new Character((char) (i + 'A')) + ": ");
	       accounts[i].print();
	       System.out.print(" (");
	       accounts[i].printMod();
	       System.out.print(")\n");
	    }
	 }    
     
        
     @Test
	 public void testIncrement() throws IOException {
	
		// initialize accounts 
		accounts = new Account[numLetters];
		for (int i = A; i <= Z; i++) {
			accounts[i] = new Account(Z-i);
		}			 
		
		
		MultithreadedServer.runServer("hw09/data/increment", accounts);
		// assert correct account values
		for (int i = A; i <= Z; i++) {
			Character c = new Character((char) (i+'A'));
			assertEquals("Account "+c+" differs",Z-i+1,accounts[i].getValue());
		}		

	 }
	 
	 @Test
	 public void testRotate() throws IOException{
	 	
	 	//initialize accounts
	 	accounts = new Account[numLetters];
	 	for (int i = A; i <= Z; i++){
	 		accounts[i] = new Account(Z-i);
	 	}
	 	
	 	//System.out.println(accounts[0].getValue());
	 	MultithreadedServer.runServer("hw09/data/rotate", accounts);
	 	//System.out.println(accounts[0].getValue());
	 
	 }
	 
	 @Test 
	 public void testDecrement() throws IOException{
	 	
	 	//initialize accounts
	 	accounts = new Account[numLetters];
	 	for (int i = A; i <= Z; i++){
	 		accounts[i] = new Account(Z-i);
	 	}
	 	
	 	MultithreadedServer.runServer("hw09/data/decrement", accounts);
	 	
	 	//assert correct account values
	 	for (int i = A; i <= Z; ++i){
	 		Character c = new Character((char) (i+'A'));
	 		assertEquals("Account " + c + " differs", Z - i - 1, accounts[i].getValue());
	 	}
	 }
	 
	 @Test
	 public void testSwap() throws IOException{
	 	
	 	//initialize accounts
	 	accounts = new Account[numLetters];
	 	for (int i = A; i <= Z; i++){
	 		accounts[i] = new Account(Z-i);
	 	}
	 	
	 	MultithreadedServer.runServer("hw09/data/swap", accounts);
	 	
	 	//assert correct account values
	 	for (int i = A; i <= Z; ++i){
	 		Character c = new Character((char) (i+'A'));
	 	}
	 }
	 
	 @Test
	 public void testModulo() throws IOException{
	 	
	 	//initialize accounts
	 	accounts = new Account[numLetters];
	 	for (int i = A; i <= Z; i++){
	 		accounts[i] = new Account(Z-i);
	 	}
	 	
	 	MultithreadedServer.runServer("hw09/data/modulo", accounts);
	 	
	 	//assert correct account values
	 	for (int i = A; i <= Z; ++i){
	 		Character c = new Character((char) (i+'A'));
	 		if ( ((i + 'A') <= 13 + 'A') && ((i + 'A') != (4 + 'A'))) {
	 			assertEquals("Account " + c + " differs", 4 + Z - i, accounts[i].getValue());
	 		}
	 		
	 	}
	 }
	 
	 @Test
	 public void testIndirect2() throws IOException{
		 
		//initialize accounts
		 	accounts = new Account[numLetters];
		 	for (int i = A; i <= Z; i++){
		 		accounts[i] = new Account(Z-i);
		 	}
		 	
		 	MultithreadedServer.runServer("hw09/data/indirect2", accounts);
		 
	 }
	
}