package com.livv.greenmail;

import com.icegreen.greenmail.Managers;
import com.icegreen.greenmail.store.FolderException;
import com.icegreen.greenmail.store.MailFolder;
import com.icegreen.greenmail.user.*;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;


public class App
{
    public static void main( String[] args )
    {
        GreenMail greenMail = new GreenMail(new ServerSetup(8080, null, "smtp"));
        greenMail.start();

        //This throws concurrent modification exception
        ConcurrencyTest concurrencyTest = new ConcurrencyTest(greenMail, true);

        //This throws null pointer exception
        //ConcurrencyTest concurrencyTest = new ConcurrencyTest(greenMail, false);
        concurrencyTest.performTest();

        greenMail.stop();

        System.exit(0);
    }
}

class ConcurrencyTest {

    private static final int NO_THREADS = 2;

    private static final int NO_EMAILS_PER_THREAD  = 20;

    private GreenMail greenMail;

    private boolean creationSynchronized;

    public ConcurrencyTest (GreenMail greenMail, boolean creationSynchronized) {
        this.greenMail = greenMail;
        this.creationSynchronized = creationSynchronized;
    }

    private void createMailbox (String email) throws UserException {
        Managers managers = this.greenMail.getManagers();
        UserManager userManager = managers.getUserManager();
        userManager.createUser(email, email, email);
    }

    private void deleteMailbox(String email) throws FolderException {

        GreenMailUser user = this.greenMail.getManagers().getUserManager().getUserByEmail(email);

        MailFolder inbox = this.greenMail.getManagers().getImapHostManager().getInbox(user);
        inbox.deleteAllMessages();
        this.greenMail.getManagers().getUserManager().deleteUser(user);
    }

    public void performTest() {

        Runnable task = () -> {
            for (int counter = 0; counter < NO_EMAILS_PER_THREAD; counter++) {
                String email = "email_" + Thread.currentThread().getName() + "_" + counter;
                try {
                    if (creationSynchronized) {
                        synchronized (ConcurrencyTest.class) {
                            createMailbox(email);
                        }
                    } else {
                        createMailbox(email);
                    }
                    deleteMailbox(email);
                } catch (FolderException | UserException e) {
                    e.printStackTrace();
                }
            }
        };

        Thread [] threads = new Thread[4];
        for (int  i = 0; i < NO_THREADS; i++) {
            threads[i] = new Thread(task);
            threads[i].start();
        }

        for (int i = 0; i < NO_THREADS; i++) {
            try {
                threads[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
