/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.atomix.catalogue.client;

import io.atomix.catalyst.util.BuilderPool;

/**
 * Raft state queries read system state.
 * <p>
 * Queries are submitted by clients to a Raft server to read Raft cluster-wide state. In contrast to
 * {@link Command commands}, queries allow for more flexible
 * {@link ConsistencyLevel consistency levels} that trade consistency for performance.
 * <p>
 * All queries must specify a {@link #consistency()} with which to execute the query. The provided consistency level
 * dictates how queries are submitted to the Raft cluster. Higher consistency levels like
 * {@link ConsistencyLevel#LINEARIZABLE} and {@link ConsistencyLevel#BOUNDED_LINEARIZABLE}
 * are forwarded to the cluster leader, while lower levels are allowed to read from followers for higher throughput.
 * <p>
 * By default, all queries should use the strongest consistency level, {@link ConsistencyLevel#LINEARIZABLE}.
 * It is essential that users understand the trade-offs in the various consistency levels before using them.
 *
 * @see ConsistencyLevel
 *
 * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
 */
public interface Query<T> extends Operation<T> {

  /**
   * Constants for specifying Raft {@link Query} consistency levels.
   * <p>
   * This enum provides identifiers for configuring consistency levels for {@link Query queries}
   * submitted to a Raft cluster.
   * <p>
   * Consistency levels are used to dictate how queries are routed through the Raft cluster and the requirements for
   * completing read operations based on submitted queries. For expectations of specific consistency levels, see below.
   *
   * @author <a href="http://github.com/kuujo">Jordan Halterman</a>
   */
  enum ConsistencyLevel {

    /**
     * Enforces causal query consistency.
     * <p>
     * Causal consistency requires that clients always see non-overlapping state progress monotonically. This constraint allows
     * reads from followers. When a causally consistent {@link Query} is submitted to the cluster, the first server that
     * receives the query will attempt to handle it. If the server that receives the query is more than a heartbeat behind the
     * leader, the query will be forwarded to the leader. If the server that receives the query has not advanced past the
     * client's last write, the read will be queued until it can be satisfied.
     */
    CAUSAL,

    /**
     * Enforces sequential query consistency.
     * <p>
     * Sequential read consistency requires that clients always see state progress in monotonically increasing order. Note that
     * this constraint allows reads from followers. When a sequential {@link Query} is submitted to the cluster, the first
     * server that receives the query will handle it. However, in order to ensure that state does not go back in time, the
     * client must submit its last known index with the query as well. If the server that receives the query has not advanced
     * past the provided client index, it will queue the query and await more entries from the leader.
     */
    SEQUENTIAL,

    /**
     * Enforces bounded linearizable query consistency based on leader lease.
     * <p>
     * Bounded linearizability is a special implementation of linearizable reads that relies on the semantics of Raft's
     * election timers to determine whether it is safe to immediately apply a query to the Raft state machine. When a
     * linearizable {@link Query} is submitted to the Raft cluster with linearizable consistency,
     * it must be forwarded to the current cluster leader. For lease-based linearizability, the leader will determine whether
     * it's safe to apply the query to its state machine based on the last time it successfully contacted a majority of the
     * cluster. If the leader contacted a majority of the cluster within the last election timeout, it assumes that no other
     * member could have since become the leader and immediately applies the query to its state machine. Alternatively, if it
     * hasn't contacted a majority of the cluster within an election timeout, the leader will handle the query as if it were
     * submitted with {@link #LINEARIZABLE} consistency.
     */
    BOUNDED_LINEARIZABLE,

    /**
     * Enforces linearizable query consistency.
     * <p>
     * The linearizable consistency level guarantees consistency by contacting a majority of the cluster on every read.
     * When a {@link Query} is submitted to the cluster with linearizable consistency, it must be
     * forwarded to the current cluster leader. Once received by the leader, the leader will contact a majority of the
     * cluster before applying the query to its state machine and returning the result. Note that if the leader is already
     * in the process of contacting a majority of the cluster, it will queue the {@link Query} to
     * be processed on the next round trip. This allows the leader to batch expensive quorum based reads for efficiency.
     */
    LINEARIZABLE

  }

  /**
   * Returns the query consistency level.
   * <p>
   * The consistency will dictate how the query is executed on the server state. Stronger consistency levels can guarantee
   * linearizability in all or most cases, while weaker consistency levels trade linearizability for more performant
   * reads from followers. Consult the {@link ConsistencyLevel} documentation for more information
   * on the different consistency levels.
   * <p>
   * By default, this method enforces strong consistency with the {@link ConsistencyLevel#LINEARIZABLE} consistency level.
   *
   * @return The query consistency level.
   */
  default ConsistencyLevel consistency() {
    return ConsistencyLevel.LINEARIZABLE;
  }

  /**
   * Base builder for queries.
   */
  abstract class Builder<T extends Builder<T, U, V>, U extends Query<V>, V> extends Operation.Builder<T, U, V> {
    protected U query;

    protected Builder(BuilderPool<T, U> pool) {
      super(pool);
    }

    @Override
    protected void reset(U query) {
      super.reset(query);
      this.query = query;
    }

    @Override
    public U build() {
      close();
      return query;
    }
  }

}