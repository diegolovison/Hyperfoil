package com.example;

import io.hyperfoil.cli.commands.Wrk2;

// use wrk to get the rate
// run with -Xms16g -Xmx16g
// look at /tmp/hyperfoil/hyperfoil.local.log
public class Wrk2ClientBigTimeout {

   public static void main(String[] args) {
      Wrk2 cmd = new Wrk2();
      int result = cmd.exec(
            new String[] { "-t", "2", "-c", "6", "-d", "10s", "-R", "50000", "--timeout", "100s", "localhost:8080/hello" });
      System.out.println(result);
   }
}
