package com.lendingclub.MysqlBenchmark;

import com.fasterxml.uuid.EthernetAddress;
import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.TimeBasedGenerator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;

import java.util.List;

public class LcOptimizedGuids {

	private static final TimeBasedGenerator gen = Generators.timeBasedGenerator(EthernetAddress.fromInterface());
	private static Splitter splitter = Splitter.on('-');
    private static Joiner joiner = Joiner.on("");
	private static Joiner dashJoiner = Joiner.on("-");

	static String asHexString() {
		return asHex(false);
	}

	static String asHyphenatedHexString() {
		return asHex(true);
	}

	private static String asHex(boolean useDashJoiner) {
		Joiner joiner = useDashJoiner ? dashJoiner : LcOptimizedGuids.joiner;
		String uuid = gen.generate().toString();
		List<String> parts = splitter.splitToList(uuid);
		return joiner.join(parts.get(2) , parts.get(1) , parts.get(0) , parts.get(3) , parts.get(4));
	}

}
