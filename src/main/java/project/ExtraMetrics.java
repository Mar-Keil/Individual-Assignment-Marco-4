package project;

import org.openjdk.jmh.annotations.*;

@AuxCounters(AuxCounters.Type.EVENTS)
@State(Scope.Thread)
public class ExtraMetrics {
    public double CPU = 0;
    public long RAM = 0;
}
