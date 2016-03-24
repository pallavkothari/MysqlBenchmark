package com.lendingclub.MysqlBenchmark;

import java.util.List;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

public class LcOptimizedGuids {

	private static final TimeBasedGenerator gen = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
	private static Splitter splitter = Splitter.on('-');
    private static Joiner joiner = Joiner.on("");

	public static String asHexString() {
		String uuid = gen.generate().toString();
        List<String> parts = splitter.splitToList(uuid);
        String reordered = joiner.join(parts.get(2) , parts.get(1) , parts.get(0) , parts.get(3) , parts.get(4));
        return reordered;
	}

}
