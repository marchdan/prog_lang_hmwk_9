package hw09;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;

//https://docs.oracle.com/javase/7/docs/api/java/util/concurrent/ThreadPoolExecutor.html


class CachedAccount {
    
    Account account;
    String name;
    int initial_value;
    int current_value;
    public boolean writing_account;
    public boolean reading_account;
    
    public CachedAccount(Account acc, String word){
        account = acc;
        initial_value = account.peek();
        current_value = initial_value;
        name = word;
        writing_account = false;
        reading_account = false;
    }
    
    public Account GetCachedAccount(){
        return left_account;
    }
    
    public void SetReading(){
        reading_account = true;
    }
    
    public void SetWriting(){
        writing_account = true;
    }
    
    public void SetValue(int value){
        current_value = value;
    }
    
    public String GetName() {
        return name;
    }
    
    public void open(){
        bool opened = false;
        try{
            if (writing_account){
                account.open(true);
            }
            opened = true;
            if (reading_account){
                account.open(false);
            }
        }
        catch(TransactionAbortException exception){
            if (opened){
                close();
            }
        }
    }
    
    public void close(){
        account.close();
    }
    
    public int peek(){
        return current_value;
    }
    
    public void update(int newValue) {
        try {
            account.update(int newValue);
        }
        catch(TransactionUsageError exc){
            throw exc;
        }
    }
}

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
    private CachedAccount parseAccount(String name, boolean forWriting) {
        int accountNum = (int) (name.charAt(0)) - (int) 'A';
        if (accountNum < A || accountNum > Z)
            throw new InvalidTransactionError();
        Account a = accounts[accountNum];
        for (int i = 1; i < name.length(); i++) {
            if (name.charAt(i) != '*')
                throw new InvalidTransactionError();
            int searcher = AlreadyCached(accountNum + 'A');
            if (searcher == -1){
                CachedAccount ca = new CachedAccount(a, (char) (accountNum + 'A'));
                ca.SetReading();
                cached_accounts.add(ca);
                accountNum = (accounts[accountNum].peek() % numLetters);
                a = accounts[accountNum];
            }
            else{
                accountNum = cached_accounts[searcher].peek();
                a = accounts[accountNum];
            }
        }
        int searcher = AlreadyCached(accountNum + 'A');
        CachedAccount ca;
        if (searcher == -1){
            ca = new CachedAccount(a, (char) (accountNum + 'A'));
        }
        else{
            ca = cached_accounts[searcher];
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

    private int parseAccountOrNum(String name) {
        int rtn;
        if (name.charAt(0) >= '0' && name.charAt(0) <= '9') {
            rtn = new Integer(name).intValue();
        } else {
            rtn = parseAccount(name, false).peek();
        }
        return rtn;
    }
    
    public void SortAccounts(){
        
        for (int x = 1; x < cached_accounts.length; x++){
            int y = x - 1;
            int sort_result = cached_accounts[y].GetName().compareTo(cached_accounts[x].GetName());
            
            //previous letter comes after next letter
            if (sort_result > 0) {
                
                //swap the account
                CachedAccount temp_account = cached_accounts[y];
                cached_accounts[y] = cached_accounts[x];
                cached_accounts[x] = temp_account;
                x = 0;
                
            }
        }
    }
    
    public int AlreadyCached(String name) {
        for (int i = 0; i < cached_accounts.length; ++i) {
            if (cached_accounts[i].GetName() == name) {
                return i;
            }
        }
        
        return -1;
    }

    public void run() {
        // tokenize transaction
        
        String[] commands = transaction.split(";");
        
        while(true){

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
            try{
                SortAccounts();
                for (int x = 0; x < cached_accounts.length; x++){
                    cached_accounts[x].open();
                    opened_accounts += 1;
                }
                catch (TransactionAbortException e) {
                        // won't happen in sequential version
                        while (opened_accounts > 0){
                            cached_accounts[opened_accounts - 1].close();
                            opened_accounts -= 1;
                        }
                        try {
                            Thread.sleep(100);  // ms
                        } catch(InterruptedException e) {}
                        continue;
                    }
                }
            }
            
            try{
                for (int x = 0; x < cached_accounts.length; x++){
                    cached_accounts[x].verify(cached_accounts[x].initial_value);
                }
            }
            catch(TransactionAbortException e){
                for (int x = 0; x < cached_accounts.length; x++){
                    cached_accounts[x].close();   
                }
                try {
                    Thread.sleep(100);  // ms
                } catch(InterruptedException e) {}
                continue;
            }
            
            try{
                for (int x = 0; x < cached_accounts.length; x++){
                    if (cached_accounts[x].writing_account){
                        cached_accounts[x].update(cached_accounts[x].peek());
                    }
                }
            }
            catch(TransactionUsageError e){
                break;
            }
            
            try{
                for (int x = 0; x < cached_accounts.length; x++){
                    cached_accounts.close();
                }
            }
            catch(TransactionUsageError e){
                break;
            }
            
            System.out.println("commit: " + transaction);
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
        
        private static Executor e = Executors.newCachedThreadPool();

        while ((line = input.readLine()) != null) {
            e.execute(new Task(accounts, line));
            
            //Task t = new Task(accounts, line);
            //t.run();
        }
        
        if (!e.awaitTermination(60, TimeUnit.SECONDS)){
            e.shutdownNow();
            if (!e.awaitTermination(60, TimeUnit.SECONDS)){
                System.err.println("Executor did not terminate");
            }
        } catch (InterruptedException ie){
            e.shutdownNow();
        }
        
        input.close();
    }
}
