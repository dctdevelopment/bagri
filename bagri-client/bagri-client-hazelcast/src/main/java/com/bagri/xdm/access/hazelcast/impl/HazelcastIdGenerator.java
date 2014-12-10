package com.bagri.xdm.access.hazelcast.impl;

import com.bagri.common.idgen.IdGenerator;
import com.hazelcast.core.IAtomicLong;

public class HazelcastIdGenerator implements IdGenerator<Long> {
	
	private IAtomicLong idGen; 

	public HazelcastIdGenerator(IAtomicLong idGen) {
		this.idGen = idGen;
	}

	@Override
	public Long next() {
		//
		return idGen.incrementAndGet();
	}

	@Override
	public Long[] nextRange(int size) {
		Long[] result = new Long[size];
		long current = idGen.getAndAdd(size);
		for (int i=1; i <= size; i++) {
			result[i] = current + 1;
		}
		return result; 
	}

}