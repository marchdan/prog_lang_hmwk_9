package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;


//A cached account class keeps track of the current and starting values
//of a given account in a single thread.
class CachedAccount {
    
    Account account;
    String name;
    int initial_value;
    int current_value;
    public boolean writing_account;
    public boolean reading_account;
    
    //Constructor function, takes in the actual account and the account's name.
    public CachedAccount(Account acc, String word){
        account = acc;
        initial_value = account.peek();
        current_value = initial_value;
        name = word;
        writing_account = false;
        reading_account = false;
    }
    
    //Access function for the actual account object. Unused.
    public Account GetCachedAccount(){
        return account;
    }
    
    //Make sure the account is set to be opened for reading.
    public void SetReading(){
        reading_account = true;
    }
    
    //Make sure the account is set to be opened for writing.
    public void SetWriting(){
        writing_account = true;
    }
    
    //Set the cache's current value to the new value.
    public void SetValue(int value){
        current_value = value;
    }
    
    //Accessor for the name of the cached account.
    public String GetName() {
        return name;
    }
    
    //Function that will open the cached account for reading and/or writing.
    public void open() throws TransactionAbortException{
        boolean opened = false;
        try{
            if (reading_account){
            	//System.out.println("OPENING AN ACCOUNT FOR WRITING");
                account.open(false);
            }
            opened = true;
            if (writing_account){
                account.open(true);
            }
        }
        catch(TransactionAbortException exception){
            if (opened){
                close();
            }
            throw exception;
        }
        catch (TransactionUsageError exception){
        	if (opened){
        		close();
        	}
        	return;
        }
    }
    
    //Function that will close the cached account for both reading and writing.
    public void close(){
        account.close();
    }
    
    //Accessor for the cached account's current value.
    public int peek(){
        return current_value;
    }
    
    //Updates the actual account's value with the cached account's current value.
    public void update() {
        try {
            account.update(current_value);
        }
        catch(TransactionUsageError exc){
        	//System.out.println("GETTING AN ERROR IN UPDATE");
            throw exc;
        }
    }
    
    public void verify() throws TransactionAbortException{
        try{
            account.verify(initial_value);
        }
        catch(TransactionAbortException exc){
            throw exc;
        }
        catch (TransactionUsageError exception){
        	return;
        }
    }
}

//A runnable task that is created by the executor.
class Task implements Runnable {
    
    private static final int A = constants.A;
    private static final int Z = constants.Z;
    private static final int numLetters = constants.numLetters;

    private Account[] accounts;
    private String transaction;
    private List<CachedAccount> cached_accounts = new ArrayList<CachedAccount>();
    

    // TO DO: The sequential version of Task peeks at accounts
    // whenever it needs to get a value, and opens, updates, and closes
    // an account whenever it needs to set a value.  This won't work in
    // the parallel version.  Instead, you'll need to cache values
    // you've read and written, and then, after figuring out everything
    // you want to do, (1) open all accounts you need, for reading,
    // writing, or both, (2) verify all previously peeked-at values,
    // (3) perform all updates, and (4) close all opened accounts.

    public Task(Account[] allAccounts, String trans) {
        accounts = allAccounts;
        transaction = trans;
    }
    
    // TO DO: parseAccount currently returns a reference to an account.
    // You probably want to change it to return a reference to an
    // account *cache* instead.
    //
    
    //A function that takes in an account name and whether it's being opened for
    //reading or writing. It returns the cached account if it has already been
    //stored or returns a new cached account and stores it.
    private CachedAccount parseAccount(String name, boolean forWriting) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            int searcher = AlreadyCached((char)(accountNum + 'A'));
            if (searcher == -1){
                CachedAccount ca = new CachedAccount(a, (char) (accountNum + 'A') + "");
                ca.SetReading();
                cached_accounts.add(ca);
                accountNum = (accounts[accountNum].peek() % numLetters);
                a = accounts[accountNum];
            }
            else{
                accountNum = cached_accounts.get(searcher).peek();
                a = accounts[accountNum];
            }
        }
        int searcher = AlreadyCached((char)(accountNum + 'A'));
        CachedAccount ca;
        if (searcher == -1){
            ca = new CachedAccount(a, (char) (accountNum + 'A') + "");
        }
        else{
            ca = cached_accounts.get(searcher);
        }
        if (forWriting){
            ca.SetWriting();
        }
        else{
            ca.SetReading();
        }
        if (searcher == -1){
            cached_accounts.add(ca);
        }
        return ca;
    }

    //Function that takes in a number or account name and returns the appropriate value.
    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name, false).peek();
        }
        return rtn;
    }
    
    //Function to sort all the cached accounts by their names.
    public void SortAccounts(){
        
        for (int x = 1; x < cached_accounts.size(); x++){
            int y = x - 1;
            int sort_result = cached_accounts.get(y).GetName().compareTo(cached_accounts.get(x).GetName());
            
            //previous letter comes after next letter
            if (sort_result > 0) {
                
                //swap the account
                CachedAccount temp_account = cached_accounts.get(y);
                cached_accounts.set(y, cached_accounts.get(x));
                cached_accounts.set(x, temp_account);
                x = 0;
                
            }
        }
    }
    
    //Function to check whether an account has already been stored in the cache.
    public int AlreadyCached(char name) {
        for (int i = 0; i < cached_accounts.size(); ++i) {
            if (cached_accounts.get(i).GetName() == name + "") {
                return i;
            }
        }
        
        return -1;
    }

    //Function to parse the commands, make the calculations, lock the accounts, make
    //the updates, and then re-open the accounts for further changes.
    public void run() {
        // tokenize transaction
        
        String[] commands = transaction.split(";");
        
        //Keep going until the transactions have successfully been submitted.
        while(true){
        	//System.out.println(commands[0]);
            //Calculates the transactions using the current values.
            for (int i = 0; i < commands.length; i++) {
                String[] words = commands[i].trim().split("\\s");
                if (words.length < 3)
                    throw new InvalidTransactionError();
                    
                CachedAccount lhs = parseAccount(words[0], true);
                
                if (!words[1].equals("="))
                    throw new InvalidTransactionError();
                    
                int rhs = parseAccountOrNum(words[2]);

                
                for (int j = 3; j < words.length; j+=2) {
                    if (words[j].equals("+")){
                        int rhs2 = parseAccountOrNum(words[j+1]);
                        rhs += rhs2;
                    }
                    else if (words[j].equals("-")){
                        int rhs2 = parseAccountOrNum(words[j+1]);
                        rhs -= rhs2;
                    }
                    else
                        throw new InvalidTransactionError();
                }
                lhs.SetValue(rhs);
            }
            
            
            int opened_accounts = 0;
            //Try block to attempt to open all accounts that need to be read from or written to.
            try{
                SortAccounts();
                for (int x = 0; x < cached_accounts.size(); x++){
                    cached_accounts.get(x).open();
                    opened_accounts += 1;
                }
            }
            //Closes all accounts that were successfully opened prior to the error.
            catch (TransactionAbortException e) {
                    // won't happen in sequential version
                    while (opened_accounts > 0){
                        cached_accounts.get(opened_accounts - 1).close();
                        opened_accounts -= 1;
                    }
                    try {
                        Thread.sleep(100);  // ms
                    } catch(InterruptedException e1) {
                    continue;
                    }
                }

           
            
            //Try block to attempt to verify that the values of the cached accounts haven't changed since the calculations.
            try{
                for (int x = 0; x < cached_accounts.size(); x++){
                    cached_accounts.get(x).verify();
                }
            }
            //Closes all cached accounts if there has been a change.
            catch(TransactionAbortException e){
                for (int x = 0; x < cached_accounts.size(); x++){
                    cached_accounts.get(x).close();   
                }
                try {
                    Thread.sleep(100);  // ms
                } catch(InterruptedException e1) {}
                continue;
            }
            
            //Try block to update all accounts that have been modified by the transactions.
            try{
                for (int x = 0; x < cached_accounts.size(); x++){
                    if (cached_accounts.get(x).writing_account){
                        cached_accounts.get(x).update();
                    }
                }
            }
            catch(TransactionUsageError e){
                break;
            }
            
            //Try block to close all accounts that have been opened.
            try{
                for (int x = 0; x < cached_accounts.size(); x++){
                    cached_accounts.get(x).close();
                }
            }
            catch(TransactionUsageError e){
                break;
            }
            
            System.out.println("commit: " + transaction);
            break;
        }
    }
}

public class MultithreadedServer {

	// requires: accounts != null && accounts[i] != null (i.e., accounts are properly initialized)
	// modifies: accounts
	// effects: accounts change according to transactions in inputFile
    public static void runServer(String inputFile, Account accounts[])
        throws IOException {

        // read transactions from input file
        String line;
        BufferedReader input =
            new BufferedReader(new FileReader(inputFile));

        // TO DO: you will need to create an Executor and then modify the
        // following loop to feed tasks to the executor instead of running them
        // directly.  DONE?
        
        ExecutorService e = Executors.newCachedThreadPool();

        while ((line = input.readLine()) != null) {
            e.execute(new Task(accounts, line));
            
            //Task t = new Task(accounts, line);
            //t.run();
        }
        e.shutdown();
        try{
	        if (!e.awaitTermination(60, TimeUnit.SECONDS)){
	            e.shutdownNow();
	            if (!e.awaitTermination(60, TimeUnit.SECONDS)){
	                System.err.println("Executor did not terminate");
	            }
	        }
        }
        catch(InterruptedException exception){
        	e.shutdownNow();
        }
        
        input.close();
    }
}
