package com.bagri.client.hazelcast.impl;

import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.cli_QueuedCursor;
import static com.bagri.client.hazelcast.serialize.DataSerializationFactoryImpl.factoryId;
import static com.bagri.core.server.api.CacheConstants.PN_XDM_SCHEMA_POOL;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bagri.client.hazelcast.task.query.ResultFetcher;
import com.bagri.core.api.BagriException;
import com.bagri.core.api.impl.ResultCursorBase;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IExecutorService;
import com.hazelcast.core.IQueue;
import com.hazelcast.core.Member;
import com.hazelcast.core.MemberSelector;
import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.IdentifiedDataSerializable;

public class QueuedCursorImpl extends ResultCursorBase implements IdentifiedDataSerializable { 
	
	private int batchSize;
	private int queueSize;
	private String clientId;
	private String memberId;
	private String queueName;
	private Object current;

	// server side
	private List<Object> results;
	private Iterator<Object> iter;
	
	private IQueue<Object> queue;
	private HazelcastInstance hzi;

	private MemberSelector selector = new ResultMemberSelector();
	
	public QueuedCursorImpl() {
		// for de-serializer
	}
	
	public QueuedCursorImpl(List<Object> results, String clientId, int batchSize) {
		this.results = results;
		this.clientId = clientId;
		this.batchSize = batchSize;
		this.queueSize = UNKNOWN;
	}

	public QueuedCursorImpl(List<Object> results, String clientId, int batchSize, int queueSize, Iterator<Object> iter) {
		this(results, clientId, batchSize);
		this.queueSize = queueSize;
		this.iter = iter;
	}
	
	@Override
	public void close() {
		logger.trace("close.enter; queue remaining size: {}", queue.size());
		queue.clear();
		queue.destroy();
		iter = null; // on the server side
		current = null;
	}

	protected Object getCurrent() {
		return current;
	}

	private IQueue<Object> getQueue() {
		if (queue == null) {
			queue = hzi.getQueue(queueName); 
		}
		return queue;
	}

	// client side
	public void deserialize(HazelcastInstance hzi) {
		this.hzi = hzi;
		queue = getQueue();
		current = null; 
		position = 0; //-1;
	}
	
	private void initQueue(HazelcastInstance hzi) {
		this.hzi =  hzi;
		memberId = hzi.getCluster().getLocalMember().getUuid();
		queueName = "client:" + clientId + ":" + UUID.randomUUID().toString();
		queue = getQueue();
	}
	
	// server side
	public int serialize(HazelcastInstance hzi) {
		initQueue(hzi);
		int size = 0;
		if (iter == null) {
			iter = results.iterator();
		}  
		if (batchSize > 0) {
			for (int i = 0; i < batchSize && addNext(); i++) {
				size++;
			}
			if (queueSize < EMPTY) {
				if (size > 0) {
					if (iter.hasNext()) {
						queueSize = ONE_OR_MORE;
					} else {
						queueSize = ONE; 
					}
				} else {
					queueSize = EMPTY;
				}
			} else if (size > queueSize) {
				logger.info("serialize; declared and current batch queue sizes do not match: {}/{}", queueSize, size);
				//queueSize = size; ?? 
			}
		} else {
			while (iter.hasNext()) { 
				addNext();
				size++;
			}
			if (queueSize < EMPTY) {
				queueSize = size;
			} else if (size != queueSize) {
				logger.info("serialize; declared and current queue sizes do not match: {}/{}", queueSize, size);
			}
		}
		return size;
	}
	
	private boolean addNext() {
		if (iter.hasNext()) {
			Object o = iter.next();
			logger.trace("addNext; next: {}", o);
			if (o != null) {
				if (queue.offer(o)) {
					position++;
					return true;
				} else {
					logger.warn("addNext; queue is full!");
				}
			}
		}
		return false;
	}
	
	@Override
	public List<Object> getList() throws BagriException {
		return results;
		//throw new XDMException("Not implemented in queued cursor", XDMException.ecQuery);
	}
	
	@Override
	public boolean isFixed() {
		return false;
	}
	
	@Override
	public boolean next() {
		current = queue.poll();
		boolean result = current != null;
		if (!result) {
			if (position < queueSize || ((queueSize < EMPTY) && (position % batchSize) == 0)) {
				logger.debug("hasNext; got end of the queue; position: {}; queueSize: {}", position, queueSize);
				// request next batch from server side..
				IExecutorService exec = hzi.getExecutorService(PN_XDM_SCHEMA_POOL);
				Future<Boolean> fetcher = exec.submit(new ResultFetcher(clientId), selector);
				try {
					if (fetcher.get()) {
						current = queue.poll();
						result = current != null;
						if (!result && position < queueSize) {
							logger.warn("next; declared and fetched queue sizes do not match: {}/{}", queueSize, position);
						}
					}
				} catch (InterruptedException | ExecutionException ex) {
					logger.error("next.error", ex); 
				}
			}
		} else {
			position++;
		}
		logger.trace("next; returning: {}", result); 
		return result;
	}

	@Override
	public int getFactoryId() {
		return factoryId;
	}

	@Override
	public int getId() {
		return cli_QueuedCursor;
	}

	@Override
	public void readData(ObjectDataInput in) throws IOException {
		clientId = in.readUTF();
		queueSize = in.readInt();
		memberId = in.readUTF();
		batchSize = in.readInt();
		queueName = in.readUTF();
	}

	@Override
	public void writeData(ObjectDataOutput out) throws IOException {
		out.writeUTF(clientId);
		out.writeInt(queueSize);
		out.writeUTF(memberId);
		out.writeInt(batchSize);
		out.writeUTF(queueName);
	}
	
	@Override
	public String toString() {
		return "ResultCursor [clientId=" + clientId + ", memberId=" + memberId + 
			", queueSize=" + queueSize + ", position=" + position + ", batchSize=" + batchSize + "]";
	}

	
	private class ResultMemberSelector implements MemberSelector {

		@Override
		public boolean select(Member member) {
			return memberId.equals(member.getUuid());
		}
		
	}

}
