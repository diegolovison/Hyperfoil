package com.example;

import io.hyperfoil.cli.commands.Wrk2;

// run with -Xms16g -Xmx16g
// look at /tmp/hyperfoil/hyperfoil.local.log
public class Wrk2Client {

   public static void main(String[] args) {
      Wrk2 cmd = new Wrk2();
      int result = cmd.exec(
            new String[] { "-t", "2", "-c", "6", "-d", "60s", "-R", "500000", "--timeout", "2s", "localhost:8080/hello2" });
      System.out.println(result);
   }
}
