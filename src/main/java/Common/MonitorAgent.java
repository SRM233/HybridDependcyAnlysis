package Common;

import java.lang.instrument.Instrumentation;

public class MonitorAgent {
    public static void premain(String agentArgs, Instrumentation inst) {
        System.out.println("Agent started with args: " + agentArgs);
        // Add transformers or ByteBuddy hooks here
    }
}
